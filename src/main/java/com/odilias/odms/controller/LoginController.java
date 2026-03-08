package com.odilias.odms.controller;

import com.odilias.odms.dao.UserDAO;
import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Label networkRibbon;

    private UserDAO userDAO = new UserDAO();
    private ScheduledExecutorService networkCheckExecutor;
    private boolean isAuthenticating = false;

    @FXML
    public void initialize() {
        startNetworkChecker();
    }

    private void startNetworkChecker() {
        networkCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        networkCheckExecutor.scheduleAtFixedRate(() -> {
            boolean isOnline = checkConnection();
            Platform.runLater(() -> {
                networkRibbon.setVisible(!isOnline);
                networkRibbon.setManaged(!isOnline);
                if (!isAuthenticating) {
                    loginButton.setDisable(!isOnline);
                }
            });
        }, 0, 15, TimeUnit.SECONDS);
    }

    private boolean checkConnection() {
        try (Socket socket = new Socket()) {
            // Check general internet connectivity instead of DB server directly
            // to avoid MySQL "Host Blocked" errors from interrupted pings.
            socket.connect(new InetSocketAddress("1.1.1.1", 53), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        // Show loading state
        isAuthenticating = true;
        loginButton.setDisable(true);
        loginButton.setText("AUTHENTICATING...");
        errorLabel.setText("");

        new Thread(() -> {
            try {
                User user = userDAO.authenticate(username, password);
                Platform.runLater(() -> {
                    if (user != null) {
                        try {
                            SessionManager.setCurrentUser(user);
                            FXMLLoader loader = new FXMLLoader(
                                    com.odilias.odms.MainApp.class.getResource("view/dashboard.fxml"));
                            Parent root = loader.load();

                            DashboardController dashboardController = loader.getController();
                            dashboardController.initData(user);

                            Stage stage = (Stage) loginButton.getScene().getWindow();
                            stage.setScene(new Scene(root, 1024, 768));
                            stage.setResizable(true);
                            stage.centerOnScreen();
                            stage.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorLabel.setText("Error loading dashboard.");
                            resetLoginButton();
                        }
                    } else {
                        errorLabel.setText("Invalid username or password.");
                        resetLoginButton();
                    }
                });
            } catch (Throwable t) {
                t.printStackTrace();
                Platform.runLater(() -> {
                    errorLabel.setText("Runtime Error: " + t.getClass().getSimpleName() + " - " + t.getMessage());
                    resetLoginButton();
                });
            }
        }).start();
    }

    private void resetLoginButton() {
        isAuthenticating = false;
        loginButton.setDisable(false);
        loginButton.setText("INITIALIZE SECURE LOGIN");
    }
}

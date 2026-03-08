package com.odilias.odms.controller;

import com.odilias.odms.dao.UserDAO;
import com.odilias.odms.dao.AuditDAO;
import com.odilias.odms.model.User;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AddStaffController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField fullNameField;

    @FXML
    private ComboBox<User.Role> roleComboBox;

    @FXML
    private ComboBox<User.Status> statusComboBox;

    @FXML
    private TextField emailField;

    @FXML
    private TextField contactField;

    @FXML
    private Label infoLabel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
        statusComboBox.setItems(FXCollections.observableArrayList(User.Status.values()));
        statusComboBox.setValue(User.Status.ACTIVE);
    }

    @FXML
    void handleSave(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String fullName = fullNameField.getText();
        User.Role role = roleComboBox.getValue();
        User.Status status = statusComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || role == null) {
            infoLabel.setText("Please fill in all mandatory fields (*)");
            return;
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setFullName(fullName);
            user.setRole(role);
            user.setStatus(status);
            user.setEmail(emailField.getText());
            user.setContactNumber(contactField.getText());

            userDAO.createUser(user, password);
            AuditDAO.log(null, "STAFF", "Registered new staff: " + username + " with role " + role);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.close();
        } catch (SQLException e) {
            infoLabel.setText("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}

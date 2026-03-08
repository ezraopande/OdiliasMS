package com.odilias.odms.controller;

import com.odilias.odms.dao.UserDAO;
import com.odilias.odms.dao.AuditDAO;
import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditStaffController {

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
    private User currentUser;

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
        statusComboBox.setItems(FXCollections.observableArrayList(User.Status.values()));
    }

    public void initData(User user) {
        this.currentUser = user;
        usernameField.setText(user.getUsername());
        usernameField.setDisable(true); // Username cannot be changed
        fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
        roleComboBox.setValue(user.getRole());
        statusComboBox.setValue(user.getStatus());
        emailField.setText(user.getEmail() != null ? user.getEmail() : "");
        contactField.setText(user.getContactNumber() != null ? user.getContactNumber() : "");
    }

    @FXML
    void handleSave(ActionEvent event) {
        String fullName = fullNameField.getText();
        User.Role role = roleComboBox.getValue();
        User.Status status = statusComboBox.getValue();
        String password = passwordField.getText();

        if (fullName.isEmpty() || role == null) {
            infoLabel.setText("Please fill in all mandatory fields (*)");
            return;
        }

        try {
            currentUser.setFullName(fullName);
            currentUser.setRole(role);
            currentUser.setStatus(status);
            currentUser.setEmail(emailField.getText());
            currentUser.setContactNumber(contactField.getText());

            userDAO.adminUpdateUser(currentUser, password);
            User currentAdmin = SessionManager.getCurrentUser();
            AuditDAO.log(currentAdmin != null ? currentAdmin.getId() : null, "ADMIN",
                    "Updated staff details for: " + currentUser.getUsername());

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

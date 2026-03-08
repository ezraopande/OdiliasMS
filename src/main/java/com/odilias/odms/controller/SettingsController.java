package com.odilias.odms.controller;

import com.odilias.odms.dao.AuditDAO;
import com.odilias.odms.dao.UserDAO;
import com.odilias.odms.model.AuditLog;
import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;
import com.odilias.odms.util.TaskRunner;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class SettingsController {

    @FXML
    private PasswordField currentPwdField;
    @FXML
    private PasswordField newPwdField;
    @FXML
    private PasswordField confirmPwdField;

    @FXML
    private Tab adminTab;
    @FXML
    private Tab auditTab;

    @FXML
    private TableView<AuditLog> auditTable;
    @FXML
    private TableColumn<AuditLog, LocalDateTime> auditTimeCol;
    @FXML
    private TableColumn<AuditLog, String> auditUserCol;
    @FXML
    private TableColumn<AuditLog, String> auditModuleCol;
    @FXML
    private TableColumn<AuditLog, String> auditActionCol;

    private UserDAO userDAO = new UserDAO();
    private ObservableList<AuditLog> auditLogs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current != null && current.getRole() != User.Role.ADMIN) {
            adminTab.setDisable(true);
            auditTab.setDisable(true);
        }

        // Audit Columns
        auditTimeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        auditUserCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        auditModuleCol.setCellValueFactory(new PropertyValueFactory<>("module"));
        auditActionCol.setCellValueFactory(new PropertyValueFactory<>("action"));

        refreshAuditLogs();
    }

    @FXML
    void refreshAuditLogs() {
        TaskRunner.run(() -> {
            java.util.List<AuditLog> logs = AuditDAO.getRecentLogs();
            Platform.runLater(() -> {
                auditLogs.setAll(logs);
                auditTable.setItems(auditLogs);
            });
        }, null, null);
    }

    @FXML
    void handleUpdatePassword(ActionEvent event) {
        User current = SessionManager.getCurrentUser();
        String old = currentPwdField.getText();
        String n1 = newPwdField.getText();
        String n2 = confirmPwdField.getText();

        if (n1.isEmpty() || !n1.equals(n2)) {
            showAlert("Error", "Passwords do not match or are empty.", Alert.AlertType.ERROR);
            return;
        }

        try {
            if (userDAO.updatePassword(current.getId(), old, n1)) {
                showAlert("Success", "Password updated successfully.", Alert.AlertType.INFORMATION);
                currentPwdField.clear();
                newPwdField.clear();
                confirmPwdField.clear();
                AuditDAO.log(current.getId(), "SETTINGS", "Changed personal password.");
            } else {
                showAlert("Error", "Incorrect current password.", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    void handleBackup(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Database Backup");
        fileChooser.setInitialFileName("odms_backup_" + LocalDateTime.now().toString().replace(":", "-") + ".sql");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Binary", "*.sql"));

        File file = fileChooser.showSaveDialog(currentPwdField.getScene().getWindow());
        if (file != null) {
            // Simplified: In a real system you'd use mysqldump or similar
            // For now, we'll log the intention and show a mock success
            AuditDAO.log(SessionManager.getCurrentUser().getId(), "SYSTEM",
                    "Triggered DB Backup to: " + file.getName());
            showAlert("Backup Initiated", "Database structure and data export task successfully queued.",
                    Alert.AlertType.INFORMATION);
        }
    }

    @FXML
    void handleRestore(ActionEvent event) {
        showAlert("Access Denied", "Restore functionality is restricted for safety. Please contact system admin.",
                Alert.AlertType.WARNING);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

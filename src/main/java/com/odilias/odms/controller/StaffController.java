package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.dao.UserDAO;
import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;
import com.odilias.odms.util.TaskRunner;
import com.odilias.odms.util.ToastUtil;
import com.odilias.odms.util.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class StaffController {

    @FXML
    private TableView<User> staffTable;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> statusColumn;

    @FXML
    private HBox actionBox;

    @FXML
    private TextField searchField;

    @FXML
    private Button editBtn;

    @FXML
    private Button activateBtn;

    @FXML
    private Button unlockBtn;

    @FXML
    private Button deactivateBtn;

    private UserDAO userDAO = new UserDAO();
    private ObservableList<User> staffList = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<User> filteredStaff;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(cellData -> {
            var role = cellData.getValue().getRole();
            return new javafx.beans.property.SimpleStringProperty(role != null ? role.name() : null);
        });
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        statusColumn.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(status != null ? status.name() : null);
        });

        filteredStaff = new javafx.collections.transformation.FilteredList<>(staffList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredStaff.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (user.getFullName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (user.getUsername().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        staffTable.setItems(filteredStaff);

        // Show/hide action box and buttons based on table selection and status
        staffTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            actionBox.setVisible(hasSelection);
            actionBox.setManaged(hasSelection);

            if (hasSelection) {
                User.Status status = newVal.getStatus();

                // Activate: show if INACTIVE or LOCKED
                activateBtn.setVisible(status != User.Status.ACTIVE);
                activateBtn.setManaged(status != User.Status.ACTIVE);

                // Unlock: show if LOCKED
                unlockBtn.setVisible(status == User.Status.LOCKED);
                unlockBtn.setManaged(status == User.Status.LOCKED);

                // Deactivate: show if ACTIVE
                deactivateBtn.setVisible(status == User.Status.ACTIVE);
                deactivateBtn.setManaged(status == User.Status.ACTIVE);
            }
        });

        loadStaff();
    }

    private void loadStaff() {
        TaskRunner.run(() -> {
            try {
                java.util.List<User> users = userDAO.getAllUsers();
                javafx.application.Platform.runLater(() -> {
                    staffList.setAll(users);
                    staffTable.setItems(staffList);
                });
            } catch (SQLException e) {
                Logger.error("Failed to load staff list", e);
            }
        }, null, null);
    }

    @FXML
    void showAddStaffForm(ActionEvent event) {
        if (!isAdmin()) {
            showNoPermission();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/add_staff.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Register New Staff");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadStaff();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleEditStaff(ActionEvent event) {
        if (!isAdmin()) {
            showNoPermission();
            return;
        }
        User selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/edit_staff.fxml"));
            Parent root = loader.load();

            EditStaffController controller = loader.getController();
            controller.initData(selected);

            Stage stage = new Stage();
            stage.setTitle("Edit Staff");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadStaff();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleActivate(ActionEvent event) {
        updateStaffStatus(User.Status.ACTIVE);
    }

    @FXML
    void handleDeactivate(ActionEvent event) {
        updateStaffStatus(User.Status.INACTIVE);
    }

    @FXML
    void handleUnlock(ActionEvent event) {
        if (!isAdmin()) {
            showNoPermission();
            return;
        }
        User selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                userDAO.unlockAccount(selected.getId());
                ToastUtil.show((Stage) staffTable.getScene().getWindow(), "Account Unlocked Successfully!",
                        ToastUtil.ToastType.SUCCESS);
                loadStaff();
            } catch (SQLException e) {
                e.printStackTrace();
                ToastUtil.show((Stage) staffTable.getScene().getWindow(), "Error: " + e.getMessage(),
                        ToastUtil.ToastType.ERROR);
            }
        }
    }

    private void updateStaffStatus(User.Status status) {
        if (!isAdmin()) {
            showNoPermission();
            return;
        }
        User selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                userDAO.updateUserStatus(selected.getId(), status);
                ToastUtil.show((Stage) staffTable.getScene().getWindow(), "Account Status Updated: " + status,
                        ToastUtil.ToastType.SUCCESS);
                loadStaff();
            } catch (SQLException e) {
                e.printStackTrace();
                ToastUtil.show((Stage) staffTable.getScene().getWindow(), "Error: " + e.getMessage(),
                        ToastUtil.ToastType.ERROR);
            }
        }
    }

    private boolean isAdmin() {
        User current = SessionManager.getCurrentUser();
        return current != null && current.getRole() == User.Role.ADMIN;
    }

    private void showNoPermission() {
        ToastUtil.show((Stage) staffTable.getScene().getWindow(), "Only Administrators can manage user accounts.",
                ToastUtil.ToastType.ERROR);
    }
}

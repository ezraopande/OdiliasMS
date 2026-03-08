package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;
import com.odilias.odms.util.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

public class DashboardController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label headerTitle;
    @FXML
    private Label userNameLabel;

    @FXML
    private Button homeBtn;
    @FXML
    private Button patientBtn;
    @FXML
    private Button appointmentBtn;
    @FXML
    private Button recordBtn;
    @FXML
    private Button staffBtn;
    @FXML
    private Button billingBtn;
    @FXML
    private Button reportBtn;
    @FXML
    private Button pharmacyBtn;
    @FXML
    private Button labBtn;

    private User currentUser;

    @FXML
    public void initialize() {
    }

    public void initData(User user) {
        this.currentUser = user;
        userNameLabel.setText(user.getFullName());
        applyRBAC();
        showHome(null);
    }

    private void applyRBAC() {
        User.Role role = currentUser.getRole();

        // Default hidden
        staffBtn.setManaged(false);
        staffBtn.setVisible(false);
        reportBtn.setManaged(false);
        reportBtn.setVisible(false);
        pharmacyBtn.setManaged(false);
        pharmacyBtn.setVisible(false);
        labBtn.setManaged(false);
        labBtn.setVisible(false);

        if (role == User.Role.ADMIN || role == User.Role.MANAGEMENT) {
            staffBtn.setManaged(true);
            staffBtn.setVisible(true);
            reportBtn.setManaged(true);
            reportBtn.setVisible(true);
            pharmacyBtn.setManaged(true);
            pharmacyBtn.setVisible(true);
            labBtn.setManaged(true);
            labBtn.setVisible(true);
        }

        if (role == User.Role.PHARMACIST) {
            billingBtn.setManaged(false);
            billingBtn.setVisible(false);
            appointmentBtn.setManaged(false);
            appointmentBtn.setVisible(false);
            pharmacyBtn.setManaged(true);
            pharmacyBtn.setVisible(true);
        }

        if (role == User.Role.RECEPTIONIST) {
            recordBtn.setManaged(false);
            recordBtn.setVisible(false);
            pharmacyBtn.setManaged(false);
            pharmacyBtn.setVisible(false);
        }

        if (role == User.Role.CLINICAL_OFFICER || role == User.Role.NURSE) {
            pharmacyBtn.setManaged(true);
            pharmacyBtn.setVisible(true);
            labBtn.setManaged(true);
            labBtn.setVisible(true);
        }

        if (role == User.Role.LAB_STAFF) {
            labBtn.setManaged(true);
            labBtn.setVisible(true);
            patientBtn.setManaged(false);
            patientBtn.setVisible(false);
            billingBtn.setManaged(false);
            billingBtn.setVisible(false);
            appointmentBtn.setManaged(false);
            appointmentBtn.setVisible(false);
            recordBtn.setManaged(false);
            recordBtn.setVisible(false);
        }
    }

    @FXML
    void showHome(ActionEvent event) {
        loadModule("dashboard_overview.fxml", "Dashboard");
    }

    @FXML
    void showPatients(ActionEvent event) {
        loadModule("patients.fxml", "Patients");
    }

    @FXML
    void showAppointments(ActionEvent event) {
        loadModule("appointments.fxml", "Appointments");
    }

    @FXML
    void showMedicalRecords(ActionEvent event) {
        loadModule("medical_records.fxml", "Medical Records");
    }

    @FXML
    void showStaff(ActionEvent event) {
        if (currentUser.getRole() == User.Role.ADMIN || currentUser.getRole() == User.Role.MANAGEMENT) {
            loadModule("staff_management.fxml", "Staff Management");
        }
    }

    @FXML
    void showBilling(ActionEvent event) {
        loadModule("billing.fxml", "Billing & Invoices");
    }

    @FXML
    void showReports(ActionEvent event) {
        if (currentUser.getRole() == User.Role.ADMIN || currentUser.getRole() == User.Role.MANAGEMENT) {
            loadModule("reports.fxml", "Reports");
        }
    }

    @FXML
    void showPharmacy(ActionEvent event) {
        loadModule("pharmacy.fxml", "Pharmacy");
    }

    @FXML
    void showLaboratory(ActionEvent event) {
        loadModule("laboratory.fxml", "Laboratory");
    }

    @FXML
    void showSettings(ActionEvent event) {
        loadModule("settings.fxml", "Settings");
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        SessionManager.clearSession();
        Parent loginRoot = FXMLLoader.load(MainApp.class.getResource("view/login.fxml"));
        Stage stage = (Stage) userNameLabel.getScene().getWindow();
        stage.setScene(new Scene(loginRoot, 800, 600));
        stage.centerOnScreen();
        stage.show();
    }

    private void loadModule(String fxmlFile, String title) {
        Platform.runLater(() -> {
            try {
                headerTitle.setText("Initializing " + title + "...");

                String resourcePath = "/com/odilias/odms/view/" + fxmlFile;
                java.net.URL fxmlUrl = getClass().getResource(resourcePath);

                if (fxmlUrl == null) {
                    throw new IOException("FXML resource not found: " + resourcePath);
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Node node = loader.load();

                contentArea.getChildren().setAll(node);
                headerTitle.setText(title);
                updateActiveBtn(fxmlFile);

                if (contentArea.getScene() != null && contentArea.getScene().getWindow() instanceof Stage) {
                    ((Stage) contentArea.getScene().getWindow()).setTitle("Odilias MS - " + title);
                }

                contentArea.requestLayout();

            } catch (Throwable e) {
                Logger.error("CRITICAL NAVIGATION FAILURE: " + title, e);
                headerTitle.setText("Module Load Failed");

                VBox errorDisplay = new VBox(20);
                errorDisplay.setAlignment(Pos.CENTER);
                errorDisplay.setPadding(new Insets(40));
                errorDisplay.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

                Label err1 = new Label("SYSTEM STALL: " + title);
                err1.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 20px;");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                TextArea stackTrace = new TextArea(sw.toString());
                stackTrace.setEditable(false);
                stackTrace.setPrefHeight(200);
                stackTrace.setMaxWidth(600);
                stackTrace.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px;");

                Button retry = new Button("FORCE RELOAD MODULE");
                retry.getStyleClass().add("btn-primary");
                retry.setPrefSize(200, 45);
                retry.setOnAction(ev -> loadModule(fxmlFile, title));

                errorDisplay.getChildren().addAll(err1, new Label("Diagnosis (Please check logs/error.log):"),
                        stackTrace, retry);
                contentArea.getChildren().setAll(errorDisplay);
            }
        });
    }

    private void updateActiveBtn(String fxml) {
        // Clear all active classes first
        if (homeBtn != null)
            homeBtn.getStyleClass().remove("nav-button-active");
        patientBtn.getStyleClass().remove("nav-button-active");
        appointmentBtn.getStyleClass().remove("nav-button-active");
        recordBtn.getStyleClass().remove("nav-button-active");
        staffBtn.getStyleClass().remove("nav-button-active");
        billingBtn.getStyleClass().remove("nav-button-active");
        pharmacyBtn.getStyleClass().remove("nav-button-active");
        labBtn.getStyleClass().remove("nav-button-active");
        reportBtn.getStyleClass().remove("nav-button-active");

        // Simple match based on file name
        if (fxml.contains("overview") || fxml.equals("dashboard.fxml")) {
            if (homeBtn != null)
                homeBtn.getStyleClass().add("nav-button-active");
        } else if (fxml.contains("patient"))
            patientBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("appointment"))
            appointmentBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("medical"))
            recordBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("staff"))
            staffBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("billing") || fxml.contains("invoice"))
            billingBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("pharmacy"))
            pharmacyBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("lab"))
            labBtn.getStyleClass().add("nav-button-active");
        else if (fxml.contains("report"))
            reportBtn.getStyleClass().add("nav-button-active");
    }
}

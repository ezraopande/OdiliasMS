package com.odilias.odms.controller;

import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.Patient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import com.odilias.odms.util.PrintUtil;

import java.sql.SQLException;
import java.util.List;

public class PatientController {

    @FXML
    private TableView<Patient> patientTable;

    @FXML
    private TableColumn<Patient, Integer> idColumn;

    @FXML
    private TableColumn<Patient, String> nameColumn;

    @FXML
    private TableColumn<Patient, Integer> ageColumn;

    @FXML
    private TableColumn<Patient, String> genderColumn;

    @FXML
    private TableColumn<Patient, String> contactColumn;

    @FXML
    private TextField searchField;

    @FXML
    private HBox patientActionBox;

    private PatientDAO patientDAO = new PatientDAO();
    private ObservableList<Patient> patientList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        genderColumn.setCellValueFactory(new PropertyValueFactory<>("gender"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contactNumber"));

        // Show/hide action box based on table selection
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            patientActionBox.setVisible(hasSelection);
            patientActionBox.setManaged(hasSelection);
        });

        loadPatients();
    }

    private void loadPatients() {
        // Load clinical data in a separate thread to keep the interface smooth
        new Thread(() -> {
            try {
                List<Patient> patients = patientDAO.getAllPatients();
                Platform.runLater(() -> {
                    patientList.setAll(patients);
                    patientTable.setItems(patientList);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String query = searchField.getText().toLowerCase();
        if (query.isEmpty()) {
            loadPatients();
        } else {
            ObservableList<Patient> filtered = patientList
                    .filtered(p -> p.getFullName().toLowerCase().contains(query) ||
                            String.valueOf(p.getId()).contains(query));
            patientTable.setItems(filtered);
        }
    }

    @FXML
    void showAddPatientForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(com.odilias.odms.MainApp.class.getResource("view/add_patient.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Register New Patient");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadPatients();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleEditPatient(ActionEvent event) {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        com.odilias.odms.MainApp.class.getResource("view/edit_patient.fxml"));
                javafx.scene.Parent root = loader.load();

                EditPatientController controller = loader.getController();
                controller.setPatient(selected);

                javafx.stage.Stage stage = new javafx.stage.Stage();
                stage.setTitle("Edit Patient - " + selected.getFullName());
                stage.setScene(new javafx.scene.Scene(root));
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.showAndWait();

                loadPatients();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void viewPatientDetails(ActionEvent event) {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(com.odilias.odms.MainApp.class.getResource("view/patient_profile.fxml"));
            javafx.scene.Parent root = loader.load();

            PatientProfileController controller = loader.getController();
            controller.setPatient(selected);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Patient Profile - " + selected.getFullName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleExportPDF(ActionEvent event) {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Stage stage = (Stage) patientTable.getScene().getWindow();
            PrintUtil.printPatientProfile(stage, selected);
        }
    }
}

package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.dao.MedicalRecordDAO;
import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.MedicalRecord;
import com.odilias.odms.model.Patient;
import javafx.beans.property.SimpleStringProperty;
import com.odilias.odms.util.TaskRunner;
import com.odilias.odms.util.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import com.odilias.odms.util.PrintUtil;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class MedicalRecordController {

    @FXML
    private TextField patientSearchField;

    @FXML
    private ListView<Patient> patientListView;

    @FXML
    private TableView<MedicalRecord> recordsTable;

    @FXML
    private TableColumn<MedicalRecord, String> dateColumn;

    @FXML
    private TableColumn<MedicalRecord, String> clinicianColumn;

    @FXML
    private TableColumn<MedicalRecord, String> diagnosisColumn;

    @FXML
    private Button newRecordButton;

    @FXML
    private VBox recordDetailsBox;
    @FXML
    private Label detVitalsLabel;
    @FXML
    private Label detDiagnosisLabel;
    @FXML
    private Label detTreatmentLabel;
    @FXML
    private Label detRxLabLabel;

    private PatientDAO patientDAO = new PatientDAO();
    private MedicalRecordDAO recordDAO = new MedicalRecordDAO();
    private ObservableList<Patient> allPatients = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        setupTable();
        setupPatientList();
        loadPatients();
    }

    private void setupTable() {
        dateColumn.setCellValueFactory(cellData -> {
            var date = cellData.getValue().getRecordDate();
            return new SimpleStringProperty(date != null ? date.format(dateFormatter) : "N/A");
        });
        clinicianColumn.setCellValueFactory(new PropertyValueFactory<>("clinicianName"));
        diagnosisColumn.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));

        // Show/hide action box based on table selection
        recordsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            recordDetailsBox.setVisible(hasSelection);
            recordDetailsBox.setManaged(hasSelection);

            if (hasSelection) {
                // Populate details
                String vitals = "BP: " + (newVal.getBloodPressure() != null ? newVal.getBloodPressure() : "-") +
                        " | Temp: " + (newVal.getTemperature() != null ? newVal.getTemperature() + "°C" : "-") +
                        " | Pulse: " + (newVal.getPulseRate() != null ? newVal.getPulseRate() + "bpm" : "-") +
                        " | Weight: " + (newVal.getWeight() != null ? newVal.getWeight() + "kg" : "-");
                detVitalsLabel.setText(vitals);
                detDiagnosisLabel.setText(newVal.getDiagnosis() != null ? newVal.getDiagnosis() : "None");
                detTreatmentLabel.setText(newVal.getTreatment() != null ? newVal.getTreatment() : "None");

                String rxs = newVal.getPrescriptions() != null ? newVal.getPrescriptions() : "None";
                String labs = newVal.getLabResults() != null ? newVal.getLabResults() : "None";
                detRxLabLabel.setText("Rx: " + rxs + "\nLabs: " + labs);
            }
        });
    }

    private void setupPatientList() {
        patientListView.setCellFactory(param -> new ListCell<Patient>() {
            @Override
            protected void updateItem(Patient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFullName() + " (" + item.getPatientUuid() + ")");
                }
            }
        });

        patientListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadPatientRecords(newVal.getId());
                newRecordButton.setDisable(false);
            }
        });
    }

    private void loadPatients() {
        TaskRunner.run(() -> {
            try {
                java.util.List<Patient> patients = patientDAO.getAllPatients();
                javafx.application.Platform.runLater(() -> {
                    allPatients.setAll(patients);
                    patientListView.setItems(allPatients);
                });
            } catch (SQLException e) {
                Logger.error("Failed to load patients for medical records", e);
            }
        }, null, null);
    }

    private void loadPatientRecords(int patientId) {
        new Thread(() -> {
            try {
                java.util.List<MedicalRecord> records = recordDAO.getRecordsByPatient(patientId);
                javafx.application.Platform.runLater(() -> {
                    recordsTable.setItems(FXCollections.observableArrayList(records));
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void handlePatientSearch(KeyEvent event) {
        String query = patientSearchField.getText().toLowerCase();
        ObservableList<Patient> filtered = allPatients.filtered(p -> p.getFullName().toLowerCase().contains(query) ||
                p.getPatientUuid().toLowerCase().contains(query));
        patientListView.setItems(filtered);
    }

    @FXML
    void showAddRecordForm(ActionEvent event) {
        Patient selectedPatient = patientListView.getSelectionModel().getSelectedItem();
        if (selectedPatient == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/add_medical_record.fxml"));
            Parent root = loader.load();

            AddMedicalRecordController controller = loader.getController();
            controller.setPatient(selectedPatient);

            Stage stage = new Stage();
            stage.setTitle("New Medical Record - " + selectedPatient.getFullName());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh records
            loadPatientRecords(selectedPatient.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handlePrintRecord(ActionEvent event) {
        MedicalRecord selected = recordsTable.getSelectionModel().getSelectedItem();
        Patient patient = patientListView.getSelectionModel().getSelectedItem();
        if (selected != null && patient != null) {
            Stage stage = (Stage) recordsTable.getScene().getWindow();
            PrintUtil.printMedicalRecord(stage, selected, patient);
        }
    }
}

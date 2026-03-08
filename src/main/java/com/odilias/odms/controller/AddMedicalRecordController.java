package com.odilias.odms.controller;

import com.odilias.odms.dao.MedicalRecordDAO;
import com.odilias.odms.dao.LabDAO;
import com.odilias.odms.model.MedicalRecord;
import com.odilias.odms.model.Patient;
import com.odilias.odms.model.User;
import com.odilias.odms.model.LabTest;
import com.odilias.odms.model.Medication;
import com.odilias.odms.dao.MedicationDAO;
import com.odilias.odms.util.PrintUtil;
import com.odilias.odms.util.SessionManager;
import com.odilias.odms.util.ToastUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

public class AddMedicalRecordController {

    @FXML
    private Label patientNameLabel;
    @FXML
    private TextArea diagnosisArea;
    @FXML
    private TextArea treatmentArea;
    @FXML
    private TextArea prescriptionsArea;
    @FXML
    private TextArea labResultsArea;
    @FXML
    private TextField bpField;
    @FXML
    private TextField tempField;
    @FXML
    private TextField pulseField;
    @FXML
    private TextField weightField;
    @FXML
    private TextField rxDosageField;
    @FXML
    private TextField rxFreqField;
    @FXML
    private TextField rxDurationField;
    @FXML
    private ComboBox<LabTest> labTestComboBox;
    @FXML
    private ComboBox<Medication> medicationComboBox;
    @FXML
    private Button saveBtn;
    @FXML
    private Label errorLabel;

    private MedicalRecordDAO recordDAO = new MedicalRecordDAO();
    private LabDAO labDAO = new LabDAO();
    private MedicationDAO medicationDAO = new MedicationDAO();
    private Patient currentPatient;
    private MedicalRecord lastSavedRecord = null;

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        patientNameLabel.setText("Medical Record for: " + patient.getFullName());
    }

    @FXML
    public void initialize() {
        setupLabTestCombo();
        loadLabTests();
        setupMedicationCombo();
        loadMedications();
    }

    private void setupLabTestCombo() {
        labTestComboBox.setConverter(new StringConverter<LabTest>() {
            @Override
            public String toString(LabTest t) {
                return t == null ? "" : t.getTestName();
            }

            @Override
            public LabTest fromString(String string) {
                return null;
            }
        });
    }

    private void loadLabTests() {
        new Thread(() -> {
            try {
                List<LabTest> tests = labDAO.getAllCatalogTests();
                Platform.runLater(() -> labTestComboBox.setItems(FXCollections.observableArrayList(tests)));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupMedicationCombo() {
        medicationComboBox.setConverter(new StringConverter<Medication>() {
            @Override
            public String toString(Medication m) {
                return m == null ? "" : m.getDrugName() + " (Batch: " + m.getBatchNumber() + ")";
            }

            @Override
            public Medication fromString(String string) {
                return null;
            }
        });
    }

    private void loadMedications() {
        new Thread(() -> {
            try {
                List<Medication> meds = medicationDAO.getAllMedications();
                Platform.runLater(() -> medicationComboBox.setItems(FXCollections.observableArrayList(meds)));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void handleSave(ActionEvent event) {
        if (diagnosisArea.getText().trim().isEmpty() || treatmentArea.getText().trim().isEmpty()) {
            ToastUtil.show((Stage) saveBtn.getScene().getWindow(), "Diagnosis & Treatment are mandatory.",
                    ToastUtil.ToastType.ERROR);
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        new Thread(() -> {
            try {
                MedicalRecord record = new MedicalRecord();
                User currentUser = SessionManager.getCurrentUser();
                record.setPatientId(currentPatient.getId());
                record.setClinicianId(currentUser != null ? currentUser.getId() : 1);
                record.setClinicianName(currentUser != null ? currentUser.getFullName() : "System");
                record.setDiagnosis(diagnosisArea.getText());
                record.setTreatment(treatmentArea.getText());
                record.setPrescriptions(prescriptionsArea.getText());
                record.setLabResults(labResultsArea.getText());
                record.setRecordDate(java.time.LocalDateTime.now());

                if (labTestComboBox.getValue() != null) {
                    record.setRequestedLabTestId(labTestComboBox.getValue().getId());
                }

                if (medicationComboBox.getValue() != null) {
                    record.setRequestedMedicationId(medicationComboBox.getValue().getId());
                }

                record.setBloodPressure(bpField.getText());
                try {
                    if (!tempField.getText().isEmpty())
                        record.setTemperature(Double.parseDouble(tempField.getText()));
                    if (!pulseField.getText().isEmpty())
                        record.setPulseRate(Integer.parseInt(pulseField.getText()));
                    if (!weightField.getText().isEmpty())
                        record.setWeight(Double.parseDouble(weightField.getText()));
                } catch (NumberFormatException e) {
                }

                record.setRxDosage(rxDosageField.getText());
                record.setRxFreq(rxFreqField.getText());
                record.setRxDuration(rxDurationField.getText());

                recordDAO.createRecord(record);
                this.lastSavedRecord = record;

                Platform.runLater(() -> {
                    ToastUtil.show((Stage) saveBtn.getScene().getWindow(), "Medical Record Saved Successfully!",
                            ToastUtil.ToastType.SUCCESS);
                    saveBtn.setText("Print Record");
                    saveBtn.setDisable(false);
                    saveBtn.setOnAction(e -> handlePrint(null));
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    ToastUtil.show((Stage) saveBtn.getScene().getWindow(), "Error: " + e.getMessage(),
                            ToastUtil.ToastType.ERROR);
                    saveBtn.setDisable(false);
                    saveBtn.setText("Save Record");
                });
            }
        }).start();
    }

    @FXML
    void handlePrint(ActionEvent event) {
        if (lastSavedRecord != null) {
            PrintUtil.printMedicalRecord((Stage) saveBtn.getScene().getWindow(), lastSavedRecord, currentPatient);
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}

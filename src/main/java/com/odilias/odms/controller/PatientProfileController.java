package com.odilias.odms.controller;

import com.odilias.odms.dao.BillingDAO;
import com.odilias.odms.dao.LabDAO;
import com.odilias.odms.dao.MedicalRecordDAO;
import com.odilias.odms.dao.MedicationDAO;
import com.odilias.odms.model.*;
import com.odilias.odms.util.PrintUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class PatientProfileController {

    @FXML
    private Label patientNameLabel;
    @FXML
    private Label patientIdLabel;
    @FXML
    private Label genderAgeLabel;
    @FXML
    private Label bloodGroupLabel;
    @FXML
    private Label registeredByLabel;
    @FXML
    private Label updatedByLabel;

    @FXML
    private TableView<MedicalRecord> visitsTable;
    @FXML
    private TableColumn<MedicalRecord, LocalDateTime> visitDateCol;
    @FXML
    private TableColumn<MedicalRecord, String> visitClinicianCol;
    @FXML
    private TableColumn<MedicalRecord, String> visitDiagnosisCol;
    @FXML
    private TableColumn<MedicalRecord, String> visitTreatmentCol;

    @FXML
    private TableView<LabResult> labTable;
    @FXML
    private TableColumn<LabResult, LocalDateTime> labDateCol;
    @FXML
    private TableColumn<LabResult, String> labTestCol;
    @FXML
    private TableColumn<LabResult, String> labResultCol;
    @FXML
    private TableColumn<LabResult, String> labStatusCol;

    @FXML
    private TableView<Prescription> medsTable;
    @FXML
    private TableColumn<Prescription, LocalDateTime> medDateCol;
    @FXML
    private TableColumn<Prescription, String> medNameCol;
    @FXML
    private TableColumn<Prescription, String> medDosageCol;
    @FXML
    private TableColumn<Prescription, String> medStatusCol;
    @FXML
    private TableColumn<Prescription, String> medDispensedByCol;
    @FXML
    private TableColumn<Prescription, String> medDispensedAtCol;

    @FXML
    private TableView<Billing> billingTable;
    @FXML
    private TableColumn<Billing, LocalDateTime> billDateCol;
    @FXML
    private TableColumn<Billing, BigDecimal> billAmountCol;
    @FXML
    private TableColumn<Billing, BigDecimal> billPaidCol;
    @FXML
    private TableColumn<Billing, String> billStatusCol;
    @FXML
    private TableColumn<Billing, String> billMethodCol;

    private MedicalRecordDAO recordDAO = new MedicalRecordDAO();
    private LabDAO labDAO = new LabDAO();
    private MedicationDAO medicationDAO = new MedicationDAO();
    private BillingDAO billingDAO = new BillingDAO();
    private Patient currentPatient;

    @FXML
    public void initialize() {
        // Table Mappings
        visitDateCol.setCellValueFactory(new PropertyValueFactory<>("recordDate"));
        visitClinicianCol.setCellValueFactory(new PropertyValueFactory<>("clinicianName"));
        visitDiagnosisCol.setCellValueFactory(new PropertyValueFactory<>("diagnosis"));
        visitTreatmentCol.setCellValueFactory(new PropertyValueFactory<>("treatment"));

        labDateCol.setCellValueFactory(new PropertyValueFactory<>("requestedAt"));
        labTestCol.setCellValueFactory(new PropertyValueFactory<>("testName"));
        labResultCol.setCellValueFactory(new PropertyValueFactory<>("resultData"));
        labStatusCol.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(status != null ? status.name() : null);
        });

        medDateCol.setCellValueFactory(new PropertyValueFactory<>("prescribedAt"));
        medNameCol.setCellValueFactory(new PropertyValueFactory<>("drugName"));
        medDosageCol.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        medStatusCol.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(status != null ? status.name() : null);
        });

        java.time.format.DateTimeFormatter rxDtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        medDispensedByCol.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getDispenserName();
            return new javafx.beans.property.SimpleStringProperty(name != null ? name : "-");
        });
        medDispensedAtCol.setCellValueFactory(cellData -> {
            var dt = cellData.getValue().getDispensedAt();
            return new javafx.beans.property.SimpleStringProperty(dt != null ? dt.format(rxDtf) : "-");
        });

        billDateCol.setCellValueFactory(new PropertyValueFactory<>("billingDate"));
        billAmountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        billPaidCol.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        billStatusCol.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getPaymentStatus();
            return new javafx.beans.property.SimpleStringProperty(status != null ? status.name() : null);
        });
        billMethodCol.setCellValueFactory(cellData -> {
            var method = cellData.getValue().getPaymentMethod();
            return new javafx.beans.property.SimpleStringProperty(method != null ? method.name() : null);
        });

        setupContextMenus();
    }

    private void setupContextMenus() {
        // Visits Table Menu
        ContextMenu visitMenu = new ContextMenu();
        MenuItem printVisit = new MenuItem("Print Consultation Summary");
        printVisit.setOnAction(e -> {
            MedicalRecord selected = visitsTable.getSelectionModel().getSelectedItem();
            if (selected != null)
                PrintUtil.printMedicalRecord((Stage) visitsTable.getScene().getWindow(), selected, currentPatient);
        });
        visitMenu.getItems().add(printVisit);
        visitsTable.setContextMenu(visitMenu);

        // Lab Table Menu
        ContextMenu labMenu = new ContextMenu();
        MenuItem printLab = new MenuItem("Print Lab Report");
        printLab.setOnAction(e -> {
            LabResult selected = labTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setClinicianName("Requested Clinician"); // Fallback if not loaded
                PrintUtil.printLabResult((Stage) labTable.getScene().getWindow(), selected, currentPatient);
            }
        });
        labMenu.getItems().add(printLab);
        labTable.setContextMenu(labMenu);

        // Billing Table Menu
        ContextMenu billMenu = new ContextMenu();
        MenuItem printBill = new MenuItem("Print Receipt/Invoice");
        printBill.setOnAction(e -> {
            Billing selected = billingTable.getSelectionModel().getSelectedItem();
            if (selected != null)
                PrintUtil.printInvoice((Stage) billingTable.getScene().getWindow(), selected, currentPatient);
        });
        billMenu.getItems().add(printBill);
        billingTable.setContextMenu(billMenu);
    }

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        patientNameLabel.setText(patient.getFullName());
        patientIdLabel.setText("ID: " + patient.getPatientUuid());
        genderAgeLabel.setText(patient.getGender() + ", " + patient.getAge() + " Years");
        bloodGroupLabel.setText("Blood: " + patient.getBloodGroup());

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String regName = patient.getRegisteredByName() != null ? patient.getRegisteredByName() : "System";
        String regTime = patient.getCreatedAt() != null ? patient.getCreatedAt().format(dtf) : "N/A";
        registeredByLabel.setText("Registered By: " + regName + " on " + regTime);

        String upName = patient.getUpdatedByName() != null ? patient.getUpdatedByName() : "N/A";
        String upTime = patient.getUpdatedAt() != null ? patient.getUpdatedAt().format(dtf) : "N/A";
        updatedByLabel.setText("Last Updated: " + upName + " on " + upTime);

        loadHistory(patient.getId());
    }

    private void loadHistory(int patientId) {
        try {
            visitsTable.setItems(FXCollections.observableArrayList(recordDAO.getRecordsByPatient(patientId)));
            labTable.setItems(FXCollections.observableArrayList(labDAO.getLabHistoryByPatient(patientId)));
            medsTable.setItems(FXCollections.observableArrayList(medicationDAO.getPrescriptionsByPatient(patientId)));
            billingTable.setItems(FXCollections.observableArrayList(billingDAO.getInvoicesByPatient(patientId)));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleClose(ActionEvent event) {
        ((Stage) patientNameLabel.getScene().getWindow()).close();
    }
}

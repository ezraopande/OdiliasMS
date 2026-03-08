package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.dao.MedicationDAO;
import com.odilias.odms.model.Medication;
import com.odilias.odms.model.Prescription;
import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.sql.SQLException;
import java.time.LocalDate;

public class PharmacyController {

    @FXML
    private TableView<Medication> medicationTable;

    @FXML
    private TableColumn<Medication, String> nameCol;

    @FXML
    private TableColumn<Medication, String> batchCol;

    @FXML
    private TableColumn<Medication, Integer> qtyCol;

    @FXML
    private TableColumn<Medication, LocalDate> expCol;

    @FXML
    private TableColumn<Medication, Double> priceCol;

    @FXML
    private TableView<Prescription> prescriptionTable;
    @FXML
    private TableColumn<Prescription, String> pDateCol;
    @FXML
    private TableColumn<Prescription, String> pPatientCol;
    @FXML
    private TableColumn<Prescription, String> pPrescriberCol;
    @FXML
    private TableColumn<Prescription, String> pDrugCol;
    @FXML
    private TableColumn<Prescription, String> pDosageCol;
    @FXML
    private TableColumn<Prescription, String> pFreqCol;
    @FXML
    private TableColumn<Prescription, String> pDurationCol;

    @FXML
    private TextField searchField;

    @FXML
    private HBox inventoryActionBox;
    @FXML
    private HBox dispenseActionBox;

    private MedicationDAO medicationDAO = new MedicationDAO();
    private ObservableList<Medication> medicationList = FXCollections.observableArrayList();
    private ObservableList<Prescription> prescriptionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Inventory Columns
        nameCol.setCellValueFactory(new PropertyValueFactory<>("drugName"));
        batchCol.setCellValueFactory(new PropertyValueFactory<>("batchNumber"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        expCol.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        // Prescription Columns
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        pDateCol.setCellValueFactory(cellData -> {
            var date = cellData.getValue().getPrescribedAt();
            return new javafx.beans.property.SimpleStringProperty(date != null ? date.format(dtf) : "N/A");
        });
        pPatientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        pPrescriberCol.setCellValueFactory(new PropertyValueFactory<>("prescriberName"));
        pDrugCol.setCellValueFactory(new PropertyValueFactory<>("drugName"));
        pDosageCol.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        pFreqCol.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        pDurationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));

        medicationTable.setRowFactory(tv -> new TableRow<Medication>() {
            @Override
            protected void updateItem(Medication item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty || item.getExpiryDate() == null) {
                    setStyle("");
                } else if (!item.getExpiryDate().isAfter(LocalDate.now())) {
                    setStyle("-fx-background-color: #ffcccc;");
                    // Using background color directly highlights the row
                } else {
                    setStyle("");
                }
            }
        });

        // Show/hide action boxes based on table selection
        medicationTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            inventoryActionBox.setVisible(hasSelection);
            inventoryActionBox.setManaged(hasSelection);
        });

        prescriptionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            dispenseActionBox.setVisible(hasSelection);
            dispenseActionBox.setManaged(hasSelection);
        });

        loadData();
    }

    private void loadData() {
        TaskRunner.run(() -> {
            try {
                java.util.List<Medication> meds = medicationDAO.getAllMedications();
                java.util.List<Prescription> prescriptions = medicationDAO.getPendingPrescriptions();
                javafx.application.Platform.runLater(() -> {
                    medicationList.setAll(meds);
                    medicationTable.setItems(medicationList);
                    prescriptionList.setAll(prescriptions);
                    prescriptionTable.setItems(prescriptionList);
                });
            } catch (SQLException e) {
                Logger.error("Failed to load pharmacy data", e);
            }
        }, null, null);
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadData();
    }

    @FXML
    void handleAddMedication(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/add_medication.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add New Medication");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData(); // Refresh after adding
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String query = searchField.getText().toLowerCase();
        ObservableList<Medication> filtered = medicationList
                .filtered(m -> m.getDrugName().toLowerCase().contains(query) ||
                        m.getBatchNumber().toLowerCase().contains(query));
        medicationTable.setItems(filtered);
    }

    @FXML
    void handleUpdateStock(ActionEvent event) {
        Medication selected = medicationTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Update Stock");
        dialog.setHeaderText("Add to Stock for: " + selected.getDrugName());
        dialog.setContentText("Quantity to add:");

        dialog.showAndWait().ifPresent(qty -> {
            try {
                int q = Integer.parseInt(qty);
                medicationDAO.updateStock(selected.getId(), q);
                loadData();
            } catch (NumberFormatException | SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void handleDispense(ActionEvent event) {
        Prescription selected = prescriptionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Please select a prescription to dispense.");
            alert.showAndWait();
            return;
        }

        Medication mappingMed = medicationList.stream()
                .filter(m -> selected.getMedicationId() != null && m.getId() == selected.getMedicationId())
                .findFirst()
                .orElse(null);

        if (mappingMed != null) {
            if (!mappingMed.getExpiryDate().isAfter(LocalDate.now())) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Expired Medication");
                alert.setHeaderText("Dispensing Blocked");
                alert.setContentText("This medication has expired and cannot be dispensed!");
                alert.showAndWait();
                return;
            }
            if (mappingMed.getQuantity() < 1) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Out of Stock");
                alert.setHeaderText("Dispensing Blocked");
                alert.setContentText("This medication is completely out of stock!");
                alert.showAndWait();
                return;
            }
        }

        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Dispense Medication");

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        String prescribedAtStr = selected.getPrescribedAt() != null ? selected.getPrescribedAt().format(dtf)
                : "Unknown Date";
        String prescriber = selected.getPrescriberName() != null ? selected.getPrescriberName() : "Unknown Doctor";

        dialog.setHeaderText("Dispensing: " + selected.getDrugName() + "\n"
                + "Prescribed By: " + prescriber + " on " + prescribedAtStr);
        dialog.setContentText("Enter Quantity dispensed:");

        dialog.showAndWait().ifPresent(qty -> {
            try {
                int q = Integer.parseInt(qty);
                if (mappingMed != null && mappingMed.getQuantity() < q) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText(
                            "Cannot dispense " + q + " units. Only " + mappingMed.getQuantity() + " left in stock.");
                    alert.showAndWait();
                    return;
                }
                User currentUser = SessionManager.getCurrentUser();
                int userId = currentUser != null ? currentUser.getId() : 1;

                medicationDAO.dispensePrescription(selected, q, userId);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Dispensing Complete");
                success.setHeaderText(null);
                success.setContentText("Stock updated and Invoice generated for: " + selected.getPatientName());
                success.showAndWait();

                loadData();
            } catch (NumberFormatException | SQLException e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setContentText("Error: " + e.getMessage());
                error.showAndWait();
                e.printStackTrace();
            }
        });
    }
}

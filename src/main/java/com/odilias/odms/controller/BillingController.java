package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.dao.BillingDAO;
import com.odilias.odms.model.Billing;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.scene.layout.HBox;
import com.odilias.odms.util.PrintUtil;
import com.odilias.odms.util.TaskRunner;
import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.Patient;
import com.odilias.odms.model.Payment;

public class BillingController {

    @FXML
    private TableView<Billing> billingTable;

    @FXML
    private TableColumn<Billing, String> dateColumn;

    @FXML
    private TableColumn<Billing, String> patientColumn;

    @FXML
    private TableColumn<Billing, BigDecimal> totalColumn;

    @FXML
    private TableColumn<Billing, BigDecimal> paidColumn;

    @FXML
    private TableColumn<Billing, String> statusColumn;

    @FXML
    private TableColumn<Billing, BigDecimal> balanceColumn;

    @FXML
    private HBox actionBox;

    private PatientDAO patientDAO = new PatientDAO();
    private BillingDAO billingDAO = new BillingDAO();
    private ObservableList<Billing> billingList = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        dateColumn.setCellValueFactory(cellData -> {
            var date = cellData.getValue().getBillingDate();
            return new SimpleStringProperty(date != null ? date.format(dateFormatter) : "N/A");
        });
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        paidColumn.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        statusColumn.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getPaymentStatus();
            return new SimpleStringProperty(status != null ? status.name() : null);
        });
        balanceColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getBalance()));

        // Show/hide action box based on table selection
        billingTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            actionBox.setVisible(hasSelection);
            actionBox.setManaged(hasSelection);
        });

        loadInvoices();
    }

    private void loadInvoices() {
        // Must extract UI component values on the JavaFX Application Thread BEFORE
        // running the background task
        TaskRunner.run(() -> {
            try {
                java.util.List<Billing> invoices = billingDAO.getAllInvoices();
                javafx.application.Platform.runLater(() -> {
                    billingList.setAll(invoices);
                    billingTable.setItems(billingList);
                });
            } catch (SQLException e) {
                com.odilias.odms.util.Logger.error("Failed to load invoices", e);
            }
        }, null, null);
    }

    @FXML
    void showCreateInvoiceForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/create_invoice.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Create New Invoice");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadInvoices();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleRecordPayment(ActionEvent event) {
        Billing selected = billingTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        TextInputDialog dialog = new TextInputDialog(selected.getBalance().toString());
        dialog.setTitle("Record Payment");
        dialog.setHeaderText("Patient: " + selected.getPatientName() + "\nBalance: KES " + selected.getBalance());
        dialog.setContentText("Enter amount to pay:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(amountStr -> {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                // For simplicity, default to CASH in this dialog
                billingDAO.recordPayment(selected.getId(), amount, Billing.PaymentMethod.CASH);
                loadInvoices();
            } catch (NumberFormatException | SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void handlePrintInvoice(ActionEvent event) {
        Billing selected = billingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Patient patient = patientDAO.getPatientById(selected.getPatientId());
                if (patient != null) {
                    // Populate items and history before passing to PrintUtil
                    selected.setItems(billingDAO.getInvoiceItems(selected.getId()));
                    List<Payment> history = billingDAO.getPaymentHistory(selected.getId());

                    Stage stage = (Stage) billingTable.getScene().getWindow();
                    PrintUtil.printInvoiceWithHistory(stage, selected, patient, history);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

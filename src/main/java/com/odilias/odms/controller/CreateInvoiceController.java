package com.odilias.odms.controller;

import com.odilias.odms.dao.BillingDAO;
import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.*;
import com.odilias.odms.util.PrintUtil;
import com.odilias.odms.util.ToastUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreateInvoiceController {

    @FXML
    private ComboBox<Patient> patientComboBox;
    @FXML
    private ComboBox<Service> serviceComboBox;
    @FXML
    private TableView<InvoiceItem> itemTable;
    @FXML
    private TableColumn<InvoiceItem, String> itemNameCol;
    @FXML
    private TableColumn<InvoiceItem, BigDecimal> itemPriceCol;
    @FXML
    private Label totalLabel;
    @FXML
    private TextField paidAmountField;
    @FXML
    private ComboBox<Billing.PaymentMethod> paymentMethodComboBox;
    @FXML
    private Label errorLabel;
    @FXML
    private Button createBtn;

    private BillingDAO billingDAO = new BillingDAO();
    private PatientDAO patientDAO = new PatientDAO();
    private ObservableList<InvoiceItem> selectedItems = FXCollections.observableArrayList();
    private BigDecimal currentTotal = BigDecimal.ZERO;
    private Billing lastCreatedBilling = null;

    @FXML
    public void initialize() {
        setupConverters();
        itemNameCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        itemPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemTable.setItems(selectedItems);
        paymentMethodComboBox.setItems(FXCollections.observableArrayList(Billing.PaymentMethod.values()));

        // Load data in background for speed
        totalLabel.setText("Loading data...");
        new Thread(this::loadInitialData).start();
    }

    private void setupConverters() {
        patientComboBox.setConverter(new StringConverter<Patient>() {
            @Override
            public String toString(Patient p) {
                return p == null ? "" : p.getFullName() + " [" + p.getPatientUuid() + "]";
            }

            @Override
            public Patient fromString(String string) {
                return null;
            }
        });
        serviceComboBox.setConverter(new StringConverter<Service>() {
            @Override
            public String toString(Service s) {
                return s == null ? "" : s.getServiceName() + " (KES " + s.getBasePrice() + ")";
            }

            @Override
            public Service fromString(String string) {
                return null;
            }
        });
    }

    private void loadInitialData() {
        try {
            List<Patient> patients = patientDAO.getAllPatients();
            List<Service> services = billingDAO.getAvailableServices();
            Platform.runLater(() -> {
                patientComboBox.setItems(FXCollections.observableArrayList(patients));
                serviceComboBox.setItems(FXCollections.observableArrayList(services));
                totalLabel.setText("KES 0.00");
            });
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> errorLabel.setText("Error loading catalog."));
        }
    }

    @FXML
    void handleAddItem(ActionEvent event) {
        Service service = serviceComboBox.getValue();
        if (service == null)
            return;

        InvoiceItem item = new InvoiceItem();
        item.setItemType(InvoiceItem.ItemType.SERVICE);
        item.setItemReferenceId(service.getId());
        item.setItemName(service.getServiceName());
        item.setQuantity(1);
        item.setUnitPrice(service.getBasePrice());
        item.setSubtotal(service.getBasePrice());

        selectedItems.add(item);
        updateTotal();
    }

    private void updateTotal() {
        currentTotal = BigDecimal.ZERO;
        for (InvoiceItem item : selectedItems) {
            currentTotal = currentTotal.add(item.getSubtotal());
        }
        totalLabel.setText("KES " + currentTotal.toString());
    }

    @FXML
    void handleCreate(ActionEvent event) {
        Patient patient = patientComboBox.getValue();
        if (patient == null || selectedItems.isEmpty()) {
            ToastUtil.show((Stage) totalLabel.getScene().getWindow(), "Please select patient and items.",
                    ToastUtil.ToastType.ERROR);
            return;
        }

        createBtn.setDisable(true);
        createBtn.setText("Processing...");

        new Thread(() -> {
            try {
                BigDecimal paidAmount = BigDecimal.ZERO;
                if (!paidAmountField.getText().isEmpty()) {
                    paidAmount = new BigDecimal(paidAmountField.getText());
                }

                Billing billing = new Billing();
                billing.setPatientId(patient.getId());
                billing.setTotalAmount(currentTotal);
                billing.setPaidAmount(paidAmount);
                billing.setItems(new ArrayList<>(selectedItems));

                if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                    final Billing.PaymentMethod method = paymentMethodComboBox.getValue();
                    if (method == null) {
                        Platform.runLater(() -> {
                            errorLabel.setText("Please select payment method.");
                            createBtn.setDisable(false);
                            createBtn.setText("Create & Finalize");
                        });
                        return;
                    }
                    billing.setPaymentMethod(method);
                    billing.setPaymentStatus(paidAmount.compareTo(currentTotal) >= 0 ? Billing.PaymentStatus.PAID
                            : Billing.PaymentStatus.PARTIAL);
                } else {
                    billing.setPaymentStatus(Billing.PaymentStatus.PENDING);
                }

                billingDAO.createInvoice(billing);
                this.lastCreatedBilling = billing;

                Platform.runLater(() -> {
                    ToastUtil.show((Stage) totalLabel.getScene().getWindow(), "Invoice Created Successfully!",
                            ToastUtil.ToastType.SUCCESS);
                    // Instead of closing, let's offer printing
                    createBtn.setText("Print Invoice");
                    createBtn.setDisable(false);
                    createBtn.setOnAction(e -> handlePrint(null));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    ToastUtil.show((Stage) totalLabel.getScene().getWindow(), "Error: " + e.getMessage(),
                            ToastUtil.ToastType.ERROR);
                    createBtn.setDisable(false);
                    createBtn.setText("Create & Finalize");
                });
            }
        }).start();
    }

    @FXML
    void handlePrint(ActionEvent event) {
        if (lastCreatedBilling != null) {
            PrintUtil.printInvoice((Stage) totalLabel.getScene().getWindow(), lastCreatedBilling,
                    patientComboBox.getValue());
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        ((Stage) totalLabel.getScene().getWindow()).close();
    }
}

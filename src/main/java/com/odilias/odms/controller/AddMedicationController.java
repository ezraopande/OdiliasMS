package com.odilias.odms.controller;

import com.odilias.odms.dao.MedicationDAO;
import com.odilias.odms.model.Medication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;

public class AddMedicationController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField batchField;

    @FXML
    private TextField qtyField;

    @FXML
    private DatePicker expiryDatePicker;

    @FXML
    private TextField priceField;

    @FXML
    private Label errorLabel;

    private MedicationDAO medicationDAO = new MedicationDAO();

    @FXML
    void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String batch = batchField.getText();
        String qtyStr = qtyField.getText();
        String priceStr = priceField.getText();

        if (name.isEmpty() || batch.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()
                || expiryDatePicker.getValue() == null) {
            errorLabel.setText("Please fill in all mandatory fields (*)");
            return;
        }

        if (expiryDatePicker.getValue().isBefore(java.time.LocalDate.now().plusDays(1))) {
            errorLabel.setText("Expiry date must be in the future.");
            return;
        }

        try {
            Medication med = new Medication();
            med.setDrugName(name);
            med.setBatchNumber(batch);
            med.setQuantity(Integer.parseInt(qtyStr));
            med.setExpiryDate(expiryDatePicker.getValue());
            med.setUnitPrice(new BigDecimal(priceStr));

            medicationDAO.createMedication(med);

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid format for quantity or price.");
        } catch (SQLException e) {
            errorLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}

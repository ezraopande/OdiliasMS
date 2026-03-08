package com.odilias.odms.controller;

import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.Patient;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import com.odilias.odms.model.User;
import com.odilias.odms.util.SessionManager;

import java.sql.SQLException;
import java.util.UUID;

public class AddPatientController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField ageField;

    @FXML
    private ComboBox<String> genderComboBox;

    @FXML
    private TextField contactField;

    @FXML
    private TextField addressField;

    @FXML
    private Label infoLabel;

    @FXML
    private TextField nationalIdField;

    @FXML
    private DatePicker dobPicker;

    @FXML
    private TextField emailField;

    @FXML
    private TextField emergencyContactField;

    @FXML
    private ComboBox<String> bloodGroupComboBox;

    @FXML
    private TextArea allergiesArea;

    private PatientDAO patientDAO = new PatientDAO();

    @FXML
    public void initialize() {
        genderComboBox.setItems(FXCollections.observableArrayList("MALE", "FEMALE", "OTHER"));
        bloodGroupComboBox.setItems(FXCollections.observableArrayList(
                "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "UNKNOWN"));

        dobPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                int calculatedAge = java.time.Period.between(newValue, java.time.LocalDate.now()).getYears();
                ageField.setText(String.valueOf(calculatedAge));
            } else {
                ageField.clear();
            }
        });
    }

    @FXML
    void handleSave(ActionEvent event) {
        String name = nameField.getText();
        String ageStr = ageField.getText();
        String gender = genderComboBox.getValue();
        String contact = contactField.getText();
        String address = addressField.getText();
        String nationalId = nationalIdField.getText();
        String email = emailField.getText();
        String emergencyContact = emergencyContactField.getText();
        String bloodGroup = bloodGroupComboBox.getValue();
        String allergies = allergiesArea.getText();
        java.time.LocalDate dob = dobPicker.getValue();

        if (name.isEmpty() || ageStr.isEmpty() || gender == null || contact.isEmpty() || nationalId.isEmpty()
                || emergencyContact.isEmpty()) {
            infoLabel.setText("Please fill in all mandatory fields (*)");
            return;
        }

        try {
            // Check for duplicates
            if (patientDAO.isDuplicatePatient(nationalId, contact)) {
                infoLabel.setText("A patient with this National ID or Contact Number already exists.");
                return;
            }

            int age = Integer.parseInt(ageStr);

            Patient patient = new Patient();
            patient.setPatientUuid("PAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            patient.setFullName(name);
            patient.setAge(age);
            patient.setGender(gender);
            patient.setContactNumber(contact);
            patient.setAddress(address);
            patient.setNationalId(nationalId);
            patient.setDob(dob);
            patient.setEmail(email);
            patient.setEmergencyContact(emergencyContact);
            patient.setBloodGroup(bloodGroup != null ? bloodGroup : "UNKNOWN");
            patient.setAllergies(allergies);

            User currentUser = SessionManager.getCurrentUser();
            patient.setRegisteredByName(currentUser != null ? currentUser.getFullName() : "System");

            patientDAO.createPatient(patient);

            // Close the window on success
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();

        } catch (NumberFormatException e) {
            infoLabel.setText("Age must be a valid number.");
        } catch (SQLException e) {
            infoLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}

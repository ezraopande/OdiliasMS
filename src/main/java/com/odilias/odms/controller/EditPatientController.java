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

public class EditPatientController {

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

    private Patient currentPatient;

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        nameField.setText(patient.getFullName());
        ageField.setText(String.valueOf(patient.getAge()));
        genderComboBox.setValue(patient.getGender());
        contactField.setText(patient.getContactNumber());
        addressField.setText(patient.getAddress());
        nationalIdField.setText(patient.getNationalId());
        dobPicker.setValue(patient.getDob());
        emailField.setText(patient.getEmail());
        emergencyContactField.setText(patient.getEmergencyContact());
        bloodGroupComboBox.setValue(patient.getBloodGroup());
        allergiesArea.setText(patient.getAllergies());

        // National ID shouldn't typically change, but we will leave it editable for
        // correction
    }

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
            // Check for duplicates only if changing nationalId or contact
            if ((!nationalId.equals(currentPatient.getNationalId())
                    || !contact.equals(currentPatient.getContactNumber()))
                    && patientDAO.isDuplicatePatient(nationalId, contact)) {
                infoLabel.setText("A patient with this National ID or Contact Number already exists.");
                return;
            }

            int age = Integer.parseInt(ageStr);

            currentPatient.setFullName(name);
            currentPatient.setAge(age);
            currentPatient.setGender(gender);
            currentPatient.setContactNumber(contact);
            currentPatient.setAddress(address);
            currentPatient.setNationalId(nationalId);
            currentPatient.setDob(dob);
            currentPatient.setEmail(email);
            currentPatient.setEmergencyContact(emergencyContact);
            currentPatient.setBloodGroup(bloodGroup != null ? bloodGroup : "UNKNOWN");
            currentPatient.setAllergies(allergies);

            User currentUser = SessionManager.getCurrentUser();
            currentPatient.setUpdatedByName(currentUser != null ? currentUser.getFullName() : "Admin");
            currentPatient.setUpdateNotes("Updated via Edit Patient workflow");

            patientDAO.updatePatient(currentPatient);

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

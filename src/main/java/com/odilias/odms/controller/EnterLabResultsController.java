package com.odilias.odms.controller;

import com.odilias.odms.dao.LabDAO;
import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.LabResult;
import com.odilias.odms.model.Patient;
import com.odilias.odms.model.User;
import com.odilias.odms.util.PrintUtil;
import com.odilias.odms.util.SessionManager;
import com.odilias.odms.util.ToastUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EnterLabResultsController {

    @FXML
    private Label testLabel;
    @FXML
    private Label patientLabel;
    @FXML
    private TextArea resultField;
    @FXML
    private TextField rangeField;
    @FXML
    private TextArea remarksField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button submitBtn;

    private LabDAO labDAO = new LabDAO();
    private PatientDAO patientDAO = new PatientDAO();
    private LabResult currentResult;
    private Patient currentPatient;

    public void setLabResult(LabResult result) {
        this.currentResult = result;
        testLabel.setText("Test: " + result.getTestName());
        patientLabel.setText("Patient: " + result.getPatientName());

        // Fetch full patient data in background for printing
        new Thread(() -> {
            try {
                this.currentPatient = patientDAO.getPatientByConsultationId(result.getConsultationId());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    void handleSave(ActionEvent event) {
        if (resultField.getText().trim().isEmpty()) {
            ToastUtil.show((Stage) submitBtn.getScene().getWindow(), "Please enter the test result findings.",
                    ToastUtil.ToastType.ERROR);
            return;
        }

        submitBtn.setDisable(true);
        submitBtn.setText("Submitting...");

        new Thread(() -> {
            try {
                User currentUser = SessionManager.getCurrentUser();
                currentResult.setResultData(resultField.getText());
                currentResult.setReferenceRange(rangeField.getText());
                currentResult.setComments(remarksField.getText());
                currentResult.setStatus(LabResult.Status.COMPLETED);
                currentResult.setConductedBy(currentUser != null ? currentUser.getId() : 1);
                currentResult.setCompletedAt(java.time.LocalDateTime.now());

                labDAO.updateLabResult(currentResult);

                Platform.runLater(() -> {
                    ToastUtil.show((Stage) submitBtn.getScene().getWindow(), "Lab results submitted successfully!",
                            ToastUtil.ToastType.SUCCESS);
                    submitBtn.setText("Print Report");
                    submitBtn.setDisable(false);
                    submitBtn.setOnAction(e -> handlePrint(null));
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    errorLabel.setText("Database error: " + e.getMessage());
                    submitBtn.setDisable(false);
                    submitBtn.setText("Submit Lab Result");
                });
            }
        }).start();
    }

    @FXML
    void handlePrint(ActionEvent event) {
        if (currentResult != null && currentPatient != null) {
            PrintUtil.printLabResult((Stage) submitBtn.getScene().getWindow(), currentResult, currentPatient);
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        ((Stage) resultField.getScene().getWindow()).close();
    }
}

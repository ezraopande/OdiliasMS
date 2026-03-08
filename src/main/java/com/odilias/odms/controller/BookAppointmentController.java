package com.odilias.odms.controller;

import com.odilias.odms.dao.AppointmentDAO;
import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.Appointment;
import com.odilias.odms.model.Patient;
import com.odilias.odms.model.User;
import com.odilias.odms.util.DatabaseConnection;
import com.odilias.odms.util.ToastUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookAppointmentController {

    @FXML
    private ComboBox<Patient> patientComboBox;
    @FXML
    private ComboBox<User> staffComboBox;
    @FXML
    private ComboBox<String> timeComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextArea reasonArea;
    @FXML
    private Label errorLabel;
    @FXML
    private Button bookBtn;

    private AppointmentDAO appointmentDAO = new AppointmentDAO();
    private PatientDAO patientDAO = new PatientDAO();

    @FXML
    public void initialize() {
        setupComboBuilders();
        setupTimeSlots();
        new Thread(this::loadData).start();
    }

    private void setupTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime time = LocalTime.of(8, 0);
        while (!time.isAfter(LocalTime.of(18, 0))) {
            slots.add(time.toString());
            time = time.plusMinutes(30);
        }
        timeComboBox.setItems(FXCollections.observableArrayList(slots));
    }

    private void setupComboBuilders() {
        patientComboBox.setConverter(new StringConverter<Patient>() {
            @Override
            public String toString(Patient p) {
                return p == null ? "" : p.getFullName() + " (" + p.getPatientUuid() + ")";
            }

            @Override
            public Patient fromString(String string) {
                return null;
            }
        });

        staffComboBox.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User u) {
                return u == null ? "" : u.getFullName() + " [" + u.getRole() + "]";
            }

            @Override
            public User fromString(String string) {
                return null;
            }
        });
    }

    private void loadData() {
        try {
            List<Patient> patients = patientDAO.getAllPatients();
            List<User> medicalStaff = new ArrayList<>();
            String query = "SELECT u.id, u.full_name, r.role_name FROM users u " +
                    "JOIN roles r ON u.role_id = r.id " +
                    "WHERE r.role_name IN ('CLINICAL_OFFICER', 'NURSE', 'ADMIN') AND u.status='ACTIVE'";

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setRole(User.Role.valueOf(rs.getString("role_name")));
                    medicalStaff.add(u);
                }
            }

            Platform.runLater(() -> {
                patientComboBox.setItems(FXCollections.observableArrayList(patients));
                staffComboBox.setItems(FXCollections.observableArrayList(medicalStaff));
                datePicker.setValue(LocalDate.now());

                // Disable past dates in UI
                datePicker.setDayCellFactory(picker -> new DateCell() {
                    @Override
                    public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date.isBefore(LocalDate.now()));
                    }
                });
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleBook(ActionEvent event) {
        Patient patient = patientComboBox.getValue();
        User staff = staffComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String timeStr = timeComboBox.getValue();
        String reason = reasonArea.getText();

        if (patient == null || staff == null || date == null || timeStr == null) {
            errorLabel.setText("Please fill in all mandatory fields.");
            return;
        }

        LocalTime time = LocalTime.parse(timeStr);
        LocalDateTime apptDateTime = LocalDateTime.of(date, time);

        if (apptDateTime.isBefore(LocalDateTime.now())) {
            errorLabel.setText("Cannot book appointments in the past.");
            return;
        }

        bookBtn.setDisable(true);
        bookBtn.setText("Booking...");

        new Thread(() -> {
            try {
                // CHECK FOR DUPLICATES
                if (appointmentDAO.isDuplicateAppointment(staff.getId(), date, time)) {
                    Platform.runLater(() -> {
                        ToastUtil.show((Stage) errorLabel.getScene().getWindow(), "Doctor already booked at this time.",
                                ToastUtil.ToastType.ERROR);
                        bookBtn.setDisable(false);
                        bookBtn.setText("Confirm Booking");
                    });
                    return;
                }

                Appointment appt = new Appointment();
                appt.setPatientId(patient.getId());
                appt.setStaffId(staff.getId());
                appt.setAppointmentTime(apptDateTime);
                appt.setReason(reason);
                appt.setStatus(Appointment.Status.SCHEDULED);

                appointmentDAO.createAppointment(appt);

                Platform.runLater(() -> {
                    ToastUtil.show((Stage) errorLabel.getScene().getWindow(), "Appointment Booked Successfully!",
                            ToastUtil.ToastType.SUCCESS);
                    ((Stage) errorLabel.getScene().getWindow()).close();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorLabel.setText("Database error: " + e.getMessage());
                    bookBtn.setDisable(false);
                    bookBtn.setText("Confirm Booking");
                });
            }
        }).start();
    }

    @FXML
    void handleCancel(ActionEvent event) {
        ((Stage) patientComboBox.getScene().getWindow()).close();
    }
}

package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.dao.AppointmentDAO;
import com.odilias.odms.model.Appointment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.scene.layout.HBox;
import com.odilias.odms.util.PrintUtil;
import com.odilias.odms.util.TaskRunner;
import com.odilias.odms.util.Logger;

public class AppointmentController {

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Appointment> appointmentTable;

    @FXML
    private TableColumn<Appointment, String> idColumn;

    @FXML
    private TableColumn<Appointment, String> timeColumn;

    @FXML
    private TableColumn<Appointment, String> patientColumn;

    @FXML
    private TableColumn<Appointment, String> staffColumn;

    @FXML
    private TableColumn<Appointment, String> reasonColumn;

    @FXML
    private TableColumn<Appointment, String> statusColumn;

    @FXML
    private HBox actionBox;

    private AppointmentDAO appointmentDAO = new AppointmentDAO();
    private ObservableList<Appointment> appointmentList = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Appointment> filteredAppointments;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());

        idColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("APT-%04d", cellData.getValue().getId())));
        timeColumn.setCellValueFactory(cellData -> {
            var time = cellData.getValue().getAppointmentTime();
            return new SimpleStringProperty(time != null ? time.format(timeFormatter) : "N/A");
        });
        patientColumn.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        staffColumn.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        statusColumn.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status.name() : null);
        });

        statusColumn.setCellFactory(col -> new javafx.scene.control.TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("SCHEDULED")) {
                        setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    } else if (item.equals("COMPLETED")) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else if (item.equals("CANCELLED")) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        filteredAppointments = new javafx.collections.transformation.FilteredList<>(appointmentList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredAppointments.setPredicate(appt -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (appt.getPatientName() != null && appt.getPatientName().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (appt.getReason() != null && appt.getReason().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (String.format("apt-%04d", appt.getId()).contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        appointmentTable.setItems(filteredAppointments);

        // Show/hide action box based on selection
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            actionBox.setVisible(hasSelection);
            actionBox.setManaged(hasSelection);
        });

        loadAppointments();
    }

    private void loadAppointments() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            return;
        }

        TaskRunner.run(() -> {
            try {
                java.util.List<Appointment> appts = appointmentDAO.getAppointmentsByDate(selectedDate);
                javafx.application.Platform.runLater(() -> appointmentList.setAll(appts));
            } catch (SQLException e) {
                Logger.error("Failed to load appointments", e);
            }
        }, null, null);
    }

    @FXML
    void handleDateChange(ActionEvent event) {
        loadAppointments();
    }

    @FXML
    void showBookAppointmentForm(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/book_appointment.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Book Appointment");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadAppointments();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getStatus() == Appointment.Status.COMPLETED) {
                com.odilias.odms.util.ToastUtil.show((Stage) appointmentTable.getScene().getWindow(),
                        "Completed appointments cannot be cancelled.", com.odilias.odms.util.ToastUtil.ToastType.ERROR);
                return;
            }
            try {
                appointmentDAO.updateStatus(selected.getId(), Appointment.Status.CANCELLED);
                loadAppointments();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleComplete(ActionEvent event) {
        Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getStatus() == Appointment.Status.CANCELLED) {
                com.odilias.odms.util.ToastUtil.show((Stage) appointmentTable.getScene().getWindow(),
                        "Cancelled appointments cannot be marked as completed.",
                        com.odilias.odms.util.ToastUtil.ToastType.ERROR);
                return;
            }
            try {
                appointmentDAO.updateStatus(selected.getId(), Appointment.Status.COMPLETED);
                loadAppointments();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleReschedule(ActionEvent event) {
        Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        if (selected.getStatus() == Appointment.Status.COMPLETED
                || selected.getStatus() == Appointment.Status.CANCELLED) {
            com.odilias.odms.util.ToastUtil.show((Stage) appointmentTable.getScene().getWindow(),
                    "Only scheduled or pending appointments can be rescheduled.",
                    com.odilias.odms.util.ToastUtil.ToastType.ERROR);
            return;
        }

        javafx.scene.control.Dialog<java.time.LocalDateTime> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Reschedule Appointment");
        dialog.setHeaderText("Reschedule Appointment for " + selected.getPatientName());

        javafx.scene.control.ButtonType saveButtonType = new javafx.scene.control.ButtonType("Save",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        DatePicker newDatePicker = new DatePicker(selected.getAppointmentTime().toLocalDate());
        javafx.scene.control.ComboBox<String> timeComboBox = new javafx.scene.control.ComboBox<>();
        java.util.List<String> slots = new java.util.ArrayList<>();
        java.time.LocalTime t = java.time.LocalTime.of(8, 0);
        while (!t.isAfter(java.time.LocalTime.of(18, 0))) {
            slots.add(t.toString());
            t = t.plusMinutes(30);
        }
        timeComboBox.setItems(FXCollections.observableArrayList(slots));
        timeComboBox.setValue(selected.getAppointmentTime().toLocalTime().toString());

        grid.add(new javafx.scene.control.Label("New Date:"), 0, 0);
        grid.add(newDatePicker, 1, 0);
        grid.add(new javafx.scene.control.Label("New Time:"), 0, 1);
        grid.add(timeComboBox, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return java.time.LocalDateTime.of(newDatePicker.getValue(),
                        java.time.LocalTime.parse(timeComboBox.getValue()));
            }
            return null;
        });

        java.util.Optional<java.time.LocalDateTime> result = dialog.showAndWait();
        result.ifPresent(newDateTime -> {
            if (newDateTime.isBefore(java.time.LocalDateTime.now())) {
                com.odilias.odms.util.ToastUtil.show((Stage) appointmentTable.getScene().getWindow(),
                        "Cannot reschedule to a past date.", com.odilias.odms.util.ToastUtil.ToastType.ERROR);
                return;
            }
            try {
                if (appointmentDAO.isDuplicateAppointment(selected.getStaffId(), newDateTime.toLocalDate(),
                        newDateTime.toLocalTime()) && !newDateTime.equals(selected.getAppointmentTime())) {
                    com.odilias.odms.util.ToastUtil.show((Stage) appointmentTable.getScene().getWindow(),
                            "Doctor already booked at this time.", com.odilias.odms.util.ToastUtil.ToastType.ERROR);
                    return;
                }
                appointmentDAO.updateDateTime(selected.getId(), newDateTime);
                appointmentDAO.updateStatus(selected.getId(), Appointment.Status.SCHEDULED);
                com.odilias.odms.util.ToastUtil.show((Stage) appointmentTable.getScene().getWindow(),
                        "Appointment Rescheduled!", com.odilias.odms.util.ToastUtil.ToastType.SUCCESS);
                loadAppointments();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void handlePrintSlip(ActionEvent event) {
        Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Stage stage = (Stage) appointmentTable.getScene().getWindow();
            PrintUtil.printAppointmentSlip(stage, selected);
        }
    }
}

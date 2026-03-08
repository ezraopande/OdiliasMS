package com.odilias.odms.controller;

import com.odilias.odms.MainApp;
import com.odilias.odms.dao.LabDAO;
import com.odilias.odms.model.LabResult;
import com.odilias.odms.model.LabTest;
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
import java.time.LocalDateTime;
import com.odilias.odms.dao.PatientDAO;
import com.odilias.odms.model.Patient;
import com.odilias.odms.util.PrintUtil;
import com.odilias.odms.util.TaskRunner;
import com.odilias.odms.util.Logger;

public class LaboratoryController {

    @FXML
    private TableView<LabResult> requestTable;
    @FXML
    private TableColumn<LabResult, String> patientCol;
    @FXML
    private TableColumn<LabResult, String> testNameCol;
    @FXML
    private TableColumn<LabResult, String> clinicianCol;
    @FXML
    private TableColumn<LabResult, LocalDateTime> timeCol;
    @FXML
    private TableColumn<LabResult, String> statusCol;

    @FXML
    private TableView<LabTest> catalogTable;
    @FXML
    private TableColumn<LabTest, String> catalogNameCol;
    @FXML
    private TableColumn<LabTest, String> catalogDescCol;
    @FXML
    private TableColumn<LabTest, Double> catalogCostCol;

    @FXML
    private HBox labActionBox;

    private PatientDAO patientDAO = new PatientDAO();
    private LabDAO labDAO = new LabDAO();
    private ObservableList<LabResult> requestList = FXCollections.observableArrayList();
    private ObservableList<LabTest> catalogList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Request Table mappings
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        testNameCol.setCellValueFactory(new PropertyValueFactory<>("testName"));
        clinicianCol.setCellValueFactory(new PropertyValueFactory<>("clinicianName"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("requestedAt"));
        statusCol.setCellValueFactory(cellData -> {
            var status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(status != null ? status.name() : null);
        });

        // Catalog Table mappings
        catalogNameCol.setCellValueFactory(new PropertyValueFactory<>("testName"));
        catalogDescCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        catalogCostCol.setCellValueFactory(new PropertyValueFactory<>("cost"));

        // Show/hide action box based on table selection
        requestTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            labActionBox.setVisible(hasSelection);
            labActionBox.setManaged(hasSelection);
        });

        loadData();
    }

    private void loadData() {
        TaskRunner.run(() -> {
            try {
                java.util.List<LabResult> requests = labDAO.getActiveLabRequests();
                java.util.List<LabTest> catalog = labDAO.getAllCatalogTests();
                javafx.application.Platform.runLater(() -> {
                    requestList.setAll(requests);
                    requestTable.setItems(requestList);
                    catalogList.setAll(catalog);
                    catalogTable.setItems(catalogList);
                });
            } catch (SQLException e) {
                Logger.error("Failed to load laboratory data", e);
            }
        }, null, null);
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        loadData();
    }

    @FXML
    void handleEnterResults(ActionEvent event) {
        LabResult selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selection Required");
            alert.setHeaderText(null);
            alert.setContentText("Please select a lab request from the list.");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("view/enter_lab_results.fxml"));
            Parent root = loader.load();

            EnterLabResultsController controller = loader.getController();
            controller.setLabResult(selected);

            Stage stage = new Stage();
            stage.setTitle("Lab Result Entry");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handlePrintLabReport(ActionEvent event) {
        LabResult selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selection Required");
            alert.setHeaderText(null);
            alert.setContentText("Please select a lab result from the list.");
            alert.showAndWait();
            return;
        }

        if (selected.getStatus() != LabResult.Status.COMPLETED) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Results Not Ready");
            alert.setHeaderText("Cannot Print Lab Report");
            alert.setContentText(
                    "This lab test has not been completed yet. Please enter results before printing.\n\nCurrent Status: "
                            + selected.getStatus().name());
            alert.showAndWait();
            return;
        }

        try {
            Patient patient = patientDAO.getPatientById(selected.getPatientId());
            if (patient != null) {
                Stage stage = (Stage) requestTable.getScene().getWindow();
                PrintUtil.printLabResult(stage, selected, patient);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

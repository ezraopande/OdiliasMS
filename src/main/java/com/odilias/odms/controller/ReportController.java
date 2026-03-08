package com.odilias.odms.controller;

import com.odilias.odms.util.DatabaseConnection;
import com.odilias.odms.util.TaskRunner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;

public class ReportController {

    @FXML
    private Label dailyPatientsLabel;
    @FXML
    private Label dailyRevenueLabel;
    @FXML
    private Label monthlyPatientsLabel;
    @FXML
    private Label monthlyRevenueLabel;

    @FXML
    private Label totalPatientsLabel;
    @FXML
    private Label totalApptsLabel;
    @FXML
    private Label pendingApptsLabel;
    @FXML
    private Label activeStaffLabel;

    @FXML
    public void initialize() {
        TaskRunner.run(() -> {
            loadDailyStats();
            loadMonthlyStats();
            loadOverallStats();
        }, null, null);
    }

    private void loadDailyStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Daily Total Patients
            String q1 = "SELECT COUNT(*) FROM patients WHERE DATE(created_at) = CURDATE()";
            try (PreparedStatement stmt = conn.prepareStatement(q1)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt(1);
                    javafx.application.Platform.runLater(() -> dailyPatientsLabel.setText(String.valueOf(count)));
                }
            }

            // Daily Revenue
            String q2 = "SELECT SUM(paid_amount) FROM invoices WHERE DATE(generated_at) = CURDATE()";
            try (PreparedStatement stmt = conn.prepareStatement(q2)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal(1);
                    String val = "KES " + (total != null ? total.setScale(2, RoundingMode.HALF_UP) : "0.00");
                    javafx.application.Platform.runLater(() -> dailyRevenueLabel.setText(val));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMonthlyStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            LocalDate now = LocalDate.now();
            String startOfMonth = now.withDayOfMonth(1).toString();

            // Total Patients this month
            String patientQuery = "SELECT COUNT(*) FROM patients WHERE created_at >= ?";
            try (PreparedStatement stmt = conn.prepareStatement(patientQuery)) {
                stmt.setString(1, startOfMonth);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt(1);
                    javafx.application.Platform.runLater(() -> monthlyPatientsLabel.setText(String.valueOf(count)));
                }
            }

            // Total Revenue this month
            String revenueQuery = "SELECT SUM(paid_amount) FROM invoices WHERE generated_at >= ?";
            try (PreparedStatement stmt = conn.prepareStatement(revenueQuery)) {
                stmt.setString(1, startOfMonth);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal(1);
                    String val = "KES " + (total != null ? total.setScale(0, RoundingMode.HALF_UP) : "0");
                    javafx.application.Platform.runLater(() -> monthlyRevenueLabel.setText(val));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadOverallStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            executeStat(conn, "SELECT COUNT(*) FROM patients", totalPatientsLabel);
            executeStat(conn, "SELECT COUNT(*) FROM appointments", totalApptsLabel);
            executeStat(conn, "SELECT COUNT(*) FROM appointments WHERE status = 'PENDING'", pendingApptsLabel);
            executeStat(conn, "SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'", activeStaffLabel);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeStat(Connection conn, String sql, Label label) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String val = String.valueOf(rs.getInt(1));
                javafx.application.Platform.runLater(() -> label.setText(val));
            }
        }
    }

    @FXML
    void generateFinancialReport(ActionEvent event) {
        exportToCSV("Revenue_Report", "SELECT * FROM invoices");
    }

    @FXML
    void generateVisitReport(ActionEvent event) {
        exportToCSV("Patient_Visit_Report", "SELECT * FROM patients");
    }

    @FXML
    void generateStaffActivityReport(ActionEvent event) {
        exportToCSV("Staff_Report", "SELECT id, username, full_name, role_id, status FROM users");
    }

    private void exportToCSV(String fileNamePrefix, String query) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName(fileNamePrefix + "_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(dailyPatientsLabel.getScene().getWindow());
        if (file != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    FileWriter writer = new FileWriter(file)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    writer.append(metaData.getColumnName(i)).append(i == columnCount ? "" : ",");
                }
                writer.append("\n");

                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        String value = rs.getString(i);
                        writer.append(value != null ? value.replace(",", ";") : "").append(i == columnCount ? "" : ",");
                    }
                    writer.append("\n");
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}

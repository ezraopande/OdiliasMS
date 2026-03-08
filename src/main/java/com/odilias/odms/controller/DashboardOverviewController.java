package com.odilias.odms.controller;

import com.odilias.odms.util.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import java.sql.*;
import java.math.BigDecimal;

public class DashboardOverviewController {

    @FXML
    private Label totalPatientsLabel;
    @FXML
    private Label todayAppointmentsLabel;
    @FXML
    private Label totalBilledLabel;
    @FXML
    private Label totalPaidLabel;
    @FXML
    private Label pendingPaymentsLabel;

    @FXML
    private LineChart<String, Number> revenueChart;
    @FXML
    private PieChart paymentPieChart;
    @FXML
    private BarChart<String, Number> drugSalesChart;
    @FXML
    private ListView<String> lowStockList;

    @FXML
    public void initialize() {
        // Run all heavy analytics in a background thread to keep UI smooth
        new Thread(() -> {
            loadSummaryStats();
            loadFinancialCharts();
            loadInventoryAnalytics();
        }).start();
    }

    private void loadSummaryStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            executeStat(conn, "SELECT COUNT(*) FROM patients", totalPatientsLabel);
            executeStat(conn, "SELECT COUNT(*) FROM appointments WHERE DATE(appointment_date) = CURDATE()",
                    todayAppointmentsLabel);

            String query = "SELECT SUM(total_amount), SUM(paid_amount), SUM(total_amount - paid_amount) FROM invoices WHERE status != 'CANCELLED'";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    String billed = "KES " + formatMoney(rs.getBigDecimal(1));
                    String paid = "KES " + formatMoney(rs.getBigDecimal(2));
                    String pending = "KES " + formatMoney(rs.getBigDecimal(3));

                    Platform.runLater(() -> {
                        totalBilledLabel.setText(billed);
                        totalPaidLabel.setText(paid);
                        pendingPaymentsLabel.setText(pending);
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadFinancialCharts() {
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenue (KES)");

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String lineQ = "SELECT DATE(generated_at) as d, SUM(paid_amount) FROM invoices " +
                    "WHERE generated_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                    "GROUP BY DATE(generated_at) ORDER BY d ASC";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(lineQ)) {
                while (rs.next()) {
                    revenueSeries.getData().add(new XYChart.Data<>(rs.getString(1), rs.getDouble(2)));
                }
            }

            String pieQ = "SELECT status, SUM(total_amount) FROM invoices GROUP BY status";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(pieQ)) {
                while (rs.next()) {
                    pieData.add(new PieChart.Data(rs.getString(1), rs.getDouble(2)));
                }
            }

            Platform.runLater(() -> {
                revenueChart.getData().clear();
                revenueChart.getData().add(revenueSeries);
                paymentPieChart.setData(pieData);
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadInventoryAnalytics() {
        XYChart.Series<String, Number> drugSeries = new XYChart.Series<>();
        drugSeries.setName("Quantity Issued");
        ObservableList<String> lowStockItems = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String barQ = "SELECT item_name, SUM(quantity) FROM invoice_items WHERE item_type = 'MEDICATION' " +
                    "GROUP BY item_name ORDER BY SUM(quantity) DESC LIMIT 5";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(barQ)) {
                while (rs.next()) {
                    drugSeries.getData().add(new XYChart.Data<>(rs.getString(1), rs.getInt(2)));
                }
            }

            String stockQ = "SELECT drug_name, quantity FROM medications WHERE quantity < 50 ORDER BY quantity ASC LIMIT 10";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(stockQ)) {
                while (rs.next()) {
                    lowStockItems.add(rs.getString(1) + " (Rem: " + rs.getInt(2) + ")");
                }
            }

            Platform.runLater(() -> {
                drugSalesChart.getData().clear();
                drugSalesChart.getData().add(drugSeries);
                lowStockList.setItems(lowStockItems);
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeStat(Connection conn, String sql, Label label) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String val = String.valueOf(rs.getInt(1));
                Platform.runLater(() -> label.setText(val));
            }
        }
    }

    private String formatMoney(BigDecimal amount) {
        return amount != null ? String.format("%,.0f", amount) : "0";
    }
}

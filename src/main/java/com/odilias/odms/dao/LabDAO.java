package com.odilias.odms.dao;

import com.odilias.odms.model.LabResult;
import com.odilias.odms.model.LabTest;
import com.odilias.odms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LabDAO {

    public List<LabTest> getAllCatalogTests() throws SQLException {
        List<LabTest> tests = new ArrayList<>();
        String query = "SELECT * FROM lab_tests ORDER BY test_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                LabTest t = new LabTest(
                        rs.getInt("id"),
                        rs.getString("test_name"),
                        rs.getString("description"),
                        rs.getBigDecimal("cost"));
                tests.add(t);
            }
        }
        return tests;
    }

    public List<LabResult> getActiveLabRequests() throws SQLException {
        List<LabResult> requests = new ArrayList<>();
        String query = "SELECT lr.*, lt.test_name, p.id as patient_id, p.full_name as patient_name, u.full_name as clinician_name "
                +
                "FROM lab_results lr " +
                "JOIN lab_tests lt ON lr.lab_test_id = lt.id " +
                "JOIN consultations c ON lr.consultation_id = c.id " +
                "JOIN patients p ON c.patient_id = p.id " +
                "JOIN users u ON lr.requested_by = u.id " +
                "WHERE lr.status != 'COMPLETED' " +
                "ORDER BY lr.requested_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                LabResult lr = mapLabResult(rs);
                lr.setTestName(rs.getString("test_name"));
                lr.setPatientName(rs.getString("patient_name"));
                lr.setClinicianName(rs.getString("clinician_name"));
                lr.setPatientId(rs.getInt("patient_id"));
                requests.add(lr);
            }
        }
        return requests;
    }

    public List<LabResult> getLabHistoryByPatient(int patientId) throws SQLException {
        List<LabResult> results = new ArrayList<>();
        String query = "SELECT lr.*, lt.test_name, c.patient_id, u.full_name as clinician_name FROM lab_results lr " +
                "JOIN lab_tests lt ON lr.lab_test_id = lt.id " +
                "JOIN consultations c ON lr.consultation_id = c.id " +
                "JOIN users u ON lr.requested_by = u.id " +
                "WHERE c.patient_id = ? ORDER BY lr.requested_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LabResult lr = mapLabResult(rs);
                    lr.setTestName(rs.getString("test_name"));
                    lr.setClinicianName(rs.getString("clinician_name"));
                    lr.setPatientId(rs.getInt("patient_id"));
                    results.add(lr);
                }
            }
        }
        return results;
    }

    public void updateLabResult(LabResult lr) throws SQLException {
        String query = "UPDATE lab_results SET status = ?, result_data = ?, reference_range = ?, comments = ?, conducted_by = ?, completed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, lr.getStatus().name());
            stmt.setString(2, lr.getResultData());
            stmt.setString(3, lr.getReferenceRange());
            stmt.setString(4, lr.getComments());
            stmt.setInt(5, lr.getConductedBy());
            stmt.setInt(6, lr.getId());
            stmt.executeUpdate();
            AuditDAO.log(lr.getConductedBy(), "LAB", "Completed lab test results for ID: " + lr.getId());
        }
    }

    private LabResult mapLabResult(ResultSet rs) throws SQLException {
        LabResult lr = new LabResult();
        lr.setId(rs.getInt("id"));
        lr.setConsultationId(rs.getInt("consultation_id"));
        lr.setLabTestId(rs.getInt("lab_test_id"));
        lr.setRequestedBy(rs.getInt("requested_by"));
        lr.setStatus(LabResult.Status.valueOf(rs.getString("status")));
        lr.setResultData(rs.getString("result_data"));
        lr.setReferenceRange(rs.getString("reference_range"));
        lr.setComments(rs.getString("comments"));
        lr.setConductedBy(rs.getObject("conducted_by", Integer.class));
        Timestamp reqTime = rs.getTimestamp("requested_at");
        if (reqTime != null)
            lr.setRequestedAt(reqTime.toLocalDateTime());
        Timestamp compTime = rs.getTimestamp("completed_at");
        if (compTime != null)
            lr.setCompletedAt(compTime.toLocalDateTime());
        return lr;
    }
}

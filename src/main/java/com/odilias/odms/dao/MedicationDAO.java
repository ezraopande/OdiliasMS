package com.odilias.odms.dao;

import com.odilias.odms.model.Medication;
import com.odilias.odms.model.Prescription;
import com.odilias.odms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicationDAO {

    public List<Medication> getAllMedications() throws SQLException {
        List<Medication> meds = new ArrayList<>();
        String query = "SELECT * FROM medications ORDER BY drug_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Medication m = new Medication();
                m.setId(rs.getInt("id"));
                m.setDrugName(rs.getString("drug_name"));
                m.setBatchNumber(rs.getString("batch_number"));
                m.setQuantity(rs.getInt("quantity"));
                Date exp = rs.getDate("expiry_date");
                if (exp != null)
                    m.setExpiryDate(exp.toLocalDate());
                m.setUnitPrice(rs.getBigDecimal("unit_price"));
                meds.add(m);
            }
        }
        return meds;
    }

    public void createMedication(Medication m) throws SQLException {
        String query = "INSERT INTO medications (drug_name, batch_number, quantity, expiry_date, unit_price) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, m.getDrugName());
            stmt.setString(2, m.getBatchNumber());
            stmt.setInt(3, m.getQuantity());
            if (m.getExpiryDate() != null)
                stmt.setDate(4, Date.valueOf(m.getExpiryDate()));
            else
                stmt.setNull(4, Types.DATE);
            stmt.setBigDecimal(5, m.getUnitPrice());
            stmt.executeUpdate();
            AuditDAO.log(null, "PHARMACY", "Added medication: " + m.getDrugName());
        }
    }

    public void updateStock(int id, int qtyChange) throws SQLException {
        String query = "UPDATE medications SET quantity = quantity + ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, qtyChange);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public List<Prescription> getPendingPrescriptions() throws SQLException {
        List<Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT pr.*, m.drug_name, m.unit_price, p.full_name as patient_name, u.full_name as prescriber_name "
                +
                "FROM prescriptions pr " +
                "JOIN medications m ON pr.medication_id = m.id " +
                "JOIN consultations c ON pr.consultation_id = c.id " +
                "JOIN patients p ON c.patient_id = p.id " +
                "JOIN users u ON c.clinician_id = u.id " +
                "WHERE pr.status = 'PENDING' " +
                "ORDER BY pr.prescribed_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Prescription p = new Prescription();
                p.setId(rs.getInt("id"));
                p.setConsultationId(rs.getInt("consultation_id"));
                p.setMedicationId(rs.getInt("medication_id"));
                p.setDrugName(rs.getString("drug_name"));
                p.setDosage(rs.getString("dosage"));
                p.setFrequency(rs.getString("frequency"));
                p.setDuration(rs.getString("duration"));
                p.setPatientName(rs.getString("patient_name"));
                p.setUnitPrice(rs.getDouble("unit_price"));
                p.setPrescriberName(rs.getString("prescriber_name"));
                Timestamp pa = rs.getTimestamp("prescribed_at");
                if (pa != null)
                    p.setPrescribedAt(pa.toLocalDateTime());
                prescriptions.add(p);
            }
        }
        return prescriptions;
    }

    public void dispensePrescription(Prescription p, int qtyDispensed, int dispensedBy) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 0. Get patient_id from consultation
                int patientId = -1;
                String q0 = "SELECT patient_id FROM consultations WHERE id = ?";
                try (PreparedStatement s0 = conn.prepareStatement(q0)) {
                    s0.setInt(1, p.getConsultationId());
                    try (ResultSet rs = s0.executeQuery()) {
                        if (rs.next())
                            patientId = rs.getInt(1);
                    }
                }

                // 1. Deduct stock
                String deductQuery = "UPDATE medications SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
                try (PreparedStatement ds = conn.prepareStatement(deductQuery)) {
                    ds.setInt(1, qtyDispensed);
                    ds.setInt(2, p.getMedicationId());
                    ds.setInt(3, qtyDispensed);
                    int updated = ds.executeUpdate();
                    if (updated == 0)
                        throw new SQLException("Insufficient stock or medication not found.");
                }

                // 2. Mark prescription status
                String updatePrescription = "UPDATE prescriptions SET status = 'DISPENSED', dispensed_by = ?, dispensed_at = NOW() WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updatePrescription)) {
                    ps.setInt(1, dispensedBy);
                    ps.setInt(2, p.getId());
                    ps.executeUpdate();
                }

                // 3. Generate Invoice for the Pharmacy Sale
                BigDecimal unitPrice = BigDecimal.valueOf(p.getUnitPrice());
                BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(qtyDispensed));

                String q3 = "INSERT INTO invoices (patient_id, consultation_id, total_amount, paid_amount, status, generated_by) VALUES (?, ?, ?, ?, ?, ?)";
                int invoiceId = -1;
                try (PreparedStatement s3 = conn.prepareStatement(q3, Statement.RETURN_GENERATED_KEYS)) {
                    s3.setInt(1, patientId);
                    s3.setInt(2, p.getConsultationId());
                    s3.setBigDecimal(3, totalAmount);
                    s3.setBigDecimal(4, BigDecimal.ZERO);
                    s3.setString(5, "PENDING");
                    s3.setInt(6, dispensedBy);
                    s3.executeUpdate();
                    try (ResultSet rs = s3.getGeneratedKeys()) {
                        if (rs.next())
                            invoiceId = rs.getInt(1);
                    }
                }

                // 4. Record Invoice Item
                String q4 = "INSERT INTO invoice_items (invoice_id, item_type, item_reference_id, item_name, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement s4 = conn.prepareStatement(q4)) {
                    s4.setInt(1, invoiceId);
                    s4.setString(2, "MEDICATION");
                    s4.setInt(3, p.getMedicationId());
                    s4.setString(4, p.getDrugName());
                    s4.setInt(5, qtyDispensed);
                    s4.setBigDecimal(6, unitPrice);
                    s4.setBigDecimal(7, totalAmount);
                    s4.executeUpdate();
                }

                conn.commit();
                AuditDAO.log(dispensedBy, "PHARMACY",
                        "Dispensed & Billed medication: " + p.getDrugName() + " (Qty: " + qtyDispensed
                                + ") for Patient ID: " + patientId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Prescription> getPrescriptionsByPatient(int patientId) throws SQLException {
        List<Prescription> prescriptions = new ArrayList<>();
        String query = "SELECT pr.*, m.drug_name, du.full_name as dispenser_name FROM prescriptions pr " +
                "JOIN medications m ON pr.medication_id = m.id " +
                "JOIN consultations c ON pr.consultation_id = c.id " +
                "LEFT JOIN users du ON pr.dispensed_by = du.id " +
                "WHERE c.patient_id = ? ORDER BY pr.prescribed_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Prescription p = new Prescription();
                    p.setId(rs.getInt("id"));
                    p.setConsultationId(rs.getInt("consultation_id"));
                    p.setDrugName(rs.getString("drug_name"));
                    p.setDosage(rs.getString("dosage"));
                    p.setFrequency(rs.getString("frequency"));
                    p.setStatus(Prescription.Status.valueOf(rs.getString("status")));
                    Timestamp pa = rs.getTimestamp("prescribed_at");
                    if (pa != null)
                        p.setPrescribedAt(pa.toLocalDateTime());
                    Timestamp da = rs.getTimestamp("dispensed_at");
                    if (da != null)
                        p.setDispensedAt(da.toLocalDateTime());
                    p.setDispenserName(rs.getString("dispenser_name"));
                    prescriptions.add(p);
                }
            }
        }
        return prescriptions;
    }
}

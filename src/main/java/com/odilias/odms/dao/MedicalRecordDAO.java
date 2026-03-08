package com.odilias.odms.dao;

import com.odilias.odms.model.MedicalRecord;
import com.odilias.odms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO {

    public void createRecord(MedicalRecord record) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert consultation
                String q1 = "INSERT INTO consultations (patient_id, clinician_id, appointment_id, clinical_notes) VALUES (?, ?, ?, ?)";
                int consultationId = -1;
                try (PreparedStatement s1 = conn.prepareStatement(q1, Statement.RETURN_GENERATED_KEYS)) {
                    s1.setInt(1, record.getPatientId());
                    s1.setInt(2, record.getClinicianId());
                    if (record.getAppointmentId() != null) {
                        s1.setInt(3, record.getAppointmentId());
                    } else {
                        s1.setNull(3, Types.INTEGER);
                    }
                    s1.setString(4, record.getTreatment() != null ? record.getTreatment() : "");
                    s1.executeUpdate();
                    try (ResultSet rs = s1.getGeneratedKeys()) {
                        if (rs.next())
                            consultationId = rs.getInt(1);
                    }
                }

                if (consultationId != -1) {
                    // 2. Insert diagnosis
                    if (record.getDiagnosis() != null && !record.getDiagnosis().trim().isEmpty()) {
                        String q2 = "INSERT INTO diagnosis (consultation_id, disease_name) VALUES (?, ?)";
                        try (PreparedStatement s2 = conn.prepareStatement(q2)) {
                            s2.setInt(1, consultationId);
                            s2.setString(2, record.getDiagnosis());
                            s2.executeUpdate();
                        }
                    }
                    // 3. Insert prescription
                    if (record.getRequestedMedicationId() != null) {
                        String q3 = "INSERT INTO prescriptions (consultation_id, medication_id, dosage, frequency, duration, custom_drug_name) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement s3 = conn.prepareStatement(q3)) {
                            s3.setInt(1, consultationId);
                            s3.setInt(2, record.getRequestedMedicationId());
                            s3.setString(3, record.getRxDosage() != null ? record.getRxDosage() : "");
                            s3.setString(4, record.getRxFreq() != null ? record.getRxFreq() : "");
                            s3.setString(5, record.getRxDuration() != null ? record.getRxDuration() : "");
                            s3.setString(6, record.getPrescriptions() != null ? record.getPrescriptions() : "");
                            s3.executeUpdate();
                        }
                    } else if (record.getPrescriptions() != null && !record.getPrescriptions().trim().isEmpty()) {
                        String q3 = "INSERT INTO prescriptions (consultation_id, custom_drug_name, dosage, frequency, duration) VALUES (?, ?, ?, ?, ?)";
                        try (PreparedStatement s3 = conn.prepareStatement(q3)) {
                            s3.setInt(1, consultationId);
                            s3.setString(2, record.getPrescriptions());
                            s3.setString(3, record.getRxDosage() != null ? record.getRxDosage() : "");
                            s3.setString(4, record.getRxFreq() != null ? record.getRxFreq() : "");
                            s3.setString(5, record.getRxDuration() != null ? record.getRxDuration() : "");
                            s3.executeUpdate();
                        }
                    }
                    // 4. Lab Request (using requestedLabTestId from the model)
                    if (record.getRequestedLabTestId() != null) {
                        String q4 = "INSERT INTO lab_results (consultation_id, lab_test_id, requested_by, result_data) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement s4 = conn.prepareStatement(q4)) {
                            s4.setInt(1, consultationId);
                            s4.setInt(2, record.getRequestedLabTestId());
                            s4.setInt(3, record.getClinicianId());
                            s4.setString(4, record.getLabResults() != null ? record.getLabResults() : "");
                            s4.executeUpdate();
                        }
                    } else if (record.getLabResults() != null && !record.getLabResults().trim().isEmpty()) {
                        // If no test is selected but results are typed (legacy/notes)
                        String q4 = "INSERT INTO lab_results (consultation_id, lab_test_id, requested_by, result_data) VALUES (?, 1, ?, ?)";
                        try (PreparedStatement s4 = conn.prepareStatement(q4)) {
                            s4.setInt(1, consultationId);
                            s4.setInt(2, record.getClinicianId());
                            s4.setString(3, record.getLabResults());
                            s4.executeUpdate();
                        }
                    }
                    // 5. Insert Vitals
                    if (record.getBloodPressure() != null || record.getTemperature() != null) {
                        String q5 = "INSERT INTO vitals (consultation_id, blood_pressure, temperature, pulse_rate, weight, recorded_by) VALUES (?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement s5 = conn.prepareStatement(q5)) {
                            s5.setInt(1, consultationId);
                            s5.setString(2, record.getBloodPressure());
                            if (record.getTemperature() != null)
                                s5.setDouble(3, record.getTemperature());
                            else
                                s5.setNull(3, Types.DECIMAL);
                            if (record.getPulseRate() != null)
                                s5.setInt(4, record.getPulseRate());
                            else
                                s5.setNull(4, Types.INTEGER);
                            if (record.getWeight() != null)
                                s5.setDouble(5, record.getWeight());
                            else
                                s5.setNull(5, Types.DECIMAL);
                            s5.setInt(6, record.getClinicianId());
                            s5.executeUpdate();
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<MedicalRecord> getRecordsByPatient(int patientId) throws SQLException {
        List<MedicalRecord> records = new ArrayList<>();
        String query = "SELECT c.id, c.patient_id, c.clinician_id, c.appointment_id, c.consultation_date as record_date, c.clinical_notes as treatment, "
                +
                "u.full_name as clinician_name, " +
                "(SELECT GROUP_CONCAT(disease_name SEPARATOR ', ') FROM diagnosis WHERE consultation_id = c.id) as diagnosis, "
                +
                "(SELECT GROUP_CONCAT(custom_drug_name SEPARATOR ', ') FROM prescriptions WHERE consultation_id = c.id) as prescriptions, "
                +
                "(SELECT GROUP_CONCAT(result_data SEPARATOR ', ') FROM lab_results WHERE consultation_id = c.id) as lab_results, "
                +
                "v.blood_pressure, v.temperature, v.pulse_rate, v.weight " +
                "FROM consultations c " +
                "JOIN users u ON c.clinician_id = u.id " +
                "LEFT JOIN vitals v ON v.consultation_id = c.id " +
                "WHERE c.patient_id = ? ORDER BY c.consultation_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MedicalRecord record = new MedicalRecord();
                    record.setId(rs.getInt("id")); // consultation id
                    record.setPatientId(rs.getInt("patient_id"));
                    record.setClinicianId(rs.getInt("clinician_id"));
                    record.setAppointmentId(rs.getObject("appointment_id", Integer.class));
                    record.setDiagnosis(rs.getString("diagnosis"));
                    record.setTreatment(rs.getString("treatment"));
                    record.setPrescriptions(rs.getString("prescriptions"));
                    record.setLabResults(rs.getString("lab_results"));
                    record.setBloodPressure(rs.getString("blood_pressure"));
                    record.setTemperature(rs.getObject("temperature", Double.class));
                    record.setPulseRate(rs.getObject("pulse_rate", Integer.class));
                    record.setWeight(rs.getObject("weight", Double.class));
                    record.setRecordDate(rs.getTimestamp("record_date").toLocalDateTime());
                    record.setClinicianName(rs.getString("clinician_name"));
                    records.add(record);
                }
            }
        }
        return records;
    }
}

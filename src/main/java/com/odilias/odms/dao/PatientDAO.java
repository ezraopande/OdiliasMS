package com.odilias.odms.dao;

import com.odilias.odms.model.Patient;
import com.odilias.odms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    public boolean isDuplicatePatient(String nationalId, String contactNumber) throws SQLException {
        if ((nationalId == null || nationalId.isEmpty()) && (contactNumber == null || contactNumber.isEmpty())) {
            return false;
        }
        String query = "SELECT COUNT(*) FROM patients WHERE national_id = ? OR contact_number = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, nationalId);
            stmt.setString(2, contactNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public void createPatient(Patient patient) throws SQLException {
        String query = "INSERT INTO patients (patient_uuid, full_name, dob, age, gender, contact_number, email, address, national_id, emergency_contact, blood_group, allergies, registered_by_name) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, patient.getPatientUuid());
            stmt.setString(2, patient.getFullName());
            if (patient.getDob() != null) {
                stmt.setDate(3, java.sql.Date.valueOf(patient.getDob()));
            } else {
                stmt.setNull(3, Types.DATE);
            }
            stmt.setInt(4, patient.getAge());
            stmt.setString(5, patient.getGender());
            stmt.setString(6, patient.getContactNumber());
            stmt.setString(7, patient.getEmail());
            stmt.setString(8, patient.getAddress());
            stmt.setString(9, patient.getNationalId());
            stmt.setString(10, patient.getEmergencyContact());
            stmt.setString(11, patient.getBloodGroup());
            stmt.setString(12, patient.getAllergies());
            stmt.setString(13, patient.getRegisteredByName());
            stmt.executeUpdate();
        }
    }

    public void updatePatient(Patient patient) throws SQLException {
        String query = "UPDATE patients SET full_name=?, dob=?, age=?, gender=?, contact_number=?, email=?, address=?, emergency_contact=?, blood_group=?, allergies=?, updated_by_name=?, update_notes=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, patient.getFullName());
            if (patient.getDob() != null) {
                stmt.setDate(2, java.sql.Date.valueOf(patient.getDob()));
            } else {
                stmt.setNull(2, Types.DATE);
            }
            stmt.setInt(3, patient.getAge());
            stmt.setString(4, patient.getGender());
            stmt.setString(5, patient.getContactNumber());
            stmt.setString(6, patient.getEmail());
            stmt.setString(7, patient.getAddress());
            stmt.setString(8, patient.getEmergencyContact());
            stmt.setString(9, patient.getBloodGroup());
            stmt.setString(10, patient.getAllergies());
            stmt.setString(11, patient.getUpdatedByName());
            stmt.setString(12, patient.getUpdateNotes());
            stmt.setInt(13, patient.getId());
            stmt.executeUpdate();
        }
    }

    public List<Patient> getAllPatients() throws SQLException {
        List<Patient> patients = new ArrayList<>();
        String query = "SELECT * FROM patients";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                patients.add(mapPatient(rs));
            }
        }
        return patients;
    }

    public Patient getPatientById(int id) throws SQLException {
        String query = "SELECT * FROM patients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPatient(rs);
                }
            }
        }
        return null;
    }

    public Patient getPatientByConsultationId(int consultationId) throws SQLException {
        String query = "SELECT p.* FROM patients p JOIN consultations c ON p.id = c.patient_id WHERE c.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, consultationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapPatient(rs);
                }
            }
        }
        return null;
    }

    private Patient mapPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("id"));
        patient.setPatientUuid(rs.getString("patient_uuid"));
        patient.setFullName(rs.getString("full_name"));
        java.sql.Date dob = rs.getDate("dob");
        if (dob != null)
            patient.setDob(dob.toLocalDate());
        patient.setAge(rs.getInt("age"));
        patient.setGender(rs.getString("gender"));
        patient.setContactNumber(rs.getString("contact_number"));
        patient.setEmail(rs.getString("email"));
        patient.setAddress(rs.getString("address"));
        patient.setNationalId(rs.getString("national_id"));
        patient.setEmergencyContact(rs.getString("emergency_contact"));
        patient.setBloodGroup(rs.getString("blood_group"));
        patient.setAllergies(rs.getString("allergies"));

        Timestamp cAt = rs.getTimestamp("created_at");
        if (cAt != null)
            patient.setCreatedAt(cAt.toLocalDateTime());
        Timestamp uAt = rs.getTimestamp("updated_at");
        if (uAt != null)
            patient.setUpdatedAt(uAt.toLocalDateTime());
        patient.setRegisteredByName(rs.getString("registered_by_name"));
        patient.setUpdatedByName(rs.getString("updated_by_name"));
        patient.setUpdateNotes(rs.getString("update_notes"));

        return patient;
    }
}

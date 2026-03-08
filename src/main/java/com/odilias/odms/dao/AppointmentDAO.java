package com.odilias.odms.dao;

import com.odilias.odms.model.Appointment;
import com.odilias.odms.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDAO {

    public boolean isDuplicateAppointment(int staffId, LocalDate date, LocalTime time) throws SQLException {
        String query = "SELECT COUNT(*) FROM appointments WHERE staff_id = ? AND appointment_date = ? AND appointment_time = ? AND status != 'CANCELLED'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, staffId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setTime(3, Time.valueOf(time));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public void createAppointment(Appointment appt) throws SQLException {
        String query = "INSERT INTO appointments (patient_id, staff_id, appointment_date, appointment_time, reason, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, appt.getPatientId());
            stmt.setInt(2, appt.getStaffId());
            stmt.setDate(3, java.sql.Date.valueOf(appt.getAppointmentTime().toLocalDate()));
            stmt.setTime(4, java.sql.Time.valueOf(appt.getAppointmentTime().toLocalTime()));
            stmt.setString(5, appt.getReason());
            stmt.setString(6, appt.getStatus().name());
            stmt.executeUpdate();
        }
    }

    public List<Appointment> getAppointmentsByDate(LocalDate date) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        String query = "SELECT a.*, p.full_name as patient_name, u.full_name as staff_name " +
                "FROM appointments a " +
                "JOIN patients p ON a.patient_id = p.id " +
                "LEFT JOIN users u ON a.staff_id = u.id " +
                "WHERE a.appointment_date = ? " +
                "ORDER BY a.appointment_time ASC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Appointment appt = new Appointment();
                    appt.setId(rs.getInt("id"));
                    appt.setPatientId(rs.getInt("patient_id"));
                    appt.setStaffId(rs.getInt("staff_id"));
                    java.sql.Date sqlDate = rs.getDate("appointment_date");
                    java.sql.Time sqlTime = rs.getTime("appointment_time");
                    if (sqlDate != null && sqlTime != null) {
                        appt.setAppointmentTime(LocalDateTime.of(sqlDate.toLocalDate(), sqlTime.toLocalTime()));
                    }
                    appt.setReason(rs.getString("reason"));
                    appt.setStatus(Appointment.Status.valueOf(rs.getString("status")));
                    appt.setPatientName(rs.getString("patient_name"));
                    appt.setStaffName(rs.getString("staff_name"));
                    appointments.add(appt);
                }
            }
        }
        return appointments;
    }

    public void updateStatus(int appointmentId, Appointment.Status status) throws SQLException {
        String query = "UPDATE appointments SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, appointmentId);
            stmt.executeUpdate();
        }
    }

    public void updateDateTime(int appointmentId, LocalDateTime apptDateTime) throws SQLException {
        String query = "UPDATE appointments SET appointment_date = ?, appointment_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(apptDateTime.toLocalDate()));
            stmt.setTime(2, java.sql.Time.valueOf(apptDateTime.toLocalTime()));
            stmt.setInt(3, appointmentId);
            stmt.executeUpdate();
        }
    }
}

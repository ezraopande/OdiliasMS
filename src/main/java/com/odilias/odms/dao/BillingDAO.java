package com.odilias.odms.dao;

import com.odilias.odms.model.Billing;
import com.odilias.odms.model.InvoiceItem;
import com.odilias.odms.model.Payment;
import com.odilias.odms.model.Service;
import com.odilias.odms.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillingDAO {

    public void createInvoice(Billing billing) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String q1 = "INSERT INTO invoices (patient_id, consultation_id, total_amount, paid_amount, status) VALUES (?, ?, ?, ?, ?)";
                int invoiceId = -1;
                try (PreparedStatement s1 = conn.prepareStatement(q1, Statement.RETURN_GENERATED_KEYS)) {
                    s1.setInt(1, billing.getPatientId());
                    if (billing.getConsultationId() != null)
                        s1.setInt(2, billing.getConsultationId());
                    else
                        s1.setNull(2, Types.INTEGER);
                    s1.setBigDecimal(3, billing.getTotalAmount());
                    s1.setBigDecimal(4, billing.getPaidAmount());
                    s1.setString(5, billing.getPaymentStatus().name());
                    s1.executeUpdate();
                    try (ResultSet rs = s1.getGeneratedKeys()) {
                        if (rs.next())
                            invoiceId = rs.getInt(1);
                    }
                }

                if (invoiceId != -1) {
                    // Insert items
                    String q2 = "INSERT INTO invoice_items (invoice_id, item_type, item_reference_id, item_name, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement s2 = conn.prepareStatement(q2)) {
                        for (InvoiceItem item : billing.getItems()) {
                            s2.setInt(1, invoiceId);
                            s2.setString(2, item.getItemType().name());
                            s2.setInt(3, item.getItemReferenceId());
                            s2.setString(4, item.getItemName());
                            s2.setInt(5, item.getQuantity());
                            s2.setBigDecimal(6, item.getUnitPrice());
                            s2.setBigDecimal(7, item.getSubtotal());
                            s2.executeUpdate();
                        }
                    }

                    // Insert initial payment if any
                    if (billing.getPaidAmount().compareTo(BigDecimal.ZERO) > 0 && billing.getPaymentMethod() != null) {
                        String q3 = "INSERT INTO payments (invoice_id, amount, payment_method) VALUES (?, ?, ?)";
                        try (PreparedStatement s3 = conn.prepareStatement(q3)) {
                            s3.setInt(1, invoiceId);
                            s3.setBigDecimal(2, billing.getPaidAmount());
                            s3.setString(3, billing.getPaymentMethod().name());
                            s3.executeUpdate();
                        }
                    }
                }
                conn.commit();
                AuditDAO.log(null, "BILLING", "Created itemized invoice for patient ID: " + billing.getPatientId());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Service> getAvailableServices() throws SQLException {
        List<Service> services = new ArrayList<>();
        String query = "SELECT * FROM services WHERE status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Service s = new Service();
                s.setId(rs.getInt("id"));
                s.setServiceName(rs.getString("service_name"));
                s.setBasePrice(rs.getBigDecimal("base_price"));
                s.setCategory(rs.getString("category"));
                services.add(s);
            }
        }
        return services;
    }

    public List<Billing> getAllInvoices() throws SQLException {
        List<Billing> invoices = new ArrayList<>();
        String query = "SELECT b.id, b.patient_id, b.consultation_id, b.total_amount, b.paid_amount, b.status, b.generated_at as billing_date, "
                +
                "p.full_name as patient_name, " +
                "(SELECT payment_method FROM payments WHERE invoice_id = b.id ORDER BY payment_date DESC LIMIT 1) as payment_method "
                +
                "FROM invoices b JOIN patients p ON b.patient_id = p.id ORDER BY b.generated_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Billing b = new Billing();
                b.setId(rs.getInt("id"));
                b.setPatientId(rs.getInt("patient_id"));
                b.setAppointmentId(rs.getObject("consultation_id", Integer.class));
                b.setTotalAmount(rs.getBigDecimal("total_amount"));
                b.setPaidAmount(rs.getBigDecimal("paid_amount"));
                b.setPaymentStatus(Billing.PaymentStatus.valueOf(rs.getString("status")));
                String method = rs.getString("payment_method");
                if (method != null)
                    b.setPaymentMethod(Billing.PaymentMethod.valueOf(method));
                Timestamp bd = rs.getTimestamp("billing_date");
                if (bd != null)
                    b.setBillingDate(bd.toLocalDateTime());
                b.setPatientName(rs.getString("patient_name"));
                invoices.add(b);
            }
        }
        return invoices;
    }

    public void recordPayment(int invoiceId, BigDecimal amount, Billing.PaymentMethod method) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String selectQuery = "SELECT total_amount, paid_amount FROM invoices WHERE id = ?";
                try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                    selectStmt.setInt(1, invoiceId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            BigDecimal total = rs.getBigDecimal("total_amount");
                            BigDecimal currentPaid = rs.getBigDecimal("paid_amount");
                            BigDecimal newPaid = currentPaid.add(amount);

                            String status = "PARTIAL";
                            if (newPaid.compareTo(total) >= 0) {
                                status = "PAID";
                            }

                            String updateQuery = "UPDATE invoices SET paid_amount = ?, status = ? WHERE id = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                                updateStmt.setBigDecimal(1, newPaid);
                                updateStmt.setString(2, status);
                                updateStmt.setInt(3, invoiceId);
                                updateStmt.executeUpdate();
                            }

                            String insertPayment = "INSERT INTO payments (invoice_id, amount, payment_method) VALUES (?, ?, ?)";
                            try (PreparedStatement insStmt = conn.prepareStatement(insertPayment)) {
                                insStmt.setInt(1, invoiceId);
                                insStmt.setBigDecimal(2, amount);
                                insStmt.setString(3, method.name());
                                insStmt.executeUpdate();
                            }
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

    public List<Billing> getInvoicesByPatient(int patientId) throws SQLException {
        List<Billing> invoices = new ArrayList<>();
        String query = "SELECT b.*, (SELECT payment_method FROM payments WHERE invoice_id = b.id ORDER BY payment_date DESC LIMIT 1) as payment_method "
                +
                "FROM invoices b WHERE b.patient_id = ? ORDER BY b.generated_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Billing b = new Billing();
                    b.setId(rs.getInt("id"));
                    b.setPatientId(rs.getInt("patient_id"));
                    b.setAppointmentId(rs.getObject("consultation_id", Integer.class));
                    b.setTotalAmount(rs.getBigDecimal("total_amount"));
                    b.setPaidAmount(rs.getBigDecimal("paid_amount"));
                    b.setPaymentStatus(Billing.PaymentStatus.valueOf(rs.getString("status")));
                    String method = rs.getString("payment_method");
                    if (method != null)
                        b.setPaymentMethod(Billing.PaymentMethod.valueOf(method));
                    Timestamp bd = rs.getTimestamp("generated_at");
                    if (bd != null)
                        b.setBillingDate(bd.toLocalDateTime());
                    invoices.add(b);
                }
            }
        }
        return invoices;
    }

    public List<InvoiceItem> getInvoiceItems(int invoiceId) throws SQLException {
        List<InvoiceItem> items = new ArrayList<>();
        String query = "SELECT * FROM invoice_items WHERE invoice_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, invoiceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setId(rs.getInt("id"));
                    item.setInvoiceId(rs.getInt("invoice_id"));
                    item.setItemType(InvoiceItem.ItemType.valueOf(rs.getString("item_type")));
                    item.setItemReferenceId(rs.getInt("item_reference_id"));
                    item.setItemName(rs.getString("item_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    public List<Payment> getPaymentHistory(int invoiceId) throws SQLException {
        List<Payment> payments = new ArrayList<>();
        String query = "SELECT * FROM payments WHERE invoice_id = ? ORDER BY payment_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, invoiceId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getInt("id"));
                    p.setInvoiceId(rs.getInt("invoice_id"));
                    p.setAmount(rs.getBigDecimal("amount"));
                    p.setPaymentMethod(Billing.PaymentMethod.valueOf(rs.getString("payment_method")));
                    p.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
                    payments.add(p);
                }
            }
        }
        return payments;
    }
}

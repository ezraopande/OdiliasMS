-- Odilias Dispensary Management System (ODMS) Database Initialization
-- Database: hospital

DROP DATABASE IF EXISTS hospital;
CREATE DATABASE hospital;
USE hospital;

-- 1. Roles table
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT
);

-- Initialize default roles
INSERT IGNORE INTO roles (role_name) VALUES 
('ADMIN'), ('RECEPTIONIST'), ('NURSE'), ('CLINICAL_OFFICER'), ('PHARMACIST'), ('LAB_STAFF'), ('MANAGEMENT');

-- 2. Users / Staff table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    email VARCHAR(100) UNIQUE,
    contact_number VARCHAR(20),
    department VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    failed_attempts INT DEFAULT 0,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 3. Patients table
CREATE TABLE IF NOT EXISTS patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_uuid VARCHAR(20) UNIQUE NOT NULL, 
    full_name VARCHAR(100) NOT NULL,
    dob DATE,
    age INT,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    contact_number VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    national_id VARCHAR(50) UNIQUE,
    emergency_contact VARCHAR(100),
    blood_group ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-', 'UNKNOWN') DEFAULT 'UNKNOWN',
    allergies TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    staff_id INT, 
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    reason TEXT,
    status ENUM('SCHEDULED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW') DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (staff_id) REFERENCES users(id)
);

-- 5. Consultations table (Core Medical Record linking)
CREATE TABLE IF NOT EXISTS consultations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    clinician_id INT NOT NULL,
    appointment_id INT,
    symptoms TEXT,
    clinical_notes TEXT,
    status ENUM('OPEN', 'CLOSED') DEFAULT 'OPEN',
    consultation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (clinician_id) REFERENCES users(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

-- 6. Vitals table
CREATE TABLE IF NOT EXISTS vitals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    consultation_id INT NOT NULL,
    blood_pressure VARCHAR(20),
    temperature DECIMAL(4,2),
    pulse_rate INT,
    weight DECIMAL(5,2),
    height DECIMAL(5,2),
    oxygen_level INT,
    recorded_by INT,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (consultation_id) REFERENCES consultations(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id)
);

-- 7. Diagnosis table
CREATE TABLE IF NOT EXISTS diagnosis (
    id INT AUTO_INCREMENT PRIMARY KEY,
    consultation_id INT NOT NULL,
    disease_name VARCHAR(255) NOT NULL,
    description TEXT,
    diagnosed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (consultation_id) REFERENCES consultations(id)
);

-- 8. Medications (Inventory)
CREATE TABLE IF NOT EXISTS medications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    drug_name VARCHAR(255) NOT NULL,
    batch_number VARCHAR(50),
    quantity INT DEFAULT 0,
    expiry_date DATE,
    unit_price DECIMAL(10,2) NOT NULL
);

-- 9. Prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    consultation_id INT NOT NULL,
    medication_id INT,
    custom_drug_name VARCHAR(255),
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(50),
    status ENUM('PENDING', 'DISPENSED') DEFAULT 'PENDING',
    prescribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    dispensed_by INT,
    FOREIGN KEY (consultation_id) REFERENCES consultations(id),
    FOREIGN KEY (medication_id) REFERENCES medications(id),
    FOREIGN KEY (dispensed_by) REFERENCES users(id)
);

-- 12. Lab Tests (Catalog)
CREATE TABLE IF NOT EXISTS lab_tests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    test_name VARCHAR(255) NOT NULL,
    description TEXT,
    cost DECIMAL(10,2) NOT NULL
);

-- 13. Lab Results (Requests & Results)
CREATE TABLE IF NOT EXISTS lab_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    consultation_id INT NOT NULL,
    lab_test_id INT NOT NULL,
    requested_by INT NOT NULL,
    status ENUM('REQUESTED', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'REQUESTED',
    result_data TEXT,
    reference_range VARCHAR(255),
    comments TEXT,
    conducted_by INT,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (consultation_id) REFERENCES consultations(id),
    FOREIGN KEY (lab_test_id) REFERENCES lab_tests(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    FOREIGN KEY (conducted_by) REFERENCES users(id)
);

-- 14. Services table (Predefined prices for billable items)
CREATE TABLE IF NOT EXISTS services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    category ENUM('CONSULTATION', 'LABORATORY', 'PROCEDURE', 'NURSING', 'OTHER') NOT NULL,
    base_price DECIMAL(10,2) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE'
);

-- Seed initial services
INSERT IGNORE INTO services (service_name, description, category, base_price) VALUES 
('General Consultation', 'Clinical officer review', 'CONSULTATION', 500.00),
('Specialist Consultation', 'Specialist doctor review', 'CONSULTATION', 2000.00),
('Nursing Care', 'Basic nursing services', 'NURSING', 200.00),
('Nebulization', 'Inhaled medication administration', 'PROCEDURE', 1000.00),
('Dressing', 'Wound cleaning and dressing', 'PROCEDURE', 500.00);

-- 15. Invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    consultation_id INT,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('PENDING', 'PARTIAL', 'PAID', 'CANCELLED') DEFAULT 'PENDING',
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    generated_by INT,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (consultation_id) REFERENCES consultations(id),
    FOREIGN KEY (generated_by) REFERENCES users(id)
);

-- 16. Invoice Items table (Itemized billing)
CREATE TABLE IF NOT EXISTS invoice_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT NOT NULL,
    item_type ENUM('SERVICE', 'MEDICATION', 'LAB_TEST') NOT NULL,
    item_reference_id INT NOT NULL, -- ID from services, medications, or lab_tests
    item_name VARCHAR(255) NOT NULL,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

-- 17. Payments table
CREATE TABLE IF NOT EXISTS payments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method ENUM('CASH', 'MOBILE_MONEY', 'INSURANCE', 'CARD') NOT NULL,
    transaction_reference VARCHAR(100),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_by INT,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    FOREIGN KEY (processed_by) REFERENCES users(id)
);

-- 18. Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action TEXT NOT NULL,
    module VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Seed initial admin user using Role ID 1 (ADMIN)
-- password is 'admin123'
INSERT IGNORE INTO users (username, password_hash, full_name, role_id) 
VALUES ('admin', '$2a$12$R9h/LIPzKuqbK.9D3k1zGOjP7iS6S6S6S6S6S6S6S6S6S6S', 'System Administrator', 1);

-- Seed Lab Tests
INSERT IGNORE INTO lab_tests (test_name, description, cost) VALUES 
('Full Blood Count', 'Complete blood profile', 1500.00),
('Malaria Test', 'Rapid diagnostic test/Microscopy', 500.00),
('Urinalysis', 'Routine urine examination', 300.00),
('HIV Test', 'Voluntary counseling and testing', 0.00);

-- Seed Medications
INSERT IGNORE INTO medications (drug_name, batch_number, quantity, expiry_date, unit_price) VALUES 
('Paracetamol 500mg', 'BTC101', 500, '2027-12-01', 5.00),
('Amoxicillin 250mg', 'BTC102', 200, '2026-06-01', 15.00),
('Artemether/Lumefantrine', 'BTC103', 100, '2026-01-01', 250.00);

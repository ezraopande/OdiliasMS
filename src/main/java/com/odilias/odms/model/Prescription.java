package com.odilias.odms.model;

import java.time.LocalDateTime;

public class Prescription {
    public enum Status {
        PENDING, DISPENSED
    }

    private int id;
    private int consultationId;
    private Integer medicationId;
    private String customDrugName;
    private String dosage;
    private String frequency;
    private String duration;
    private Status status;
    private LocalDateTime prescribedAt;
    private Integer dispensedBy;

    // Display helpers
    private String drugName;
    private String patientName;
    private double unitPrice;
    private String prescriberName;
    private LocalDateTime dispensedAt;
    private String dispenserName;

    public Prescription() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(int consultationId) {
        this.consultationId = consultationId;
    }

    public Integer getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Integer medicationId) {
        this.medicationId = medicationId;
    }

    public String getCustomDrugName() {
        return customDrugName;
    }

    public void setCustomDrugName(String customDrugName) {
        this.customDrugName = customDrugName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getPrescribedAt() {
        return prescribedAt;
    }

    public void setPrescribedAt(LocalDateTime prescribedAt) {
        this.prescribedAt = prescribedAt;
    }

    public Integer getDispensedBy() {
        return dispensedBy;
    }

    public void setDispensedBy(Integer dispensedBy) {
        this.dispensedBy = dispensedBy;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public void setPrescriberName(String prescriberName) {
        this.prescriberName = prescriberName;
    }

    public LocalDateTime getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(LocalDateTime dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public String getDispenserName() {
        return dispenserName;
    }

    public void setDispenserName(String dispenserName) {
        this.dispenserName = dispenserName;
    }
}

package com.odilias.odms.model;

import java.time.LocalDateTime;

public class MedicalRecord {
    private int id;
    private int patientId;
    private int clinicianId;
    private Integer appointmentId;
    private String diagnosis;
    private String treatment;
    private String prescriptions;
    private String labResults;
    private String bloodPressure;
    private Double temperature;
    private Integer pulseRate;
    private Double weight;
    private LocalDateTime recordDate;
    private Integer requestedLabTestId;
    private Integer requestedMedicationId;
    private String rxDosage;
    private String rxFreq;
    private String rxDuration;

    // Display helpers
    private String patientName;
    private String clinicianName;

    public MedicalRecord() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getClinicianId() {
        return clinicianId;
    }

    public void setClinicianId(int clinicianId) {
        this.clinicianId = clinicianId;
    }

    public Integer getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Integer appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(String prescriptions) {
        this.prescriptions = prescriptions;
    }

    public String getLabResults() {
        return labResults;
    }

    public void setLabResults(String labResults) {
        this.labResults = labResults;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDateTime recordDate) {
        this.recordDate = recordDate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getClinicianName() {
        return clinicianName;
    }

    public void setClinicianName(String clinicianName) {
        this.clinicianName = clinicianName;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getPulseRate() {
        return pulseRate;
    }

    public void setPulseRate(Integer pulseRate) {
        this.pulseRate = pulseRate;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getRequestedLabTestId() {
        return requestedLabTestId;
    }

    public void setRequestedLabTestId(Integer requestedLabTestId) {
        this.requestedLabTestId = requestedLabTestId;
    }

    public Integer getRequestedMedicationId() {
        return requestedMedicationId;
    }

    public void setRequestedMedicationId(Integer requestedMedicationId) {
        this.requestedMedicationId = requestedMedicationId;
    }

    public String getRxDosage() {
        return rxDosage;
    }

    public void setRxDosage(String rxDosage) {
        this.rxDosage = rxDosage;
    }

    public String getRxFreq() {
        return rxFreq;
    }

    public void setRxFreq(String rxFreq) {
        this.rxFreq = rxFreq;
    }

    public String getRxDuration() {
        return rxDuration;
    }

    public void setRxDuration(String rxDuration) {
        this.rxDuration = rxDuration;
    }
}

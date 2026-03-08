package com.odilias.odms.model;

import java.time.LocalDateTime;

public class LabResult {
    public enum Status {
        REQUESTED, IN_PROGRESS, COMPLETED
    }

    private int id;
    private int consultationId;
    private int labTestId;
    private int requestedBy;
    private Status status;
    private String resultData;
    private String referenceRange;
    private String comments;
    private Integer conductedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private int patientId;

    // Display helpers (for table UI)
    private String testName;
    private String patientName;
    private String clinicianName;

    public LabResult() {
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
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

    public int getLabTestId() {
        return labTestId;
    }

    public void setLabTestId(int labTestId) {
        this.labTestId = labTestId;
    }

    public int getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(int requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public void setReferenceRange(String referenceRange) {
        this.referenceRange = referenceRange;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Integer getConductedBy() {
        return conductedBy;
    }

    public void setConductedBy(Integer conductedBy) {
        this.conductedBy = conductedBy;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
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
}

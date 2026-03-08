package com.odilias.odms.model;

import java.util.List;
import java.util.ArrayList;

public class PatientProfile {
    private Patient patient;
    private List<MedicalRecord> visits = new ArrayList<>();
    private List<Prescription> medications = new ArrayList<>();
    private List<LabResult> labHistory = new ArrayList<>();
    private List<Billing> billingHistory = new ArrayList<>();

    public PatientProfile() {
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<MedicalRecord> getVisits() {
        return visits;
    }

    public void setVisits(List<MedicalRecord> visits) {
        this.visits = visits;
    }

    public List<Prescription> getMedications() {
        return medications;
    }

    public void setMedications(List<Prescription> medications) {
        this.medications = medications;
    }

    public List<LabResult> getLabHistory() {
        return labHistory;
    }

    public void setLabHistory(List<LabResult> labHistory) {
        this.labHistory = labHistory;
    }

    public List<Billing> getBillingHistory() {
        return billingHistory;
    }

    public void setBillingHistory(List<Billing> billingHistory) {
        this.billingHistory = billingHistory;
    }
}

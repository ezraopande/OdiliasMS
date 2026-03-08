package com.odilias.odms.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Medication {
    private int id;
    private String drugName;
    private String batchNumber;
    private int quantity;
    private LocalDate expiryDate;
    private BigDecimal unitPrice;

    public Medication() {
    }

    public Medication(int id, String drugName, String batchNumber, int quantity, LocalDate expiryDate,
            BigDecimal unitPrice) {
        this.id = id;
        this.drugName = drugName;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
        this.unitPrice = unitPrice;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDrugName() {
        return drugName;
    }

    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Override
    public String toString() {
        return drugName + " (Qty: " + quantity + ")";
    }
}

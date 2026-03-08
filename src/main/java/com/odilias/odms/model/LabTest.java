package com.odilias.odms.model;

import java.math.BigDecimal;

public class LabTest {
    private int id;
    private String testName;
    private String description;
    private BigDecimal cost;

    public LabTest() {
    }

    public LabTest(int id, String testName, String description, BigDecimal cost) {
        this.id = id;
        this.testName = testName;
        this.description = description;
        this.cost = cost;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    @Override
    public String toString() {
        return testName + " (KES " + cost + ")";
    }
}

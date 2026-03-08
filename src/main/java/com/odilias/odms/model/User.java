package com.odilias.odms.model;

public class User {
    private int id;
    private String username;
    private String fullName;
    private Role role;
    private Status status;
    private String email;
    private String contactNumber;

    public enum Role {
        ADMIN, RECEPTIONIST, NURSE, CLINICAL_OFFICER, PHARMACIST, LAB_STAFF, MANAGEMENT
    }

    public enum Status {
        ACTIVE, INACTIVE, LOCKED
    }

    public User() {
    }

    public User(int id, String username, String fullName, Role role, Status status) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
}

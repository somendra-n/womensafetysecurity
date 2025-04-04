package com.example.womensafetyapp;

import java.util.Map;

public class User {

    // Basic User Information
    private String userId;             // Unique ID for the user (Firebase UID)
    private String username;           // Username of the user
    private String phoneNumber;        // Phone number of the user
    private String emergencyContact1;  // First emergency contact phone number
    private String emergencyContact2;  // Second emergency contact phone number

    // User Status and Safety Details
    private boolean isSaved;           // Whether the user's safety details have been saved
    private boolean hasConfirmedSafe;  // Whether the user has confirmed their safety

    // User Location Data (Latitude, Longitude)
    private double latitude;           // Latitude for user's current location
    private double longitude;          // Longitude for user's current location

    // Timestamp when the data was recorded (in ISO 8601 format)
    private String timestamp;

    // Default constructor for Firestore deserialization
    public User() {
    }

    // Constructor with all fields
    public User(String userId, String username, String phoneNumber, String emergencyContact1, String emergencyContact2, boolean isSaved, boolean hasConfirmedSafe, double latitude, double longitude, String timestamp) {
        this.userId = userId;
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.emergencyContact1 = emergencyContact1;
        this.emergencyContact2 = emergencyContact2;
        this.isSaved = isSaved;
        this.hasConfirmedSafe = hasConfirmedSafe;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmergencyContact1() {
        return emergencyContact1;
    }

    public void setEmergencyContact1(String emergencyContact1) {
        this.emergencyContact1 = emergencyContact1;
    }

    public String getEmergencyContact2() {
        return emergencyContact2;
    }

    public void setEmergencyContact2(String emergencyContact2) {
        this.emergencyContact2 = emergencyContact2;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean saved) {
        isSaved = saved;
    }

    public boolean isHasConfirmedSafe() {
        return hasConfirmedSafe;
    }

    public void setHasConfirmedSafe(boolean hasConfirmedSafe) {
        this.hasConfirmedSafe = hasConfirmedSafe;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

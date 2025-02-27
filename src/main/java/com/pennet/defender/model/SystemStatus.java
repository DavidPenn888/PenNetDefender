package com.pennet.defender.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class SystemStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sid;

    private LocalDateTime timestamp;

    private double cpuUsage;
    private double memoryUsage;
    private double storageUsage;

    // Getters and Setters
    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public double getStorageUsage() {
        return storageUsage;
    }

    public void setStorageUsage(double storageUsage) {
        this.storageUsage = storageUsage;
    }
}
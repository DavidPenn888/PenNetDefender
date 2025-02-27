package com.pennet.defender.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ssh_http_alert")
public class SecurityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Dnumber")
    private int id;

    @Column(name = "Dtimestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "Ddetect_way", nullable = false)
    private int detectWay; // 1 = SSH, 2 = HTTP

    @Column(name = "Dalert_type", nullable = false, length = 45)
    private String alertType;

    @Column(name = "Duser_info", length = 45)
    private String userInfo;

    @Column(name = "Dip_info", length = 45)
    private String ipInfo;

    @Column(name = "Ddetail_info", nullable = false, length = 255)
    private String detailInfo;

    // 无参构造函数
    public SecurityAlert() {
    }

    // 带参数的构造函数
    public SecurityAlert(LocalDateTime timestamp, int detectWay, String alertType,
                         String userInfo, String ipInfo, String detailInfo) {
        this.timestamp = timestamp;
        this.detectWay = detectWay;
        this.alertType = alertType;
        this.userInfo = userInfo;
        this.ipInfo = ipInfo;
        this.detailInfo = detailInfo;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getDetectWay() {
        return detectWay;
    }

    public void setDetectWay(int detectWay) {
        this.detectWay = detectWay;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public String getIpInfo() {
        return ipInfo;
    }

    public void setIpInfo(String ipInfo) {
        this.ipInfo = ipInfo;
    }

    public String getDetailInfo() {
        return detailInfo;
    }

    public void setDetailInfo(String detailInfo) {
        this.detailInfo = detailInfo;
    }
}
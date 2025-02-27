package com.pennet.defender.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ThresholdAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tnumber;

    private LocalDateTime timestamp;

    private String alertType;
    private String detailInfo;

    // 无参构造函数
    public ThresholdAlert() {}

    // 带参数的构造函数
    public ThresholdAlert(LocalDateTime timestamp, String alertType, String detailInfo) {
        this.timestamp = timestamp;
        this.alertType = alertType;
        this.detailInfo = detailInfo;
    }

    // Getters and Setters
    public int getTnumber() {
        return tnumber;
    }

    public void setTnumber(int tnumber) {
        this.tnumber = tnumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getDetailInfo() {
        return detailInfo;
    }

    public void setDetailInfo(String detailInfo) {
        this.detailInfo = detailInfo;
    }
}

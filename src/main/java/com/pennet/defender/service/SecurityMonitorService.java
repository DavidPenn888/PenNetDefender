package com.pennet.defender.service;

import com.pennet.defender.model.SecurityAlert;
import org.springframework.data.domain.Page;

public interface SecurityMonitorService {
    void startSshMonitoring();
    void stopSshMonitoring();
    void startHttpMonitoring();
    void stopHttpMonitoring();
    boolean isSshMonitoringRunning();
    boolean isHttpMonitoringRunning();
    void saveAlert(SecurityAlert alert);
    Page<SecurityAlert> getAlerts(int page, int size, Integer detectWay, String alertType);
}

package com.pennet.defender.service;

import com.pennet.defender.model.SecurityAlert;
import org.springframework.data.domain.Page;

import java.io.IOException;

public interface SecurityMonitorService {
    void startSshMonitoring();
    void stopSshMonitoring();
    void startHttpMonitoring();
    void stopHttpMonitoring() throws IOException;
    boolean isSshMonitoringRunning();
    boolean isHttpMonitoringRunning();
    void saveAlert(SecurityAlert alert);
    Page<SecurityAlert> getAlerts(int page, int size, Integer detectWay, String alertType);
}

package com.pennet.defender.controller;

import com.pennet.defender.config.SecurityMonitorConfig;
import com.pennet.defender.model.SecurityAlert;
import com.pennet.defender.service.SecurityMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/traffics")
public class SecurityMonitorController {

    @Autowired
    private SecurityMonitorService securityMonitorService;

    @Autowired
    private SecurityMonitorConfig securityMonitorConfig;

    @GetMapping("/ssh_http_alert")
    public Map<String, Object> getSecurityAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) Integer detectWay,
            @RequestParam(required = false) String alertType) {

        Page<SecurityAlert> alerts = securityMonitorService.getAlerts(page, size, detectWay, alertType);

        Map<String, Object> response = new HashMap<>();
        response.put("totalPages", alerts.getTotalPages());
        response.put("totalElements", alerts.getTotalElements());
        response.put("content", alerts.getContent());

        return response;
    }

    @GetMapping("/status")
    public Map<String, Boolean> getMonitoringStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("sshMonitoring", securityMonitorService.isSshMonitoringRunning());
        status.put("httpMonitoring", securityMonitorService.isHttpMonitoringRunning());
        return status;
    }

    @PostMapping("/ssh/enable")
    public ResponseEntity<Map<String, Object>> enableSshMonitoring() {
        securityMonitorService.startSshMonitoring();
        securityMonitorConfig.setSshMonitorEnabled(true);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "SSH监控已启用");
        response.put("status", securityMonitorService.isSshMonitoringRunning());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/ssh/disable")
    public ResponseEntity<Map<String, Object>> disableSshMonitoring() {
        securityMonitorService.stopSshMonitoring();
        securityMonitorConfig.setSshMonitorEnabled(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "SSH监控已禁用");
        response.put("status", securityMonitorService.isSshMonitoringRunning());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/http/enable")
    public ResponseEntity<Map<String, Object>> enableHttpMonitoring() {
        securityMonitorService.startHttpMonitoring();
        securityMonitorConfig.setHttpMonitorEnabled(true);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "HTTP监控已启用");
        response.put("status", securityMonitorService.isHttpMonitoringRunning());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/http/disable")
    public ResponseEntity<Map<String, Object>> disableHttpMonitoring() {
        securityMonitorService.stopHttpMonitoring();
        securityMonitorConfig.setHttpMonitorEnabled(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "HTTP监控已禁用");
        response.put("status", securityMonitorService.isHttpMonitoringRunning());

        return ResponseEntity.ok(response);
    }
}

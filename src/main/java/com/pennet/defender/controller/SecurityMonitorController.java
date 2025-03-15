package com.pennet.defender.controller;

import com.pennet.defender.config.SecurityMonitorConfig;
import com.pennet.defender.model.ApiResponse;
import com.pennet.defender.model.SecurityAlert;
import com.pennet.defender.service.SecurityMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/traffics")
public class SecurityMonitorController {

    @Autowired
    private SecurityMonitorService securityMonitorService;

    @Autowired
    private SecurityMonitorConfig securityMonitorConfig;

    @GetMapping("/alert")
    public ApiResponse<Map<String, Object>> getSecurityAlerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) Integer detectWay,
            @RequestParam(required = false) String alertType) {

        Page<SecurityAlert> alerts = securityMonitorService.getAlerts(page, size, detectWay, alertType);

        Map<String, Object> data = new HashMap<>();
        data.put("totalPages", alerts.getTotalPages());
        data.put("totalElements", alerts.getTotalElements());
        data.put("content", alerts.getContent());

        return new ApiResponse<>(0, data, "success");
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Boolean>> getMonitoringStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("sshMonitoring", securityMonitorService.isSshMonitoringRunning());
        status.put("httpMonitoring", securityMonitorService.isHttpMonitoringRunning());

        return new ApiResponse<>(0, status, "success");
    }

    @PostMapping("/ssh/enable")
    public ApiResponse<Map<String, Object>> enableSshMonitoring() {
        securityMonitorService.startSshMonitoring();
        securityMonitorConfig.setSshMonitorEnabled(true);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "SSH监控已启用");
        response.put("status", securityMonitorService.isSshMonitoringRunning());

        return new ApiResponse<>(0, response, "success");
    }

    @PostMapping("/ssh/disable")
    public ApiResponse<Map<String, Object>> disableSshMonitoring() {
        securityMonitorService.stopSshMonitoring();
        securityMonitorConfig.setSshMonitorEnabled(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "SSH监控已禁用");
        response.put("status", securityMonitorService.isSshMonitoringRunning());

        return new ApiResponse<>(0, response, "success");
    }

    @PostMapping("/http/enable")
    public ApiResponse<Map<String, Object>> enableHttpMonitoring() {
        securityMonitorService.startHttpMonitoring();
        securityMonitorConfig.setHttpMonitorEnabled(true);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "HTTP监控已启用");
        response.put("status", securityMonitorService.isHttpMonitoringRunning());

        return new ApiResponse<>(0, response, "success");
    }

    @PostMapping("/http/disable")
    public ApiResponse<Map<String, Object>> disableHttpMonitoring() throws IOException {
        securityMonitorService.stopHttpMonitoring();
        securityMonitorConfig.setHttpMonitorEnabled(false);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "HTTP监控已禁用");
        response.put("status", securityMonitorService.isHttpMonitoringRunning());

        return new ApiResponse<>(0, response, "success");
    }
}

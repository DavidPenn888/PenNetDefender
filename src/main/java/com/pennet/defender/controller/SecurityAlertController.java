package com.pennet.defender.controller;

import com.pennet.defender.model.SecurityAlert;
import com.pennet.defender.service.SecurityMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
public class SecurityAlertController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertController.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private SecurityMonitorService securityMonitorService;

    /**
     * 接收来自mitmproxy脚本的HTTP告警
     * 预期的JSON格式：
     * {
     *   "timestamp": "2023-01-01T12:00:00",
     *   "detectWay": 2,
     *   "alertType": "SQL注入",
     *   "userInfo": "user1",
     *   "ipInfo": "192.168.1.1",
     *   "detailInfo": "检测到HTTP安全事件: 可能的SQL注入尝试 - GET http://example.com/..."
     * }
     */
    @PostMapping("/http_alert")
    public ResponseEntity<Map<String, Object>> receiveHttpAlert(@RequestBody Map<String, Object> alertData) {
        try {
            logger.info("收到HTTP告警: {}", alertData);
            
            // 解析时间戳
            LocalDateTime timestamp;
            if (alertData.containsKey("timestamp") && alertData.get("timestamp") != null) {
                String timestampStr = (String) alertData.get("timestamp");
                timestamp = LocalDateTime.parse(timestampStr, formatter);
            } else {
                timestamp = LocalDateTime.now();
            }
            
            // 创建SecurityAlert对象
            SecurityAlert alert = new SecurityAlert(
                timestamp,
                alertData.containsKey("detectWay") ? ((Number) alertData.get("detectWay")).intValue() : 2,
                (String) alertData.getOrDefault("alertType", "未知告警"),
                (String) alertData.get("userInfo"),
                (String) alertData.get("ipInfo"),
                (String) alertData.getOrDefault("detailInfo", "未提供详细信息")
            );
            
            // 保存告警
            securityMonitorService.saveAlert(alert);
            
            return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "告警已成功接收并处理",
                "data", Map.of("alertId", alert.getId())
            ));
        } catch (Exception e) {
            logger.error("处理HTTP告警失败", e);
            return ResponseEntity.ok(Map.of(
                "code", -1,
                "message", "处理告警失败: " + e.getMessage(),
                "data", Map.of()
            ));
        }
    }
}
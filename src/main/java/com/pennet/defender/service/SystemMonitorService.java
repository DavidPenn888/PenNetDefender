package com.pennet.defender.service;

import com.pennet.defender.config.WebHookConfig;
import com.pennet.defender.model.SystemStatus;
import com.pennet.defender.model.ThresholdAlert;
import com.pennet.defender.repository.SystemStatusRepository;
import com.pennet.defender.repository.ThresholdAlertRepository;
import com.pennet.defender.config.ThresholdConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemMonitorService {

    @Autowired
    private SystemStatusRepository systemStatusRepository;

    @Autowired
    private ThresholdAlertRepository thresholdAlertRepository;

    @Autowired
    private ThresholdConfig thresholdConfig;

    @Autowired
    private WebHookConfig webHookConfig;

    @Autowired
    private RestTemplate restTemplate;

    public void saveSystemStatus(double cpuUsage, double memoryUsage, double storageUsage) {
        SystemStatus status = new SystemStatus();
        status.setTimestamp(LocalDateTime.now());
        status.setCpuUsage(cpuUsage);
        status.setMemoryUsage(memoryUsage);
        status.setStorageUsage(storageUsage);

        if (cpuUsage > thresholdConfig.getCpuThreshold()) {
            sendAlert("CPU", "CPU 使用率过高: " + cpuUsage + "%");
            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "CPU", "CPU usage: " + cpuUsage + "%"));
        }
        if (memoryUsage > thresholdConfig.getMemoryThreshold()) {
            sendAlert("Memory", "内存使用率过高: " + memoryUsage + "%");
            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "Memory", "Memory usage: " + memoryUsage + "%"));
        }
        if (storageUsage > thresholdConfig.getStorageThreshold()) {
            sendAlert("Storage", "存储使用率过高: " + storageUsage + "%");
            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "Storage", "Storage usage: " + storageUsage + "%"));
        }

        systemStatusRepository.save(status);
    }

//    public List<SystemStatus> getStatusLast24Hours() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime dayAgo = now.minusHours(24);
//        return systemStatusRepository.findHighestUsagePerHourLast24Hours(dayAgo, now);
//    }
    public List<SystemStatus> getRecentStatus() {
        return systemStatusRepository.findTop20ByOrderByTimestampDesc();
    }

    public Page<ThresholdAlert> getThresholdAlerts(String alertType, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);  // Page index is 0-based
        if (alertType != null && !alertType.isEmpty()) {
            return thresholdAlertRepository.findByAlertTypeOrderByTimestampDesc(alertType, pageable);
        }
        return thresholdAlertRepository.findAllByOrderByTimestampDesc(pageable);
    }

    private void sendAlert(String alertType, String message) {
        String formattedMessage = "**系统告警**\n\n" +
                "**告警类型:** " + alertType + "\n" +
                "**详情:** " + message + "\n" +
                "**时间:** " + LocalDateTime.now();

        if (webHookConfig.isWechatEnable()) {
            sendWechatAlert(formattedMessage);
        }
        if (webHookConfig.isDingdingEnable()) {
            sendDingdingAlert(formattedMessage);
        }
    }

    private void sendWechatAlert(String message) {
        Map<String, Object> request = new HashMap<>();
        request.put("msgtype", "text");
        Map<String, String> text = new HashMap<>();
        text.put("content", message);
        request.put("text", text);
        restTemplate.postForEntity(webHookConfig.getWechatWebHook(), request, String.class);
    }

    private void sendDingdingAlert(String message) {
        Map<String, Object> request = new HashMap<>();
        request.put("msgtype", "text");
        Map<String, String> text = new HashMap<>();
        text.put("content", message);
        request.put("text", text);
        restTemplate.postForEntity(webHookConfig.getDingdingWebhook(), request, String.class);
    }

}

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

//    public void saveSystemStatus(double cpuUsage, double memoryUsage, double storageUsage) {
//        SystemStatus status = new SystemStatus();
//        status.setTimestamp(LocalDateTime.now());
//        status.setCpuUsage(cpuUsage);
//        status.setMemoryUsage(memoryUsage);
//        status.setStorageUsage(storageUsage);
//
//        if (cpuUsage > thresholdConfig.getCpuThreshold()) {
//            String formattedCpuUsage = String.format("%.2f", cpuUsage);
//            sendAlert("CPU", "CPU 使用率过高: " + formattedCpuUsage + "%，超过阈值" + String.format("", thresholdConfig.getCpuThreshold()) + "%");
//            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "CPU", "CPU 使用率为：" + formattedCpuUsage + "%，超过阈值" + String.format("", thresholdConfig.getCpuThreshold()) + "%"));
//        }
//        if (memoryUsage > thresholdConfig.getMemoryThreshold()) {
//            String formattedMemoryUsage = String.format("%.2f", memoryUsage);
//            sendAlert("内存", "内存使用率过高: " + formattedMemoryUsage + "%，超过阈值" + String.format("", thresholdConfig.getMemoryThreshold()) + "%");
//            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "内存", "内存使用率为：" + formattedMemoryUsage + "%，超过阈值" + String.format("", thresholdConfig.getMemoryThreshold()) + "%"));
//        }
//        if (storageUsage > thresholdConfig.getStorageThreshold()) {
//            String formattedStorageUsage = String.format("%.2f", storageUsage);
//            sendAlert("存储", "存储使用率过高: " + formattedStorageUsage + "%，超过阈值" + String.format("", thresholdConfig.getStorageThreshold()) + "%");
//            thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "存储", "存储使用率为：" + formattedStorageUsage + "%，超过阈值" + String.format("", thresholdConfig.getStorageThreshold()) + "%"));
//        }
//
//        systemStatusRepository.save(status);
//    }
public void saveSystemStatus(double cpuUsage, double memoryUsage, double storageUsage) {
    // 预处理：统一格式化为两位小数
    double formattedCpuUsage = Double.parseDouble(String.format("%.2f", cpuUsage));
    double formattedMemoryUsage = Double.parseDouble(String.format("%.2f", memoryUsage));
    double formattedStorageUsage = Double.parseDouble(String.format("%.2f", storageUsage));

    // 创建并保存状态对象
    SystemStatus status = new SystemStatus();
    status.setTimestamp(LocalDateTime.now());
    status.setCpuUsage(formattedCpuUsage);
    status.setMemoryUsage(formattedMemoryUsage);
    status.setStorageUsage(formattedStorageUsage);

    // 检查阈值并发送警报
    if (formattedCpuUsage > thresholdConfig.getCpuThreshold()) {
        sendAlert("CPU", "CPU 使用率过高: " + formattedCpuUsage + "%，超过阈值" + thresholdConfig.getCpuThreshold() + "%");
        thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "CPU", "CPU 使用率为：" + formattedCpuUsage + "%，超过阈值" + thresholdConfig.getCpuThreshold() + "%"));
    }
    if (formattedMemoryUsage > thresholdConfig.getMemoryThreshold()) {
        sendAlert("内存", "内存使用率过高: " + formattedMemoryUsage + "%，超过阈值" + thresholdConfig.getMemoryThreshold() + "%");
        thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "内存", "内存使用率为：" + formattedMemoryUsage + "%，超过阈值" + thresholdConfig.getMemoryThreshold() + "%"));
    }
    if (formattedStorageUsage > thresholdConfig.getStorageThreshold()) {
        sendAlert("存储", "存储使用率过高: " + formattedStorageUsage + "%，超过阈值" + thresholdConfig.getStorageThreshold() + "%");
        thresholdAlertRepository.save(new ThresholdAlert(LocalDateTime.now(), "存储", "存储使用率为：" + formattedStorageUsage + "%，超过阈值" + thresholdConfig.getStorageThreshold() + "%"));
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

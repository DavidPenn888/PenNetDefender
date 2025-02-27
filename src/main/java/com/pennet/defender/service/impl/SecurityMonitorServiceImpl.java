package com.pennet.defender.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pennet.defender.config.SecurityMonitorConfig;
import com.pennet.defender.model.SecurityAlert;
import com.pennet.defender.model.SecurityRule;
import com.pennet.defender.repository.SecurityAlertRepository;
import com.pennet.defender.service.SecurityMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SecurityMonitorServiceImpl implements SecurityMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityMonitorServiceImpl.class);
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern USER_PATTERN = Pattern.compile("user=(\\w+)");

    @Autowired
    private SecurityMonitorConfig config;

    @Autowired
    private SecurityAlertRepository alertRepository;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final AtomicBoolean sshMonitorRunning = new AtomicBoolean(false);
    private final AtomicBoolean httpMonitorRunning = new AtomicBoolean(false);

    private List<SecurityRule> sshRules = new ArrayList<>();
    private List<SecurityRule> httpRules = new ArrayList<>();

    private Process mitmProxyProcess;
    private Thread sshMonitorThread;
    private Thread httpMonitorThread;

    @PostConstruct
    public void init() {
        try {
            // 加载SSH规则
            loadRules(config.getSshRulePath(), sshRules);
            // 加载HTTP规则
            loadRules(config.getHttpRulePath(), httpRules);

            // 根据配置启动监控
            if (config.isSshMonitorEnabled()) {
                startSshMonitoring();
            }

            if (config.isHttpMonitorEnabled()) {
                startHttpMonitoring();
            }
        } catch (Exception e) {
            logger.error("初始化安全监控服务失败", e);
        }
    }

    private void loadRules(String path, List<SecurityRule> rulesList) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            List<SecurityRule> rules = mapper.readValue(inputStream, new TypeReference<List<SecurityRule>>() {});
            rulesList.addAll(rules);
            logger.info("已加载 {} 条规则，从 {}", rules.size(), path);
        } catch (IOException e) {
            logger.error("加载规则文件失败: " + path, e);
            throw e;
        }
    }

    @Override
    public void startSshMonitoring() {
        if (sshMonitorRunning.compareAndSet(false, true)) {
            sshMonitorThread = new Thread(this::monitorSshLogs);
            sshMonitorThread.setDaemon(true);
            sshMonitorThread.start();
            logger.info("SSH监控已启动");
        }
    }

    @Override
    public void stopSshMonitoring() {
        if (sshMonitorRunning.compareAndSet(true, false)) {
            if (sshMonitorThread != null) {
                sshMonitorThread.interrupt();
                sshMonitorThread = null;
            }
            logger.info("SSH监控已停止");
        }
    }

    @Override
    public void startHttpMonitoring() {
        if (httpMonitorRunning.compareAndSet(false, true)) {
            try {
                // 启动mitmproxy代理
                startMitmProxy();

                // 启动HTTP监控线程
                httpMonitorThread = new Thread(this::monitorHttpTraffic);
                httpMonitorThread.setDaemon(true);
                httpMonitorThread.start();
                logger.info("HTTP监控已启动");
            } catch (IOException e) {
                httpMonitorRunning.set(false);
                logger.error("启动HTTP监控失败", e);
            }
        }
    }

    @Override
    public void stopHttpMonitoring() {
        if (httpMonitorRunning.compareAndSet(true, false)) {
            if (httpMonitorThread != null) {
                httpMonitorThread.interrupt();
                httpMonitorThread = null;
            }

            // 停止mitmproxy代理
            stopMitmProxy();
            logger.info("HTTP监控已停止");
        }
    }

    @Override
    public boolean isSshMonitoringRunning() {
        return sshMonitorRunning.get();
    }

    @Override
    public boolean isHttpMonitoringRunning() {
        return httpMonitorRunning.get();
    }

    @Override
    public void saveAlert(SecurityAlert alert) {
        alertRepository.save(alert);
    }

    @Override
    public Page<SecurityAlert> getAlerts(int page, int size, Integer detectWay, String alertType) {
        Pageable pageable = PageRequest.of(page - 1, size);

        if (detectWay != null && alertType != null) {
            return alertRepository.findByDetectWayAndAlertTypeOrderByTimestampDesc(detectWay, alertType, pageable);
        } else if (detectWay != null) {
            return alertRepository.findByDetectWayOrderByTimestampDesc(detectWay, pageable);
        } else if (alertType != null) {
            return alertRepository.findByAlertTypeOrderByTimestampDesc(alertType, pageable);
        } else {
            return alertRepository.findAllByOrderByTimestampDesc(pageable);
        }
    }

    @Async
    protected void monitorSshLogs() {
        try {
            Process process = Runtime.getRuntime().exec("tail -f " + config.getAuditdLogPath());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (sshMonitorRunning.get() && (line = reader.readLine()) != null) {
                    processAuditLogLine(line);
                }
            }
        } catch (IOException e) {
            logger.error("监控SSH日志失败", e);
        }
    }

    private void processAuditLogLine(String logLine) {
        for (SecurityRule rule : sshRules) {
            if (rule.matches(logLine)) {
                // 提取用户和IP信息
                String userInfo = extractUserInfo(logLine);
                String ipInfo = extractIpInfo(logLine);

                SecurityAlert alert = new SecurityAlert(
                        LocalDateTime.now(),
                        1, // SSH检测
                        rule.getAlertType(),
                        userInfo,
                        ipInfo,
                        "检测到安全事件: " + rule.getDescription() + " - " + logLine
                );

                saveAlert(alert);
                logger.info("发现SSH安全告警: {}", rule.getAlertType());
                break; // 一行日志只触发一条规则
            }
        }
    }

    // TODO /tmp/mitmproxy_script.py 没有配置
    private void startMitmProxy() throws IOException {
        // 启动mitmproxy代理服务器
        mitmProxyProcess = Runtime.getRuntime().exec("mitmdump -s /tmp/mitmproxy_script.py --set flow_detail=3");
        logger.info("已启动mitmproxy代理");
    }

    private void stopMitmProxy() {
        if (mitmProxyProcess != null) {
            mitmProxyProcess.destroy();
            mitmProxyProcess = null;
            logger.info("已停止mitmproxy代理");
        }
    }

    @Async
    protected void monitorHttpTraffic() {
        try {
            // 监控mitmproxy的输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(mitmProxyProcess.getInputStream()))) {
                String line;
                while (httpMonitorRunning.get() && (line = reader.readLine()) != null) {
                    processHttpTrafficLine(line);
                }
            }
        } catch (IOException e) {
            logger.error("监控HTTP流量失败", e);
        }
    }

    private void processHttpTrafficLine(String trafficLine) {
        for (SecurityRule rule : httpRules) {
            if (rule.matches(trafficLine)) {
                // 提取用户和IP信息
                String userInfo = extractUserInfo(trafficLine);
                String ipInfo = extractIpInfo(trafficLine);

                SecurityAlert alert = new SecurityAlert(
                        LocalDateTime.now(),
                        2, // HTTP检测
                        rule.getAlertType(),
                        userInfo,
                        ipInfo,
                        "检测到HTTP安全事件: " + rule.getDescription() + " - " + trafficLine
                );

                saveAlert(alert);
                logger.info("发现HTTP安全告警: {}", rule.getAlertType());
                break; // 一行流量只触发一条规则
            }
        }
    }

    private String extractUserInfo(String line) {
        Matcher matcher = USER_PATTERN.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractIpInfo(String line) {
        Matcher matcher = IP_PATTERN.matcher(line);
        return matcher.find() ? matcher.group() : null;
    }

    @PreDestroy
    public void cleanup() {
        stopSshMonitoring();
        stopHttpMonitoring();
        executorService.shutdown();
    }
}
package com.pennet.defender.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pennet.defender.config.SecurityMonitorConfig;
import com.pennet.defender.config.WebHookConfig;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;


@Service
public class SecurityMonitorServiceImpl implements SecurityMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityMonitorServiceImpl.class);
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern USER_PATTERN = Pattern.compile("user=(\\w+)");

    @Autowired
    private SecurityMonitorConfig config;

    @Autowired
    private SecurityAlertRepository alertRepository;

    @Autowired
    private WebHookConfig webHookConfig;


    private final RestTemplate restTemplate = new RestTemplate();

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
    public void stopHttpMonitoring() throws IOException {
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
        // 确保 detailInfo 不超过 255 个字符
        if (alert.getDetailInfo() != null && alert.getDetailInfo().length() > 255) {
            alert.setDetailInfo(alert.getDetailInfo().substring(0, 250) + "...");
        }

        // 先发送WebHook通知
        sendWebHookNotification(alert);

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

    // TODO /tmp/mitmproxy_script.py 配置好等待测试

    private void startMitmProxy() throws IOException {
        String mitmPath = "/usr/local/bin/mitmdump"; // 替换为你的实际路径
        mitmProxyProcess = Runtime.getRuntime().exec(mitmPath + " --mode regular@8082 -s /app/mitmproxy_script.py");
        mitmProxyProcess = Runtime.getRuntime().exec("sudo cp ~/.mitmproxy/mitmproxy-ca-cert.pem /usr/local/share/ca-certificates/mitmproxy-ca-cert.crt");
        mitmProxyProcess = Runtime.getRuntime().exec("sudo update-ca-certificates");
        logger.info("已启动 mitmproxy , IP为本地地址 , 端口为8082");
        mitmProxyProcess = Runtime.getRuntime().exec("echo \"export http_proxy=http://127.0.0.1:8082\" >> /root/.bashrc");
        mitmProxyProcess = Runtime.getRuntime().exec("echo \"export https_proxy=http://127.0.0.1:8082\" >> /root/.bashrc");
        mitmProxyProcess = Runtime.getRuntime().exec("echo \"export HTTP_PROXY=http://127.0.0.1:8082\" >> /root/.bashrc");
        mitmProxyProcess = Runtime.getRuntime().exec("echo \"export HTTPS_PROXY=http://127.0.0.1:8082\" >> /root/.bashrc");
//        mitmProxyProcess = Runtime.getRuntime().exec("source /root/.bashrc");
        try {
                // 使用 ProcessBuilder 执行 source ~/.bashrc 命令
                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "source /root/.bashrc");
                Process process = processBuilder.start();

                // 获取输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // 等待命令执行完成
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.info("source ~/.bashrc 已成功加载！");
                } else {
                    logger.warn("source ~/.bashrc 执行失败！");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        logger.info("已启动 mitmproxy 代理终端和 CLI 命令");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "8082");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "8082");
        logger.info("已启动 mitmproxy 代理Java");
    }

    private void stopMitmProxy() throws IOException {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        logger.info("已管理 mitmproxy 代理Java");
        mitmProxyProcess = Runtime.getRuntime().exec("sed -i '/export http_proxy=http:\\/\\/127.0.0.1:8082/d' /root/.bashrc");
        mitmProxyProcess = Runtime.getRuntime().exec("sed -i '/export https_proxy=http:\\/\\/127.0.0.1:8082/d' /root/.bashrc");
        mitmProxyProcess = Runtime.getRuntime().exec("sed -i '/export HTTP_PROXY=http:\\/\\/127.0.0.1:8082/d' /root/.bashrc");
        mitmProxyProcess = Runtime.getRuntime().exec("sed -i '/export HTTPS_PROXY=http:\\/\\/127.0.0.1:8082/d' /root/.bashrc");
//        mitmProxyProcess = Runtime.getRuntime().exec("source /root/.bashrc");
        try {
            // 使用 ProcessBuilder 执行 source ~/.bashrc 命令
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "source /root/.bashrc");
            Process process = processBuilder.start();

            // 获取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("source ~/.bashrc 已成功加载！");
            } else {
                logger.warn("source ~/.bashrc 执行失败！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("已启动 mitmproxy 代理终端和 CLI 命令");
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
                String line = null;
                System.out.println(line);
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
    public void cleanup() throws IOException {
        stopSshMonitoring();
        stopHttpMonitoring();
        executorService.shutdown();
    }


    //配置发送通知的代码
//    @Async
    private void sendWebHookNotification(SecurityAlert alert) {
        String message = formatAlertMessage(alert);

        if (webHookConfig.isWechatEnable()) {
            sendToWebHook(webHookConfig.getWechatWebHook(), formatWechatMessage(message));
        }
        if (webHookConfig.isDingdingEnable()) {
            sendToWebHook(webHookConfig.getDingdingWebhook(), formatDingdingMessage(message));
        }
    }

    private void sendToWebHook(String url, Map<String, Object> messagePayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(messagePayload, headers);
            restTemplate.postForEntity(url, request, String.class);
            logger.info("告警已发送到 WebHook: {}", url);
        } catch (Exception e) {
            logger.error("发送 WebHook 失败", e);
        }
    }

    private String formatAlertMessage(SecurityAlert alert) {
        return "\uD83D\uDEA8 系统告警通知 \uD83D\uDEA8\n"
                + "**告警类型:** " + alert.getAlertType() + "\n"
                + "**检测方式:** " + (alert.getDetectWay() == 1 ? "SSH监控" : "HTTP监控") + "\n"
                + "**用户:** " + (alert.getUserInfo() != null ? alert.getUserInfo() : "未知") + "\n"
                + "**IP:** " + (alert.getIpInfo() != null ? alert.getIpInfo() : "未知") + "\n"
                + "**详细信息:** " + alert.getDetailInfo() + "\n"
                + "**时间:** " + alert.getTimestamp();
    }

    private Map<String, Object> formatWechatMessage(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("msgtype", "text");
        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        message.put("text", text);
        return message;
    }

    private Map<String, Object> formatDingdingMessage(String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("msgtype", "text");
        Map<String, String> text = new HashMap<>();
        text.put("content", content);
        message.put("text", text);
        return message;
    }
}

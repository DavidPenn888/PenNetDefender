package com.pennet.defender.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pennet.defender.config.ProxyConfig;
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
    
    @Autowired
    private ProxyConfig proxyConfig;

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

    private void startMitmProxy() throws IOException {
        try {
            // 检查代理是否启用
            if (!proxyConfig.isProxyEnabled()) {
                logger.info("代理未启用，跳过mitmproxy启动");
                return;
            }
            
            // 创建临时文件来存储脚本内容
            File tempScriptFile = File.createTempFile("mitmproxy_script", ".py");
            tempScriptFile.deleteOnExit(); // 确保JVM退出时删除临时文件
            
            // 从类路径资源中读取脚本内容并写入临时文件
            try (InputStream inputStream = new ClassPathResource("scripts/mitmproxy_script.py").getInputStream();
                 OutputStream outputStream = new FileOutputStream(tempScriptFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            String scriptPath = tempScriptFile.getAbsolutePath();
            logger.info("mitmproxy脚本临时路径: {}", scriptPath);
            
            // 从配置中获取代理主机和端口
            String proxyHost = proxyConfig.getProxyHost();
            int proxyPort = proxyConfig.getProxyPort();
            
            // 启动mitmproxy代理
            String mitmPath = "mitmdump"; // 使用系统PATH中的mitmdump命令
            String[] command = {mitmPath, "--mode", "regular@" + proxyPort, "-s", scriptPath};
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // 合并标准输出和错误输出
            mitmProxyProcess = processBuilder.start();
            
            // 启动一个线程来读取mitmproxy的输出
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(mitmProxyProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("mitmproxy: {}", line);
                    }
                } catch (IOException e) {
                    logger.error("读取mitmproxy输出失败", e);
                }
            }).start();
            
            logger.info("已启动mitmproxy代理，监听地址{}:{}", proxyHost, proxyPort);
            
            // 设置Java系统代理
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", String.valueOf(proxyPort));
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", String.valueOf(proxyPort));
            logger.info("已设置Java系统代理");
            
            // 如果配置了系统级代理，则设置系统级CLI和Bash代理
            if (proxyConfig.isSystemWideProxy()) {
                setupSystemWideProxy();
                logger.info("已设置系统级CLI和Bash代理");
            }
        } catch (Exception e) {
            logger.error("启动mitmproxy失败", e);
            throw new IOException("启动mitmproxy失败: " + e.getMessage(), e);
        }
    }

    private void stopMitmProxy() throws IOException {
        try {
            // 清除Java系统代理设置
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            logger.info("已清除Java系统代理设置");
            
            // 清除系统级CLI和Bash代理设置
            cleanupSystemWideProxy();
            logger.info("已清除系统级CLI和Bash代理");
            
            // 停止mitmproxy进程
            if (mitmProxyProcess != null) {
                mitmProxyProcess.destroy();
                
                // 等待进程完全终止
                try {
                    boolean terminated = mitmProxyProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (!terminated) {
                        // 如果进程没有在5秒内终止，强制终止
                        mitmProxyProcess.destroyForcibly();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("等待mitmproxy进程终止时被中断", e);
                }
                
                mitmProxyProcess = null;
                logger.info("已停止mitmproxy代理");
            }
        } catch (Exception e) {
            logger.error("停止mitmproxy失败", e);
            throw new IOException("停止mitmproxy失败: " + e.getMessage(), e);
        }
    }

    @Async
    protected void monitorHttpTraffic() {
        // 此方法现在只是一个占位符，实际的HTTP流量监控由mitmproxy脚本处理
        // mitmproxy脚本会直接调用SecurityAlertController的API发送告警
        try {
            while (httpMonitorRunning.get()) {
                // 每10秒检查一次运行状态
                Thread.sleep(10000);
                if (mitmProxyProcess != null && !mitmProxyProcess.isAlive()) {
                    logger.error("mitmproxy进程已终止，尝试重新启动");
                    // 尝试重新启动mitmproxy
                    try {
                        startMitmProxy();
                    } catch (IOException e) {
                        logger.error("重新启动mitmproxy失败", e);
                        // 如果重启失败，停止监控
                        httpMonitorRunning.set(false);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("HTTP监控线程被中断");
        }
    }

    private void processHttpTrafficLine(String trafficLine) {
        // 此方法保留用于处理可能的直接HTTP流量数据
        // 大部分HTTP流量监控现在由mitmproxy脚本处理并直接调用API
        logger.debug("处理HTTP流量数据: {}", trafficLine);
        
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
    
    /**
     * 设置系统级CLI和Bash代理
     * 通过创建/etc/profile.d/http_proxy.sh脚本设置环境变量
     */
    private void setupSystemWideProxy() {
        try {
            // 从配置中获取代理主机和端口
            String proxyHost = proxyConfig.getProxyHost();
            int proxyPort = proxyConfig.getProxyPort();
            String proxyUrl = proxyConfig.getProxyUrl();
            
            // 创建代理配置脚本内容
            String proxyScript = "#!/bin/bash\n" +
                    "# 由PenNetDefender自动生成的代理配置\n" +
                    "export HTTP_PROXY=\"" + proxyUrl + "\"\n" +
                    "export HTTPS_PROXY=\"" + proxyUrl + "\"\n" +
                    "export http_proxy=\"" + proxyUrl + "\"\n" +
                    "export https_proxy=\"" + proxyUrl + "\"\n" +
                    "export NO_PROXY=\"localhost,127.0.0.1,::1\"\n" +
                    "export no_proxy=\"localhost,127.0.0.1,::1\"\n";
            
            // 使用sudo命令创建脚本文件
            Process process = Runtime.getRuntime().exec(new String[]{"sudo", "bash", "-c", "cat > /etc/profile.d/http_proxy.sh << 'EOL'\n" + proxyScript + "EOL\n"});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 设置脚本权限
                Process chmodProcess = Runtime.getRuntime().exec(new String[]{"sudo", "chmod", "755", "/etc/profile.d/http_proxy.sh"});
                chmodProcess.waitFor();
                
                logger.info("系统级CLI和Bash代理配置已创建");
                
                // 通知用户需要重新登录或重新加载配置文件以使代理生效
                logger.info("注意：新的终端会话将自动应用代理设置，现有会话需要执行 'source /etc/profile.d/http_proxy.sh' 使代理生效");
            } else {
                logger.error("创建系统级代理配置失败，退出码: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("设置系统级CLI和Bash代理失败", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 清除系统级CLI和Bash代理
     * 通过删除/etc/profile.d/http_proxy.sh脚本清除环境变量
     */
    private void cleanupSystemWideProxy() {
        try {
            // 删除代理配置脚本
            Process process = Runtime.getRuntime().exec(new String[]{"sudo", "rm", "-f", "/etc/profile.d/http_proxy.sh"});
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                logger.info("系统级CLI和Bash代理配置已删除");
                
                // 通知用户需要重新登录或手动清除环境变量
                logger.info("注意：新的终端会话将不再使用代理，现有会话需要手动清除环境变量");
            } else {
                logger.error("删除系统级代理配置失败，退出码: {}", exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("清除系统级CLI和Bash代理失败", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
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

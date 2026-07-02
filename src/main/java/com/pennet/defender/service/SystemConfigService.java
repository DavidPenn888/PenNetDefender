package com.pennet.defender.service;

import com.pennet.defender.config.ProxyConfig;
import com.pennet.defender.config.SecurityMonitorConfig;
import com.pennet.defender.config.ThresholdConfig;
import com.pennet.defender.config.WebHookConfig;
import com.pennet.defender.model.SystemConfig;
import com.pennet.defender.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private ThresholdConfig thresholdConfig;

    @Autowired
    private WebHookConfig webHookConfig;
    
    @Autowired
    private ProxyConfig proxyConfig;
    
    @Autowired
    private SecurityMonitorConfig securityMonitorConfig;

    // 配置键常量
    private static final String CPU_THRESHOLD_KEY = "threshold.cpu";
    private static final String MEMORY_THRESHOLD_KEY = "threshold.memory";
    private static final String STORAGE_THRESHOLD_KEY = "threshold.storage";
    private static final String WECHAT_ENABLE_KEY = "alert.webhook.wechat.enable";
    private static final String WECHAT_URL_KEY = "alert.webhook.wechat.url";
    private static final String DINGDING_ENABLE_KEY = "alert.webhook.dingding.enable";
    private static final String DINGDING_URL_KEY = "alert.webhook.dingding.url";
    // 代理配置键常量
    private static final String PROXY_ENABLED_KEY = "security.proxy.enabled";
    private static final String PROXY_HOST_KEY = "security.proxy.host";
    private static final String PROXY_PORT_KEY = "security.proxy.port";
    private static final String PROXY_SYSTEM_WIDE_KEY = "security.proxy.system.wide";
    // 监控配置键常量
    private static final String SSH_MONITOR_ENABLED_KEY = "security.monitor.ssh.enabled";
    private static final String HTTP_MONITOR_ENABLED_KEY = "security.monitor.http.enabled";

    /**
     * 应用启动时初始化配置
     * 如果数据库中没有配置，则使用默认配置并保存到数据库
     */
    @PostConstruct
    public void init() {
        // 初始化阈值配置
        initConfig(CPU_THRESHOLD_KEY, String.valueOf(thresholdConfig.getCpuThreshold()), "CPU使用率阈值");
        initConfig(MEMORY_THRESHOLD_KEY, String.valueOf(thresholdConfig.getMemoryThreshold()), "内存使用率阈值");
        initConfig(STORAGE_THRESHOLD_KEY, String.valueOf(thresholdConfig.getStorageThreshold()), "存储使用率阈值");

        // 初始化WebHook配置
        initConfig(WECHAT_ENABLE_KEY, String.valueOf(webHookConfig.isWechatEnable()), "微信WebHook启用状态");
        initConfig(WECHAT_URL_KEY, webHookConfig.getWechatWebHook(), "微信WebHook URL");
        initConfig(DINGDING_ENABLE_KEY, String.valueOf(webHookConfig.isDingdingEnable()), "钉钉WebHook启用状态");
        initConfig(DINGDING_URL_KEY, webHookConfig.getDingdingWebhook(), "钉钉WebHook URL");
        
        // 初始化代理配置
        initConfig(PROXY_ENABLED_KEY, String.valueOf(proxyConfig.isProxyEnabled()), "代理启用状态");
        initConfig(PROXY_HOST_KEY, proxyConfig.getProxyHost(), "代理主机地址");
        initConfig(PROXY_PORT_KEY, String.valueOf(proxyConfig.getProxyPort()), "代理端口");
        initConfig(PROXY_SYSTEM_WIDE_KEY, String.valueOf(proxyConfig.isSystemWideProxy()), "系统级代理启用状态");

        // 初始化监控配置
        initConfig(SSH_MONITOR_ENABLED_KEY, String.valueOf(securityMonitorConfig.isSshMonitorEnabled()), "SSH监控启用状态");
        initConfig(HTTP_MONITOR_ENABLED_KEY, String.valueOf(securityMonitorConfig.isHttpMonitorEnabled()), "HTTP监控启用状态");

        // 从数据库加载配置到内存
        loadConfigFromDatabase();
    }

    /**
     * 初始化配置项
     * 如果数据库中不存在该配置项，则使用默认值创建
     */
    private void initConfig(String key, String defaultValue, String description) {
        Optional<SystemConfig> configOptional = systemConfigRepository.findByConfigKey(key);
        if (configOptional.isEmpty()) {
            SystemConfig config = new SystemConfig(key, defaultValue, description);
            systemConfigRepository.save(config);
        }
    }

    /**
     * 从数据库加载配置到内存
     */
    public void loadConfigFromDatabase() {
        // 加载阈值配置
        loadThresholdConfig();
        
        // 加载WebHook配置
        loadWebHookConfig();
        
        // 加载代理配置
        loadProxyConfig();
        
        // 加载监控配置
        loadMonitorConfig();
    }

    /**
     * 加载阈值配置
     */
    private void loadThresholdConfig() {
        getConfigValue(CPU_THRESHOLD_KEY).ifPresent(value -> 
            thresholdConfig.setCpuThreshold(Integer.parseInt(value)));
        
        getConfigValue(MEMORY_THRESHOLD_KEY).ifPresent(value -> 
            thresholdConfig.setMemoryThreshold(Integer.parseInt(value)));
        
        getConfigValue(STORAGE_THRESHOLD_KEY).ifPresent(value -> 
            thresholdConfig.setStorageThreshold(Integer.parseInt(value)));
    }

    /**
     * 加载WebHook配置
     */
    private void loadWebHookConfig() {
        getConfigValue(WECHAT_ENABLE_KEY).ifPresent(value -> 
            webHookConfig.setWechatEnable(Boolean.parseBoolean(value)));
        
        getConfigValue(WECHAT_URL_KEY).ifPresent(value -> 
            webHookConfig.setWechatWebHook(value));
        
        getConfigValue(DINGDING_ENABLE_KEY).ifPresent(value -> 
            webHookConfig.setDingdingEnable(Boolean.parseBoolean(value)));
        
        getConfigValue(DINGDING_URL_KEY).ifPresent(value -> 
            webHookConfig.setDingdingWebhook(value));
    }

    /**
     * 获取配置值
     */
    private Optional<String> getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue);
    }

    /**
     * 更新阈值配置
     */
    public void updateThresholdConfig(int cpuThreshold, int memoryThreshold, int storageThreshold) {
        // 更新内存中的配置
        thresholdConfig.setCpuThreshold(cpuThreshold);
        thresholdConfig.setMemoryThreshold(memoryThreshold);
        thresholdConfig.setStorageThreshold(storageThreshold);

        // 更新数据库中的配置
        updateConfig(CPU_THRESHOLD_KEY, String.valueOf(cpuThreshold));
        updateConfig(MEMORY_THRESHOLD_KEY, String.valueOf(memoryThreshold));
        updateConfig(STORAGE_THRESHOLD_KEY, String.valueOf(storageThreshold));
    }

    /**
     * 更新WebHook配置
     */
    public void updateWebHookConfig(boolean wechatEnable, String wechatUrl, 
                                   boolean dingdingEnable, String dingdingUrl) {
        // 更新内存中的配置
        webHookConfig.updateWebHookConfig(wechatEnable, dingdingEnable, wechatUrl, dingdingUrl);

        // 更新数据库中的配置
        updateConfig(WECHAT_ENABLE_KEY, String.valueOf(wechatEnable));
        updateConfig(WECHAT_URL_KEY, wechatUrl);
        updateConfig(DINGDING_ENABLE_KEY, String.valueOf(dingdingEnable));
        updateConfig(DINGDING_URL_KEY, dingdingUrl);
    }

    /**
     * 加载代理配置
     */
    private void loadProxyConfig() {
        getConfigValue(PROXY_ENABLED_KEY).ifPresent(value -> 
            proxyConfig.setProxyEnabled(Boolean.parseBoolean(value)));
        
        getConfigValue(PROXY_HOST_KEY).ifPresent(value -> 
            proxyConfig.setProxyHost(value));
        
        getConfigValue(PROXY_PORT_KEY).ifPresent(value -> 
            proxyConfig.setProxyPort(Integer.parseInt(value)));
        
        getConfigValue(PROXY_SYSTEM_WIDE_KEY).ifPresent(value -> 
            proxyConfig.setSystemWideProxy(Boolean.parseBoolean(value)));
    }
    
    /**
     * 加载监控配置
     */
    private void loadMonitorConfig() {
        getConfigValue(SSH_MONITOR_ENABLED_KEY).ifPresent(value -> 
            securityMonitorConfig.setSshMonitorEnabled(Boolean.parseBoolean(value)));
        
        getConfigValue(HTTP_MONITOR_ENABLED_KEY).ifPresent(value -> 
            securityMonitorConfig.setHttpMonitorEnabled(Boolean.parseBoolean(value)));
    }
    
    /**
     * 更新SSH监控配置
     */
    public void updateSshMonitorEnabled(boolean enabled) {
        securityMonitorConfig.setSshMonitorEnabled(enabled);
        updateConfig(SSH_MONITOR_ENABLED_KEY, String.valueOf(enabled));
    }

    /**
     * 更新HTTP监控配置
     */
    public void updateHttpMonitorEnabled(boolean enabled) {
        securityMonitorConfig.setHttpMonitorEnabled(enabled);
        updateConfig(HTTP_MONITOR_ENABLED_KEY, String.valueOf(enabled));
    }
    
    /**
     * 更新代理配置
     */
    public void updateProxyConfig(boolean proxyEnabled, String proxyHost, int proxyPort, boolean systemWideProxy) {
        // 更新内存中的配置
        proxyConfig.setProxyEnabled(proxyEnabled);
        proxyConfig.setProxyHost(proxyHost);
        proxyConfig.setProxyPort(proxyPort);
        proxyConfig.setSystemWideProxy(systemWideProxy);

        // 更新数据库中的配置
        updateConfig(PROXY_ENABLED_KEY, String.valueOf(proxyEnabled));
        updateConfig(PROXY_HOST_KEY, proxyHost);
        updateConfig(PROXY_PORT_KEY, String.valueOf(proxyPort));
        updateConfig(PROXY_SYSTEM_WIDE_KEY, String.valueOf(systemWideProxy));
    }
    
    /**
     * 更新配置
     */
    private void updateConfig(String key, String value) {
        systemConfigRepository.findByConfigKey(key).ifPresent(config -> {
            config.setConfigValue(value);
            systemConfigRepository.save(config);
        });
    }
}
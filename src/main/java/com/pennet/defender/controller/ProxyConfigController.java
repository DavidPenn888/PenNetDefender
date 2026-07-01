package com.pennet.defender.controller;

import com.pennet.defender.config.ProxyConfig;
import com.pennet.defender.model.ApiResponse;
import com.pennet.defender.service.SecurityMonitorService;
import com.pennet.defender.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 代理配置控制器
 * 提供API接口用于查询和修改代理配置
 */
@RestController
@RequestMapping("/api/proxy")
public class ProxyConfigController {

    @Autowired
    private ProxyConfig proxyConfig;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private SecurityMonitorService securityMonitorService;
    
    /**
     * 获取当前代理配置
     * @return 代理配置信息
     */
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getProxyConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", proxyConfig.isProxyEnabled());
        config.put("host", proxyConfig.getProxyHost());
        config.put("port", proxyConfig.getProxyPort());
        config.put("systemWide", proxyConfig.isSystemWideProxy());
        config.put("url", proxyConfig.getProxyUrl());
        config.put("httpMonitorRunning", securityMonitorService.isHttpMonitoringRunning());
        
        return new ApiResponse<>(0, config, "success");
    }
    
    /**
     * 更新代理配置
     * @param config 新的代理配置
     * @return 操作结果
     */
    @PostMapping("/update")
    public ApiResponse<Void> updateProxyConfig(@RequestBody Map<String, Object> config) {
        boolean enabled = (boolean) config.get("enabled");
        String host = (String) config.get("host");
        int port = ((Number) config.get("port")).intValue();
        boolean systemWide = (boolean) config.get("systemWide");
        
        // 使用SystemConfigService更新代理配置，确保持久化保存
        systemConfigService.updateProxyConfig(enabled, host, port, systemWide);
        
        // 如果HTTP监控正在运行，需要重启以应用新配置
        if (securityMonitorService.isHttpMonitoringRunning()) {
            try {
                // 停止HTTP监控
                securityMonitorService.stopHttpMonitoring();
                
                // 如果代理启用，则重新启动HTTP监控
                if (enabled) {
                    securityMonitorService.startHttpMonitoring();
                }
            } catch (IOException e) {
                return new ApiResponse<>(1, null, "更新代理配置成功，但重启HTTP监控失败: " + e.getMessage());
            }
        }
        
        return new ApiResponse<>(0, null, "代理配置已更新并应用");
    }
    
    /**
     * 重启HTTP监控
     * @return 操作结果
     */
    @PostMapping("/restart-http-monitor")
    public ApiResponse<Void> restartHttpMonitor() {
        try {
            // 如果HTTP监控正在运行，先停止
            if (securityMonitorService.isHttpMonitoringRunning()) {
                securityMonitorService.stopHttpMonitoring();
            }
            
            // 如果代理启用，则启动HTTP监控
            if (proxyConfig.isProxyEnabled()) {
                securityMonitorService.startHttpMonitoring();
                return new ApiResponse<>(0, null, "HTTP监控已重启");
            } else {
                return new ApiResponse<>(0, null, "代理未启用，HTTP监控未启动");
            }
        } catch (IOException e) {
            return new ApiResponse<>(1, null, "重启HTTP监控失败: " + e.getMessage());
        }
    }
}
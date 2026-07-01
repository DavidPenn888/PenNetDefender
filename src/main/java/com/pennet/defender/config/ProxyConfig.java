package com.pennet.defender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 代理配置类，用于存储和管理代理相关的配置
 */
@Configuration
public class ProxyConfig {

    @Value("${security.proxy.enabled:true}")
    private boolean proxyEnabled;

    @Value("${security.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${security.proxy.port:8082}")
    private int proxyPort;

    @Value("${security.proxy.system.wide:true}")
    private boolean systemWideProxy;

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public void setProxyEnabled(boolean proxyEnabled) {
        this.proxyEnabled = proxyEnabled;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean isSystemWideProxy() {
        return systemWideProxy;
    }

    public void setSystemWideProxy(boolean systemWideProxy) {
        this.systemWideProxy = systemWideProxy;
    }

    /**
     * 获取完整的代理URL
     * @return 代理URL，格式为http://host:port
     */
    public String getProxyUrl() {
        return "http://" + proxyHost + ":" + proxyPort;
    }
}
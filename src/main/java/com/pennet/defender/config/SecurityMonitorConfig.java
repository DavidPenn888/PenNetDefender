package com.pennet.defender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityMonitorConfig {

    @Value("${security.monitor.ssh.enabled:false}")
    private boolean sshMonitorEnabled;

    @Value("${security.monitor.http.enabled:false}")
    private boolean httpMonitorEnabled;

    @Value("${security.monitor.ssh.rulePath:classpath:rules/ssh_rules.json}")
    private String sshRulePath;

    @Value("${security.monitor.http.rulePath:classpath:rules/http_rules.json}")
    private String httpRulePath;

    @Value("${security.monitor.auditd.logPath:/var/log/audit/audit.log}")
    private String auditdLogPath;

    public boolean isSshMonitorEnabled() {
        return sshMonitorEnabled;
    }

    public void setSshMonitorEnabled(boolean sshMonitorEnabled) {
        this.sshMonitorEnabled = sshMonitorEnabled;
    }

    public boolean isHttpMonitorEnabled() {
        return httpMonitorEnabled;
    }

    public void setHttpMonitorEnabled(boolean httpMonitorEnabled) {
        this.httpMonitorEnabled = httpMonitorEnabled;
    }

    public String getSshRulePath() {
        return sshRulePath;
    }

    public String getHttpRulePath() {
        return httpRulePath;
    }

    public String getAuditdLogPath() {
        return auditdLogPath;
    }
}

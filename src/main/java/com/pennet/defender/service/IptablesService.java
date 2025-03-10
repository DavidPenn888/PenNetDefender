package com.pennet.defender.service;

import com.pennet.defender.model.FirewallRule;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IptablesService {

    // 检查防火墙状态
    public boolean isFirewallEnabled() throws IOException {
        Process process = Runtime.getRuntime().exec("iptables -L INPUT -n");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        // 检查默认策略是否为DROP或REJECT
        while ((line = reader.readLine()) != null) {
            if (line.contains("Chain INPUT") && (line.contains("policy DROP") || line.contains("policy REJECT"))) {
                return true;
            }
        }

        return false;
    }

    // 启用防火墙
    public void enableFirewall() throws IOException {
        // 设置默认策略为DROP
        executeCommand("iptables -P INPUT DROP");
        executeCommand("iptables -P FORWARD DROP");
        // 允许已建立的连接
        executeCommand("iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT");
        // 允许本地回环接口
        executeCommand("iptables -A INPUT -i lo -j ACCEPT");
    }

    // 禁用防火墙
    public void disableFirewall() throws IOException {
        // 清空所有规则
        executeCommand("iptables -F");
        // 设置默认策略为ACCEPT
        executeCommand("iptables -P INPUT ACCEPT");
        executeCommand("iptables -P FORWARD ACCEPT");
        executeCommand("iptables -P OUTPUT ACCEPT");
    }

    // 添加防火墙规则
    public void addRule(FirewallRule rule) throws IOException {
        String command = buildIptablesCommand("-A", rule);
        executeCommand(command);
    }

    // 更新防火墙规则（删除旧规则，添加新规则）
    public void updateRule(FirewallRule oldRule, FirewallRule newRule) throws IOException {
        String deleteCommand = buildIptablesCommand("-D", oldRule);
        executeCommand(deleteCommand);

        String addCommand = buildIptablesCommand("-A", newRule);
        executeCommand(addCommand);
    }

    // 删除防火墙规则
    public void deleteRule(FirewallRule rule) throws IOException {
        String command = buildIptablesCommand("-D", rule);
        executeCommand(command);
    }

    // 获取当前iptables规则
    public List<String> getCurrentRules() throws IOException {
        Process process = Runtime.getRuntime().exec("iptables -L -n --line-numbers");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        List<String> rules = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            rules.add(line);
        }

        return rules;
    }

    // 解析iptables规则为FirewallRule对象
    public List<FirewallRule> parseIptablesRules() throws IOException {
        List<String> currentRules = getCurrentRules();
        List<FirewallRule> firewallRules = new ArrayList<>();
        String currentChain = null;

        Pattern rulePattern = Pattern.compile("^(\\d+)\\s+(\\w+)\\s+(\\w+)\\s+(.*)$");

        for (String line : currentRules) {
            if (line.startsWith("Chain")) {
                // 提取链名称
                String[] parts = line.split("\\s+");
                if (parts.length > 1) {
                    currentChain = parts[1];
                }
            } else if (line.matches("^\\d+.*") && currentChain != null) {
                // 这是一条规则行
                Matcher matcher = rulePattern.matcher(line);
                if (matcher.find()) {
                    FirewallRule rule = new FirewallRule();
                    rule.setChain(currentChain);
                    rule.setAction(matcher.group(2));
                    rule.setPriority(Integer.parseInt(matcher.group(1)));

                    // 解析协议、源地址、目标地址和端口
                    String details = matcher.group(4);
                    if (details.contains("tcp") || details.contains("udp") || details.contains("icmp")) {
                        rule.setProtocol(details.contains("tcp") ? "tcp" :
                                details.contains("udp") ? "udp" : "icmp");
                    } else {
                        rule.setProtocol("all");
                    }

                    // 提取源地址
                    Pattern sourcePattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+(?:/\\d+)?)");
                    Matcher sourceMatcher = sourcePattern.matcher(details);
                    if (sourceMatcher.find()) {
                        rule.setSource(sourceMatcher.group(1));
                    } else {
                        rule.setSource("0.0.0.0/0");
                    }

                    // 提取目标地址
                    Pattern destPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+(?:/\\d+)?)");
                    Matcher destMatcher = destPattern.matcher(details.substring(sourceMatcher.end()));
                    if (destMatcher.find()) {
                        rule.setDestination(destMatcher.group(1));
                    } else {
                        rule.setDestination("0.0.0.0/0");
                    }

                    // 提取端口
                    Pattern portPattern = Pattern.compile("dpt:(\\d+)");
                    Matcher portMatcher = portPattern.matcher(details);
                    if (portMatcher.find()) {
                        rule.setPort(portMatcher.group(1));
                    } else {
                        rule.setPort("");
                    }

                    firewallRules.add(rule);
                }
            }
        }

        return firewallRules;
    }

    // 构建iptables命令
    private String buildIptablesCommand(String action, FirewallRule rule) {
        StringBuilder command = new StringBuilder("iptables " + action + " " + rule.getChain());

        if (rule.getProtocol() != null && !rule.getProtocol().isEmpty() && !rule.getProtocol().equals("all")) {
            command.append(" -p ").append(rule.getProtocol());
        }

        if (rule.getSource() != null && !rule.getSource().isEmpty() && !rule.getSource().equals("0.0.0.0/0")) {
            command.append(" -s ").append(rule.getSource());
        }

        if (rule.getDestination() != null && !rule.getDestination().isEmpty() && !rule.getDestination().equals("0.0.0.0/0")) {
            command.append(" -d ").append(rule.getDestination());
        }

        if (rule.getPort() != null && !rule.getPort().isEmpty()) {
            command.append(" --dport ").append(rule.getPort());
        }

        command.append(" -j ").append(rule.getAction());

        return command.toString();
    }

    // 执行命令
    private void executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorMessage = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorMessage.append(line).append("\n");
                }
                throw new IOException("Command execution failed: " + errorMessage.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted", e);
        }
    }
}
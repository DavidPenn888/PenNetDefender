package com.pennet.defender.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class FirewallCommandService {

    /**
     * 执行iptables命令并返回输出
     */
    public String executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    StringBuilder errorOutput = new StringBuilder();
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    throw new IOException("Command execution failed with exit code " + exitCode + ": " + errorOutput);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted", e);
        }

        return output.toString();
    }

    /**
     * 检查防火墙是否启用
     */
    public boolean isFirewallEnabled() throws IOException {
        String output = executeCommand("iptables -L -n");
        // 如果防火墙被禁用，通常会返回"Chain INPUT (policy ACCEPT)"等字样
        // 这里简单检查是否有活跃规则
        return !output.contains("Chain INPUT (policy ACCEPT)") ||
                output.split("\n").length > 9;  // 基本的iptables输出有9行以上表示有规则
    }

    /**
     * 启用防火墙
     */
    public void enableFirewall() throws IOException {
        // 设置默认策略
        executeCommand("iptables -P INPUT ACCEPT");
        executeCommand("iptables -P OUTPUT ACCEPT");
        executeCommand("iptables -P FORWARD DROP");

        // 添加基本规则 - 允许已建立和相关的连接
        executeCommand("iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT");

        // 允许本地回环接口
        executeCommand("iptables -A INPUT -i lo -j ACCEPT");
    }

    /**
     * 禁用防火墙
     */
    public void disableFirewall() throws IOException {
        // 清空所有规则
        executeCommand("iptables -F");

        // 设置默认策略为ACCEPT
        executeCommand("iptables -P INPUT ACCEPT");
        executeCommand("iptables -P OUTPUT ACCEPT");
        executeCommand("iptables -P FORWARD ACCEPT");
    }

    /**
     * 添加防火墙规则
     */
    public void addRule(String chain, String action, String protocol, String source,
                        String destination, String port, int priority) throws IOException {

        StringBuilder command = new StringBuilder("iptables -I ").append(chain).append(" ").append(priority).append(" ");

        if (protocol != null && !protocol.isEmpty() && !protocol.equalsIgnoreCase("all")) {
            command.append("-p ").append(protocol).append(" ");
        }

        if (source != null && !source.isEmpty()) {
            command.append("-s ").append(source).append(" ");
        }

        if (destination != null && !destination.isEmpty()) {
            command.append("-d ").append(destination).append(" ");
        }

        if (port != null && !port.isEmpty() && (protocol.equals("tcp") || protocol.equals("udp"))) {
            command.append("--dport ").append(port).append(" ");
        }

        command.append("-j ").append(action);

        executeCommand(command.toString());
    }

    /**
     * 删除防火墙规则
     */
    public void deleteRule(String chain, int ruleNumber) throws IOException {
        executeCommand("iptables -D " + chain + " " + ruleNumber);
    }

    /**
     * 获取当前iptables filter规则列表
     */
    public List<String> listRules() throws IOException {
        String output = executeCommand("iptables -L -n --line-numbers");
        List<String> rules = new ArrayList<>();

        String[] lines = output.split("\n");
        boolean inFilterChain = false;
        String currentChain = null;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("Chain INPUT") || line.startsWith("Chain FORWARD") || line.startsWith("Chain OUTPUT")) {
                inFilterChain = true;
                currentChain = line.split("\\s+")[1];
                continue;
            }

            if (inFilterChain && line.startsWith("num")) {
                continue; // Skip header line
            }

            if (inFilterChain && !line.isEmpty() && Character.isDigit(line.charAt(0))) {
                rules.add(currentChain + ":" + line);
            } else if (line.isEmpty()) {
                inFilterChain = false;
            }
        }

        return rules;
    }

    /**
     * 获取iptables版本
     */
    public String getFirewallVersion() throws IOException {
        String output = executeCommand("iptables --version");
        return output.trim();
    }
}
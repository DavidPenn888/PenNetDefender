package com.pennet.defender.service;

import com.pennet.defender.model.FirewallRule;
import com.pennet.defender.model.FirewallStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(IptablesService.class);

    private static final String[] VALID_CHAINS = {"INPUT", "OUTPUT", "FORWARD"};
    private static final String[] VALID_ACTIONS = {"ACCEPT", "DROP", "REJECT"};

    /**
     * 检查防火墙是否启用
     */
    public boolean isFirewallEnabled() throws IOException {
        Process process = Runtime.getRuntime().exec("iptables -L");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            // 如果能成功执行iptables命令并获取输出，则认为防火墙已启用
            return reader.readLine() != null;
        }
    }

    /**
     * 获取防火墙状态
     */
    public FirewallStatus getFirewallStatus() throws IOException {
        FirewallStatus status = new FirewallStatus();
        status.setEnabled(isFirewallEnabled());

        if (status.isEnabled()) {
//            Process process = Runtime.getRuntime().exec("iptables -L -n --line-numbers");
            Process process = Runtime.getRuntime().exec("iptables -t filter -L -n --line-numbers");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                String currentChain = null;
                int inputRules = 0, outputRules = 0, forwardRules = 0;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("Chain INPUT")) {
                        currentChain = "INPUT";
                    } else if (line.startsWith("Chain OUTPUT")) {
                        currentChain = "OUTPUT";
                    } else if (line.startsWith("Chain FORWARD")) {
                        currentChain = "FORWARD";
                    } else if (line.matches("^\\d+.*") && currentChain != null) {
                        // 计数规则行
                        switch (currentChain) {
                            case "INPUT": inputRules++; break;
                            case "OUTPUT": outputRules++; break;
                            case "FORWARD": forwardRules++; break;
                        }
                    }
                }

                status.setInputRules(inputRules);
                status.setOutputRules(outputRules);
                status.setForwardRules(forwardRules);
                // 修改A5：总规则数量只统计filter表中的INPUT/OUTPUT/FORWARD链的规则
                status.setTotalRules(inputRules + outputRules + forwardRules);
            }
        }

        return status;
    }

    /**
     * 启用防火墙
     */
    public void enableFirewall() throws IOException {
        executeCommand("iptables -P INPUT ACCEPT");
        executeCommand("iptables -P OUTPUT ACCEPT");
        executeCommand("iptables -P FORWARD ACCEPT");
    }

    /**
     * 禁用防火墙
     */
    public void disableFirewall() throws IOException {
        // 清除所有规则
        executeCommand("iptables -F");
        // 设置默认策略为ACCEPT
        executeCommand("iptables -P INPUT ACCEPT");
        executeCommand("iptables -P OUTPUT ACCEPT");
        executeCommand("iptables -P FORWARD ACCEPT");
    }

    /**
     * 添加防火墙规则
     */
    public void addRule(FirewallRule rule) throws IOException {
        validateRule(rule);

        StringBuilder command = new StringBuilder("iptables -A ");
        command.append(rule.getChain()).append(" ");

        if (rule.getProtocol() != null && !rule.getProtocol().isEmpty() && !rule.getProtocol().equalsIgnoreCase("all")) {
            command.append("-p ").append(rule.getProtocol()).append(" ");
        }

        if (rule.getSource() != null && !rule.getSource().isEmpty()) {
            command.append("-s ").append(rule.getSource()).append(" ");
        }

        if (rule.getDestination() != null && !rule.getDestination().isEmpty()) {
            command.append("-d ").append(rule.getDestination()).append(" ");
        }

        if (rule.getPort() != null && !rule.getPort().isEmpty()) {
            command.append("--dport ").append(rule.getPort()).append(" ");
        }

        command.append("-j ").append(rule.getAction());

        executeCommand(command.toString());
    }

    /**
     * 删除防火墙规则
     */
    public void deleteRule(FirewallRule rule) throws IOException {
        // 通过规则号删除
        if (rule.getId() != null) {
            String command = "iptables -D " + rule.getChain() + " " + rule.getPriority();
            executeCommand(command);
        }
    }

    /**
     * 解析iptables规则到FirewallRule对象
     */
    public List<FirewallRule> parseIptablesRules() throws IOException {
        List<FirewallRule> rules = new ArrayList<>();

        for (String chain : VALID_CHAINS) {
            Process process = Runtime.getRuntime().exec("iptables -L " + chain + " -n --line-numbers");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                // 跳过标题行
                reader.readLine(); // Chain INPUT (policy ACCEPT)
                reader.readLine(); // num   target     prot opt source               destination

                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        FirewallRule rule = parseRuleLine(line, chain);
                        if (rule != null) {
                            rules.add(rule);
                        }
                    }
                }
            }
        }

        return rules;
    }

    /**
     * 解析单行iptables规则
     */
    private FirewallRule parseRuleLine(String line, String chain) {
        // 示例: 1    ACCEPT     tcp  --  0.0.0.0/0            0.0.0.0/0            tcp dpt:22
        Pattern pattern = Pattern.compile("^(\\d+)\\s+(\\w+)\\s+(\\w+)\\s+--\\s+([\\d\\.\\S]+)\\s+([\\d\\.\\S]+)(?:\\s+(.*))?$");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            FirewallRule rule = new FirewallRule();
            rule.setChain(chain);
            rule.setPriority(Integer.parseInt(matcher.group(1)));
            rule.setAction(matcher.group(2));
            rule.setProtocol(matcher.group(3));
            rule.setSource(matcher.group(4));
            rule.setDestination(matcher.group(5));

            // 解析端口信息
            String extra = matcher.group(6);
            if (extra != null && extra.contains("dpt:")) {
                Pattern portPattern = Pattern.compile("dpt:(\\d+)");
                Matcher portMatcher = portPattern.matcher(extra);
                if (portMatcher.find()) {
                    rule.setPort(portMatcher.group(1));
                }
            }

            return rule;
        }

        return null;
    }

    /**
     * 执行命令并返回结果
     */
    private String executeCommand(String command) throws IOException {
        logger.info("Executing command: {}", command);
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
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");
                    }
                    throw new IOException("Command execution failed with exit code " + exitCode + ": " + errorOutput.toString());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution interrupted", e);
        }

        return output.toString();
    }

    /**
     * 验证规则有效性
     */
    private void validateRule(FirewallRule rule) {
        if (rule.getChain() == null || !isValidChain(rule.getChain())) {
            throw new IllegalArgumentException("Invalid chain: " + rule.getChain());
        }

        if (rule.getAction() == null || !isValidAction(rule.getAction())) {
            throw new IllegalArgumentException("Invalid action: " + rule.getAction());
        }
    }

    private boolean isValidChain(String chain) {
        for (String validChain : VALID_CHAINS) {
            if (validChain.equalsIgnoreCase(chain)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidAction(String action) {
        for (String validAction : VALID_ACTIONS) {
            if (validAction.equalsIgnoreCase(action)) {
                return true;
            }
        }
        return false;
    }
}
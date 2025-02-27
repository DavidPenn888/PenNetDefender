package com.pennet.defender.service;

import com.pennet.defender.service.SystemMonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class SystemMonitorTask {

    private final SystemMonitorService systemMonitorService;

    public SystemMonitorTask(SystemMonitorService systemMonitorService) {
        this.systemMonitorService = systemMonitorService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void monitorSystem() {
        // 获取系统的CPU、内存、存储使用情况（这里需要调用系统命令来获取这些信息）
        double cpuUsage = getCpuUsage(); // 替换为实际获取的方法
        double memoryUsage = getMemoryUsage(); // 替换为实际获取的方法
        double storageUsage = getStorageUsage(); // 替换为实际获取的方法

        systemMonitorService.saveSystemStatus(cpuUsage, memoryUsage, storageUsage);
    }

    private double getCpuUsage() {
        try {
            String command = "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - $1}'";
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return Double.parseDouble(line.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private double getMemoryUsage() {
        try {
            String command = "free | grep Mem | awk '{print $3/$2 * 100.0}'";
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return Double.parseDouble(line.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private double getStorageUsage() {
        try {
            String command = "df / | grep / | awk '{ print $5 }' | sed 's/%//g'";
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return Double.parseDouble(line.trim());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}

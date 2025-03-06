package com.pennet.defender.service;

import com.pennet.defender.service.SystemMonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;

//import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

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

//    private double getCpuUsage() {
//        try {
//            String command = "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - $1}'";
//            Process process = Runtime.getRuntime().exec(command);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line = reader.readLine();
//            return Double.parseDouble(line.trim());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
private double getCpuUsage() {
    try {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                com.sun.management.OperatingSystemMXBean.class);

        // 获取CPU负载 (0.0 ~ 1.0)
        double cpuLoad = osBean.getSystemCpuLoad();

        // 如果获取失败（返回负值），尝试使用系统负载
        if (cpuLoad < 0) {
            cpuLoad = osBean.getSystemLoadAverage() / osBean.getAvailableProcessors();
            if (cpuLoad < 0) {
                cpuLoad = 0;
            }
        }

        return cpuLoad * 100.0; // 转换为百分比
    } catch (Exception e) {
        e.printStackTrace();
        return 0.0;
    }
}
//    private double getMemoryUsage() {
//        try {
//            String command = "free | grep Mem | awk '{print $3/$2 * 100.0}'";
//            Process process = Runtime.getRuntime().exec(command);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line = reader.readLine();
//            return Double.parseDouble(line.trim());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }
//
private double getMemoryUsage() {
    try {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                com.sun.management.OperatingSystemMXBean.class);

        // 获取总物理内存和空闲内存
        long totalMemory = osBean.getTotalPhysicalMemorySize();
        long freeMemory = osBean.getFreePhysicalMemorySize();

        if (totalMemory > 0) {
            // 计算使用率
            return (double) (totalMemory - freeMemory) / totalMemory * 100.0;
        }

        return 0.0;
    } catch (Exception e) {
        e.printStackTrace();
        return 0.0;
    }
}

//    private double getStorageUsage() {
//        try {
//            String command = "df / | grep / | awk '{ print $5 }' | sed 's/%//g'";
//            Process process = Runtime.getRuntime().exec(command);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line = reader.readLine();
//            return Double.parseDouble(line.trim());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return 0;
//        }
//    }

    private double getStorageUsage() {
        try {
            File root = new File("/");

            // 获取根目录（"/"）的总空间和可用空间
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();

            if (totalSpace > 0) {
                // 计算使用率
                return (double) (totalSpace - usableSpace) / totalSpace * 100.0;
            }

            return 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }
}

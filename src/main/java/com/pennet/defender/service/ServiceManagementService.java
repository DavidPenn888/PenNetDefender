package com.pennet.defender.service;

import com.pennet.defender.model.AppService;
import com.pennet.defender.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceManagementService {

    @Autowired
    private ServiceRepository serviceRepository;

    public Page<AppService> getServices(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return serviceRepository.findAllByOrderByNameAsc(pageable);
    }
    
    public Page<AppService> searchServicesByName(String name, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return serviceRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public void refreshServices() throws IOException {
        List<AppService> appServices = new ArrayList<>();

        // 获取所有服务列表
        Process listProcess = Runtime.getRuntime().exec("systemctl list-units --type=service --output=json");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(listProcess.getInputStream()))) {
            // 这里假设输出为JSON格式，需要添加JSON解析库如Jackson
            // 实际上你可能需要使用systemctl -o json或类似的参数

            // 或者对每个服务单独使用systemctl show来获取详细信息
            // 下面是一个混合方法示例，先获取服务列表，再检查每个服务详情
        }

        // 获取所有服务名称
        Process listNamesProcess = Runtime.getRuntime().exec("systemctl list-units --type=service --plain --no-legend");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(listNamesProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // 提取第一列作为服务名
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length > 0) {
                    String serviceName = parts[0].replace(".service", "");

                    // 获取服务状态
                    String status = getServiceStatus(serviceName);
                    boolean isEnabled = checkIfServiceEnabled(serviceName);

                    AppService existingAppService = serviceRepository.findByName(serviceName);
                    if (existingAppService != null) {
                        existingAppService.setStatus(status);
                        existingAppService.setEnabled(isEnabled);
                        existingAppService.setLastUpdated(LocalDateTime.now());
                        appServices.add(existingAppService);
                    } else {
                        AppService newAppService = new AppService();
                        newAppService.setName(serviceName);
                        newAppService.setStatus(status);
                        newAppService.setEnabled(isEnabled);
                        newAppService.setLastUpdated(LocalDateTime.now());
                        appServices.add(newAppService);
                    }
                }
            }
        }

        serviceRepository.saveAll(appServices);
    }

    private String getServiceStatus(String serviceName) {
        try {
            Process statusCheck = Runtime.getRuntime().exec("systemctl is-active " + serviceName);
            try (BufferedReader statusReader = new BufferedReader(new InputStreamReader(statusCheck.getInputStream()))) {
                return statusReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "unknown";
        }
    }
    private boolean checkIfServiceEnabled(String serviceName) {
        try {
            Process enabledCheck = Runtime.getRuntime().exec("systemctl is-enabled " + serviceName);
            try (BufferedReader enabledReader = new BufferedReader(new InputStreamReader(enabledCheck.getInputStream()))) {
                String enabledStatus = enabledReader.readLine();
                return "enabled".equals(enabledStatus);
            }
        } catch (IOException e) {
            // 如果检查失败，记录日志并假设不是enabled状态
            e.printStackTrace();
            return false;
        }
    }

    public void startService(String serviceName) throws IOException {
        executeCommand("systemctl start " + serviceName);
        updateServiceStatus(serviceName);
    }

    public void stopService(String serviceName) throws IOException {
        executeCommand("systemctl stop " + serviceName);
        updateServiceStatus(serviceName);
    }

    public void restartService(String serviceName) throws IOException {
        executeCommand("systemctl restart " + serviceName);
        updateServiceStatus(serviceName);
    }

    public void enableService(String serviceName) throws IOException {
        executeCommand("systemctl enable " + serviceName);
        updateServiceStatus(serviceName);
    }

    public void disableService(String serviceName) throws IOException {
        executeCommand("systemctl disable " + serviceName);
        updateServiceStatus(serviceName);
    }

    private void updateServiceStatus(String serviceName) throws IOException {
        AppService AppService = serviceRepository.findByName(serviceName);
        if (AppService == null) {
            AppService = new AppService();
            AppService.setName(serviceName);
        }

        // Get status
        Process statusCheck = Runtime.getRuntime().exec("systemctl is-active " + serviceName);
        BufferedReader statusReader = new BufferedReader(new InputStreamReader(statusCheck.getInputStream()));
        String status = statusReader.readLine();

        // Get enabled status
        Process enabledCheck = Runtime.getRuntime().exec("systemctl is-enabled " + serviceName);
        BufferedReader enabledReader = new BufferedReader(new InputStreamReader(enabledCheck.getInputStream()));
        String enabledStatus = enabledReader.readLine();
        boolean isEnabled = "enabled".equals(enabledStatus);

        AppService.setStatus(status);
        AppService.setEnabled(isEnabled);
        AppService.setLastUpdated(LocalDateTime.now());
        serviceRepository.save(AppService);
    }

    private void executeCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(command);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

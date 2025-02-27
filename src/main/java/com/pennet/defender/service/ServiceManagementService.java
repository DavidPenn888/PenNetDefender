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

    public void refreshServices() throws IOException {
        List<AppService> AppServices = new ArrayList<>();
        Process process = Runtime.getRuntime().exec("systemctl list-units --type=service --all");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    if (line.trim().isEmpty()) {
                        skipHeader = false;
                    }
                    continue;
                }

                if (line.trim().isEmpty() || line.contains("LOAD") || line.contains("UNIT")) {
                    continue;
                }

                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    String serviceName = parts[0].replace(".service", "");
                    String status = parts[3];

                    // Get enabled status
                    Process enabledCheck = Runtime.getRuntime().exec("systemctl is-enabled " + serviceName);
                    BufferedReader enabledReader = new BufferedReader(new InputStreamReader(enabledCheck.getInputStream()));
                    String enabledStatus = enabledReader.readLine();
                    boolean isEnabled = "enabled".equals(enabledStatus);

                    AppService existingAppService = serviceRepository.findByName(serviceName);
                    if (existingAppService != null) {
                        existingAppService.setStatus(status);
                        existingAppService.setEnabled(isEnabled);
                        existingAppService.setLastUpdated(LocalDateTime.now());
                        AppServices.add(existingAppService);
                    } else {
                        AppService newAppService = new AppService();
                        newAppService.setName(serviceName);
                        newAppService.setStatus(status);
                        newAppService.setEnabled(isEnabled);
                        newAppService.setLastUpdated(LocalDateTime.now());
                        AppServices.add(newAppService);
                    }
                }
            }
        }

        serviceRepository.saveAll(AppServices);
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

package com.pennet.defender.service;

import com.pennet.defender.model.Port;
import com.pennet.defender.repository.PortRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PortQueryService {

    @Autowired
    private PortRepository portRepository;

    public Page<Port> getPorts(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return portRepository.findAllByOrderByProcessNameAsc(pageable);
    }

    public void refreshPorts() throws IOException {
        // Clear existing ports
        portRepository.deleteAll();

        List<Port> ports = new ArrayList<>();

        // Use ss command to get open ports with process info
//        Process process = Runtime.getRuntime().exec("ss -tulpn");
        Process process = Runtime.getRuntime().exec("netstat -tulnp");

//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//            String line;
//            boolean skipHeader = true;
//
//            while ((line = reader.readLine()) != null) {
//                if (skipHeader) {
//                    skipHeader = false;
//                    continue;
//                }
//
//                // Parse line to extract port information
//                Pattern protocolPattern = Pattern.compile("^(\\w+)\\s+.*");
//                Pattern portPattern = Pattern.compile(".*:(\\d+)\\s+.*");
//                Pattern processPattern = Pattern.compile(".*pid=(\\d+),name=([^,)]+).*");
//                Pattern statePattern = Pattern.compile(".*\\s(LISTEN|ESTABLISHED|TIME-WAIT|CLOSE-WAIT)\\s.*");
//
//                Matcher protocolMatcher = protocolPattern.matcher(line);
//                Matcher portMatcher = portPattern.matcher(line);
//                Matcher processMatcher = processPattern.matcher(line);
//                Matcher stateMatcher = statePattern.matcher(line);
//
//                if (portMatcher.find()) {
//                    Port port = new Port();
//                    port.setPortNumber(Integer.parseInt(portMatcher.group(1)));
//
//                    if (protocolMatcher.find()) {
//                        port.setProtocol(protocolMatcher.group(1));
//                    }
//
//                    if (processMatcher.find()) {
//                        port.setProcessId(Integer.parseInt(processMatcher.group(1)));
//                        port.setProcessName(processMatcher.group(2));
//                    }
//
//                    if (stateMatcher.find()) {
//                        port.setState(stateMatcher.group(1));
//                    } else {
//                        port.setState("UNKNOWN");
//                    }
//
//                    port.setLastUpdated(LocalDateTime.now());
//                    ports.add(port);
//                }
//            }
//        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                // Adjusted regular expressions for netstat output
                Pattern protocolPattern = Pattern.compile("^(\\w+)\\s+.*");
                Pattern portPattern = Pattern.compile(".*:(\\d+)\\s+.*");
                Pattern processPattern = Pattern.compile(".*\\s(\\d+)/([^\\s]+).*");  // This is for extracting pid and process name
                Pattern statePattern = Pattern.compile(".*\\s(LISTEN|ESTABLISHED|TIME-WAIT|CLOSE-WAIT)\\s.*");

                Matcher protocolMatcher = protocolPattern.matcher(line);
                Matcher portMatcher = portPattern.matcher(line);
                Matcher processMatcher = processPattern.matcher(line);
                Matcher stateMatcher = statePattern.matcher(line);

                if (portMatcher.find()) {
                    Port port = new Port();
                    port.setPortNumber(Integer.parseInt(portMatcher.group(1)));

                    if (protocolMatcher.find()) {
                        port.setProtocol(protocolMatcher.group(1));
                    }

                    if (processMatcher.find()) {
                        port.setProcessId(Integer.parseInt(processMatcher.group(1)));
                        port.setProcessName(processMatcher.group(2));
                    }

                    if (stateMatcher.find()) {
                        port.setState(stateMatcher.group(1));
                    } else {
                        port.setState("UNKNOWN");
                    }

                    port.setLastUpdated(LocalDateTime.now());
                    ports.add(port);
                }
            }
        }

        portRepository.saveAll(ports);
    }

    // Kill process normally (using kill)
    public String killProcess(int pid) {
        try {
            String command = "kill " + pid;
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();  // Wait for the command to finish
            return "Process with PID " + pid + " has been terminated successfully.";
        } catch (IOException | InterruptedException e) {
            return "Error terminating process with PID " + pid + ": " + e.getMessage();
        }
    }

    // Force kill process (using kill -9)
    public String forceKillProcess(int pid) {
        try {
            String command = "kill -9 " + pid;
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();  // Wait for the command to finish
            return "Process with PID " + pid + " has been forcefully terminated.";
        } catch (IOException | InterruptedException e) {
            return "Error forcefully terminating process with PID " + pid + ": " + e.getMessage();
        }
    }

}

package com.pennet.defender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThresholdConfig {

    @Value("${threshold.cpu}")
    private int cpuThreshold;

    @Value("${threshold.memory}")
    private int memoryThreshold;

    @Value("${threshold.storage}")
    private int storageThreshold;

    public int getCpuThreshold() {
        return cpuThreshold;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public int getStorageThreshold() {
        return storageThreshold;
    }

    public void setCpuThreshold(int cpuThreshold) {
        this.cpuThreshold = cpuThreshold;
    }

    public void setMemoryThreshold(int memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }

    public void setStorageThreshold(int storageThreshold) {
        this.storageThreshold = storageThreshold;
    }
}
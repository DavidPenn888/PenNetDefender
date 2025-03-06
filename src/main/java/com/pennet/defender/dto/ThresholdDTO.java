package com.pennet.defender.dto;

public class ThresholdDTO {
    private int cpuThreshold;
    private int memoryThreshold;
    private int storageThreshold;

    public ThresholdDTO(int cpuThreshold, int memoryThreshold, int storageThreshold) {
        this.cpuThreshold = cpuThreshold;
        this.memoryThreshold = memoryThreshold;
        this.storageThreshold = storageThreshold;
    }

    public int getCpuThreshold() {
        return cpuThreshold;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public int getStorageThreshold() {
        return storageThreshold;
    }
}

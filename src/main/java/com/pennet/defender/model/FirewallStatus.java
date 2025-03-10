package com.pennet.defender.model;

public class FirewallStatus {
    private boolean enabled;
    private int totalRules;
    private int inputRules;
    private int outputRules;
    private int forwardRules;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTotalRules() {
        return totalRules;
    }

    public void setTotalRules(int totalRules) {
        this.totalRules = totalRules;
    }

    public int getInputRules() {
        return inputRules;
    }

    public void setInputRules(int inputRules) {
        this.inputRules = inputRules;
    }

    public int getOutputRules() {
        return outputRules;
    }

    public void setOutputRules(int outputRules) {
        this.outputRules = outputRules;
    }

    public int getForwardRules() {
        return forwardRules;
    }

    public void setForwardRules(int forwardRules) {
        this.forwardRules = forwardRules;
    }
}
package com.pennet.defender.model;

public class FirewallStatus {
    private boolean firewallActive;
    private boolean iptablesActive;

    public FirewallStatus() {}

    public FirewallStatus(boolean firewallActive, boolean iptablesActive) {
        this.firewallActive = firewallActive;
        this.iptablesActive = iptablesActive;
    }

    public boolean isFirewallActive() {
        return firewallActive;
    }

    public void setFirewallActive(boolean firewallActive) {
        this.firewallActive = firewallActive;
    }

    public boolean isIptablesActive() {
        return iptablesActive;
    }

    public void setIptablesActive(boolean iptablesActive) {
        this.iptablesActive = iptablesActive;
    }
}
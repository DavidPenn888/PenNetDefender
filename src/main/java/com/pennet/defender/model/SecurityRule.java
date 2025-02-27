package com.pennet.defender.model;

import java.util.regex.Pattern;

public class SecurityRule {
    private String ruleId;
    private String alertType;
    private String pattern;
    private Pattern compiledPattern;
    private String description;

    public SecurityRule() {
    }

    public SecurityRule(String ruleId, String alertType, String pattern, String description) {
        this.ruleId = ruleId;
        this.alertType = alertType;
        this.pattern = pattern;
        this.description = description;
        this.compiledPattern = Pattern.compile(pattern);
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiledPattern = Pattern.compile(pattern);
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean matches(String input) {
        return compiledPattern.matcher(input).find();
    }
}
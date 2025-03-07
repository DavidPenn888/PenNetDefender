package com.pennet.defender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebHookConfig {

    @Value("${alert.webhook.wechat.enable}")
    private boolean wechatEnable;

    @Value("${alert.webhook.wechat.url}")
    private String wechatWebHook;

    @Value("${alert.webhook.dingding.enable}")
    private boolean dingdingEnable;

    @Value("${alert.webhook.dingding.url}")
    private String dingdingWebhook;

    public boolean isWechatEnable() {
        return wechatEnable;
    }

    public void setWechatEnable(boolean wechatEnable) {
        this.wechatEnable = wechatEnable;
    }

    public boolean isDingdingEnable() {
        return dingdingEnable;
    }

    public void setDingdingEnable(boolean dingdingEnable) {
        this.dingdingEnable = dingdingEnable;
    }

    public String getWechatWebHook() {
        return wechatWebHook;
    }

    public void setWechatWebHook(String wechatWebHook) {
        this.wechatWebHook = wechatWebHook;
    }

    public String getDingdingWebhook() {
        return dingdingWebhook;
    }

    public void setDingdingWebhook(String dingdingWebhook) {
        this.dingdingWebhook = dingdingWebhook;
    }

    public void updateWebHookConfig(boolean wechatEnable, boolean dingdingEnable, String wechatWebHook, String dingdingWebhook) {
        this.wechatEnable = wechatEnable;
        this.dingdingEnable = dingdingEnable;
        this.wechatWebHook = wechatWebHook;
        this.dingdingWebhook = dingdingWebhook;
    }
}

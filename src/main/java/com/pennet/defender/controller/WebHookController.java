package com.pennet.defender.controller;

import com.pennet.defender.config.WebHookConfig;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/server")
public class WebHookController {

    private WebHookConfig webHookConfig;

    public WebHookController(WebHookConfig webHookConfig) {
        this.webHookConfig = webHookConfig;
    }

    // TODO 本方法是否永久保存？包括重启项目等方式是否还会保存，同时注意：同样存储的还有告警阈值等逻辑
    @PostMapping("/change_webhook")
    public void changeWebHook(@RequestBody WebHookConfig newWebHookConfig) {
        webHookConfig.setWechatEnable(newWebHookConfig.isWechatEnable());
        webHookConfig.setDingdingEnable(newWebHookConfig.isDingdingEnable());
        webHookConfig.setWechatWebHook(newWebHookConfig.getWechatWebHook());
        webHookConfig.setDingdingWebhook(newWebHookConfig.getDingdingWebhook());
    }

}

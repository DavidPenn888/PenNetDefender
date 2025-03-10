package com.pennet.defender.controller;

import com.pennet.defender.config.WebHookConfig;
import com.pennet.defender.model.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/server")
public class WebHookController {

    private WebHookConfig webHookConfig;

    public WebHookController(WebHookConfig webHookConfig) {
        this.webHookConfig = webHookConfig;
    }

//    @GetMapping("/webhook")
//    public WebHookConfig getWebHookConfig() {
//        return webHookConfig;
//    }


//    @PostMapping("/change_webhook")
//    public void changeWebHook(@RequestBody WebHookConfig newWebHookConfig) {
//        webHookConfig.setWechatEnable(newWebHookConfig.isWechatEnable());
//        webHookConfig.setWechatWebHook(newWebHookConfig.getWechatWebHook());
//        webHookConfig.setDingdingEnable(newWebHookConfig.isDingdingEnable());
//        webHookConfig.setDingdingWebhook(newWebHookConfig.getDingdingWebhook());
//    }

    @GetMapping("/webhook")
    public ApiResponse<WebHookConfig> getWebHookConfig() {
        return new ApiResponse<>(0, webHookConfig, "success");
    }

    // TODO 本方法是否永久保存？包括重启项目等方式是否还会保存，同时注意：同样存储的还有告警阈值等逻辑
    @PostMapping("/change_webhook")
    public ApiResponse<Void> changeWebHook(@RequestBody WebHookConfig newWebHookConfig) {
        webHookConfig.setWechatEnable(newWebHookConfig.isWechatEnable());
        webHookConfig.setWechatWebHook(newWebHookConfig.getWechatWebHook());
        webHookConfig.setDingdingEnable(newWebHookConfig.isDingdingEnable());
        webHookConfig.setDingdingWebhook(newWebHookConfig.getDingdingWebhook());

        return new ApiResponse<>(0, null, "Webhook configuration updated successfully");
    }

}

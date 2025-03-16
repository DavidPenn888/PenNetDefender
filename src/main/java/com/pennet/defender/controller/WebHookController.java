package com.pennet.defender.controller;

import com.pennet.defender.config.WebHookConfig;
import com.pennet.defender.model.ApiResponse;
import com.pennet.defender.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/server")
public class WebHookController {

    private WebHookConfig webHookConfig;
    
    @Autowired
    private SystemConfigService systemConfigService;

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

    @PostMapping("/change_webhook")
    public ApiResponse<Void> changeWebHook(@RequestBody WebHookConfig newWebHookConfig) {
        // 使用SystemConfigService更新WebHook配置，确保持久化保存
        systemConfigService.updateWebHookConfig(
            newWebHookConfig.isWechatEnable(),
            newWebHookConfig.getWechatWebHook(),
            newWebHookConfig.isDingdingEnable(),
            newWebHookConfig.getDingdingWebhook()
        );

        return new ApiResponse<>(0, null, "Webhook configuration updated successfully and persisted");
    }

}

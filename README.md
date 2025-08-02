关于本项目：
用于监控系统健康状态、SSH日志自动审计规则匹配、代理流量进行简单的规则匹配，推送企业微信、钉钉告警，多功能为一体的Linux网络安全监控系统。

环境要求：
1、使用Java11、Spring Boot2.7、Maven（其他版本未测试）；
2、安装docker，拉取mysql8并运行（或使用其他方式对接数据库）；
3、Ubuntu 22（其他系统自测）；
4、Ubuntu 安装 auditd、mitmproxy 两个插件；
5、根目录/app下运行本程序，服务端口8080，代理端口8082

本项目已计划终止，不再更新。

Regarding this project: It is a multifunctional Linux network security monitoring system that monitors system health status, performs automatic audit rule matching for SSH logs, conducts simple rule matching for proxy traffic, and pushes alerts to WeChat Work and DingTalk.

Environment requirements: 1. Use Java 11, Spring Boot 2.7, and Maven (other versions have not been tested); 2. Install Docker, pull MySQL 8 and run it (or use other methods to connect to the database); 3. Ubuntu 22 (self-tested on other systems); 4. Install the auditd and mitmproxy plugins on Ubuntu; 5. Run this program in the root directory/app, with the service port at 8080 and the proxy port at 8082

This project has been planned to be terminated and will not be updated anymore.

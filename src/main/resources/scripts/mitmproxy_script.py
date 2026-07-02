#!/usr/bin/env python3

import json
import re
import requests
from mitmproxy import http
from datetime import datetime

# 配置项
API_ENDPOINT = "http://localhost:58080/api/security/http_alert"

# 从http_rules.json加载的规则
rules = [
    {
        "ruleId": "http-001",
        "alertType": "SQL注入",
        "pattern": r"(?i)(select|union|insert|update|delete|drop).*from",
        "description": "可能的SQL注入尝试"
    },
    {
        "ruleId": "http-002",
        "alertType": "XSS攻击",
        "pattern": r"(?i)(<script>|javascript:|onerror=|onload=)",
        "description": "可能的跨站脚本攻击尝试"
    },
    {
        "ruleId": "http-003",
        "alertType": "路径遍历",
        "pattern": r"(?i)(\.\./|\.\./|\.\./)",
        "description": "可能的路径遍历攻击尝试"
    },
    {
        "ruleId": "http-004",
        "alertType": "敏感信息泄露",
        "pattern": r"(?i)(password=|token=|api_key=|secret=)",
        "description": "敏感信息可能通过HTTP明文传输"
    },
    {
        "ruleId": "http-005",
        "alertType": "命令注入",
        "pattern": r"(?i)(;|&&|\|\||`|\$\()",
        "description": "可能的命令注入尝试"
    }
]

# 编译正则表达式
for rule in rules:
    rule['compiled_pattern'] = re.compile(rule['pattern'])

def extract_ip_info(flow):
    """从HTTP流量中提取IP信息"""
    return flow.client_conn.address[0]

def extract_user_info(flow):
    """尝试从HTTP流量中提取用户信息"""
    # 尝试从Cookie中提取用户信息
    if 'Cookie' in flow.request.headers:
        cookie = flow.request.headers['Cookie']
        user_match = re.search(r'user=([^;]+)', cookie)
        if user_match:
            return user_match.group(1)
    
    # 尝试从Authorization头中提取
    if 'Authorization' in flow.request.headers:
        return "Auth_User"
    
    return None

def check_request(flow):
    """检查请求是否匹配任何安全规则"""
    # 构建要检查的字符串，包括URL、查询参数和请求体
    url = flow.request.url
    method = flow.request.method
    headers = str(flow.request.headers)
    
    # 获取请求体内容（如果有）
    content = ""
    if flow.request.content:
        try:
            content = flow.request.content.decode('utf-8')
        except UnicodeDecodeError:
            content = str(flow.request.content)
    
    # 组合所有内容进行检查
    check_text = f"{method} {url} {headers} {content}"
    
    for rule in rules:
        if rule['compiled_pattern'].search(check_text):
            return rule
    
    return None

def send_alert(rule, flow):
    """将告警发送到Java应用程序"""
    try:
        ip_info = extract_ip_info(flow)
        user_info = extract_user_info(flow)
        
        # 构建告警详情
        detail_info = f"检测到HTTP安全事件: {rule['description']} - {flow.request.method} {flow.request.url}"
        
        # 构建告警数据
        alert_data = {
            "timestamp": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "detectWay": 2,  # 2表示HTTP检测
            "alertType": rule['alertType'],
            "userInfo": user_info,
            "ipInfo": ip_info,
            "detailInfo": detail_info[:250]  # 确保不超过255个字符
        }
        
        # 发送告警到Java应用
        response = requests.post(API_ENDPOINT, json=alert_data)
        print(f"告警已发送: {rule['alertType']} - 状态码: {response.status_code}")
    except Exception as e:
        print(f"发送告警失败: {str(e)}")

def request(flow):
    """处理每个HTTP请求"""
    # 跳过对Java应用自身API的请求，避免无限循环
    if "localhost:8080/api" in flow.request.url or API_ENDPOINT in flow.request.url:
        return
    
    # 检查请求是否匹配任何安全规则
    matched_rule = check_request(flow)
    if matched_rule:
        print(f"检测到可能的安全威胁: {matched_rule['alertType']} - {flow.request.url}")
        send_alert(matched_rule, flow)
    else:
        print(f"请求正常: {flow.request.method} {flow.request.url}")
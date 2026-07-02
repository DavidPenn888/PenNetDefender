#!/usr/bin/env python3

import json
import re
import requests
import warnings
from mitmproxy import http
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor

warnings.filterwarnings("ignore", message="Unverified HTTPS request")

# ================= 1. 配置项 =================
API_ENDPOINT = "https://localhost:58080/api/security/http_alert"

# 核心配置 1：排除列表（黑名单），避免“监控套娃”和不必要的检测
# 包含：自身API、钉钉、企业微信、飞书等常见 Webhook 域名
IGNORE_DOMAINS = [
    "localhost:58080",       # Java 后端自身 API（避免死循环）
    "oapi.dingtalk.com",     # 钉钉机器人
    "qyapi.weixin.qq.com",   # 企业微信机器人
    "open.feishu.cn",        # 飞书机器人
    # 如果你还有其他第三方通知渠道，可以继续在这里添加
]

# ================= 2. 初始化 =================
# 使用 Session 复用连接，提升性能
session = requests.Session()
session.verify = False

#  核心配置 2：使用全局线程池，防止高并发攻击下线程爆炸
# max_workers=20 表示最多同时有 20 个线程在发送告警，超出的会排队
alert_executor = ThreadPoolExecutor(max_workers=20, thread_name_prefix="alert_sender")

# ================= 3. 规则定义 =================
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
        "pattern": r"(?i)(\.\./)",  # ✅ 修复：去掉了原本冗余重复的 \.\./
        "description": "可能的路径遍历攻击尝试"
    },
    {
        "ruleId": "http-004",
        "alertType": "敏感信息泄露",
        "pattern": r"(?i)(password=|api_key=|secret=)",
        "description": "敏感信息可能通过HTTP明文传输"
    },
    {
        "ruleId": "http-005",
        "alertType": "命令注入",
        "pattern": r"(?i)(;|&&|\|\||`|\$\()",
        "description": "可能的命令注入尝试"
    }
]

for rule in rules:
    rule['compiled_pattern'] = re.compile(rule['pattern'])


# ================= 4. 辅助函数 =================
def extract_ip_info(flow):
    return flow.client_conn.address[0]

def extract_user_info(flow):
    if 'Cookie' in flow.request.headers:
        cookie = flow.request.headers['Cookie']
        user_match = re.search(r'user=([^;]+)', cookie)
        if user_match:
            return user_match.group(1)
    if 'Authorization' in flow.request.headers:
        return "Auth_User"
    return None

def check_request(flow):
    url = flow.request.url
    method = flow.request.method
    headers = str(flow.request.headers)

    content = ""
    if flow.request.content:
        try:
            content = flow.request.content.decode('utf-8')
        except UnicodeDecodeError:
            content = str(flow.request.content)

    check_text = f"{method} {url} {headers} {content}"

    for rule in rules:
        if rule['compiled_pattern'].search(check_text):
            return rule
    return None


# ================= 5. 告警发送逻辑 =================
def _do_send_alert(rule, flow, ip_info, user_info):
    """实际发送告警的函数（在线程池中执行）"""
    try:
        detail_info = f"检测到HTTP安全事件: {rule['description']} - {flow.request.method} {flow.request.url}"

        alert_data = {
            "timestamp": datetime.now().strftime("%Y-%m-%dT%H:%M:%S"),
            "detectWay": 2,
            "alertType": rule['alertType'],
            "userInfo": user_info,
            "ipInfo": ip_info,
            "detailInfo": detail_info[:250]
        }

        # 关键修复1：添加 timeout，防止无限等待
        # 关键修复2：使用 session 复用连接
        response = session.post(
            API_ENDPOINT,
            json=alert_data,
            timeout=(3, 10)  # 连接超时3秒，读取超时10秒
        )
        print(f"[成功] 告警已发送: {rule['alertType']} - 状态码: {response.status_code}")
    except requests.exceptions.Timeout:
        print(f"[超时] 告警发送超时: {rule['alertType']}")
    except requests.exceptions.ConnectionError:
        print(f"[连接失败] 告警发送连接失败: {rule['alertType']} - 请检查Java服务是否运行")
    except Exception as e:
        print(f"[异常] 发送告警失败: {str(e)}")


def send_alert(rule, flow):
    """关键修复3：提交到线程池，不阻塞 mitmproxy 主事件循环"""
    ip_info = extract_ip_info(flow)
    user_info = extract_user_info(flow)

    #  使用线程池提交任务，替代每次 new Thread()
    alert_executor.submit(_do_send_alert, rule, flow, ip_info, user_info)


# ================= 6. mitmproxy 核心钩子 =================
def request(flow: http.HTTPFlow):
    url = flow.request.url

    #  核心修复 4：使用排除列表（黑名单）过滤流量，解决“监控套娃”问题
    for ignore_domain in IGNORE_DOMAINS:
        if ignore_domain in url:
            return  # 命中黑名单，直接放行，不进行任何规则检测

    # 检查请求是否匹配任何安全规则
    matched_rule = check_request(flow)
    if matched_rule:
        print(f"[告警] 检测到可能的安全威胁: {matched_rule['alertType']} - {url}")
        send_alert(matched_rule, flow)
    else:
        # 💡 优化建议：注释掉正常请求的打印。
        # 在高并发场景下，打印海量“请求正常”会导致控制台 I/O 阻塞，拖慢 mitmproxy 性能。
        # print(f"请求正常: {flow.request.method} {url}")
        pass
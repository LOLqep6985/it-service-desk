package com.itservice.desk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企业微信群机器人通知（_notify.wecom.webhook_ 配置驱动，留空 = 功能关闭）。
 *
 * 使用方式（医院内网部署自助接入，无需企微管理员权限）：
 *   1. 在维修班企业微信群里：群设置 → 群机器人 → 添加机器人 → 复制 Webhook 地址
 *   2. 把地址填到 application.yml 的 notify.wecom.webhook（或环境变量 NOTIFY_WECOM_WEBHOOK）
 *   3. 重启后端即可。管理员派单后，群里会收到新工单提醒（含今日任务页深链）
 *
 * 设计约束：
 *  - 异步发送（派单主流程不被通知阻塞），失败只记日志不抛错
 *  - webhook 地址做格式校验，防误配成内网其他地址
 */
@Service
@ConditionalOnProperty(name = "notify.wecom.webhook")
public class WecomWebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WecomWebhookNotifier.class);
    /** 企微群机器人 webhook 格式：https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx */
    private static final Pattern WECOM_HOOK = Pattern.compile("^https://qyapi\\.weixin\\.qq\\.com/cgi-bin/webhook/send\\?key=[A-Za-z0-9\\-]+$");

    private final String webhook;
    private final String publicHost;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public WecomWebhookNotifier(@Value("${notify.wecom.webhook}") String webhook,
                                @Value("${notify.public-host:}") String publicHost) {
        this.webhook = webhook;
        this.publicHost = publicHost == null ? "" : publicHost.trim();
    }

    public boolean enabled() {
        return webhook != null && WECOM_HOOK.matcher(webhook).matches();
    }

    /**
     * 派单通知：@维修工（群内可见），消息含工单摘要 + 今日任务页链接。
     * 异步执行，任何异常吞掉只记 WARN —— 通知是旁路，不能影响派单。
     */
    public void notifyDispatch(String assigneeName, String workOrderTitle,
                               String departmentName, String campusLabel, Long workOrderId) {
        if (!enabled()) {
            return;
        }
        String link = todayLink();
        // 企微 markdown：手机上点链接直接进 today.html（PWA 已安装则全屏打开）
        String md = "**新工单派单通知**\n" +
                "> 工单：**#" + workOrderId + " " + esc(workOrderTitle) + "**\n" +
                "> 科室：" + esc(departmentName) + "（" + esc(campusLabel) + "）\n" +
                "> 维修工：" + esc(assigneeName) + "\n" +
                "> 请尽快[打开今日任务接单](" + link + ")";
        send(md);
    }

    /** 对外访问地址：notify.public-host（如 http://192.168.1.100:8080）；未配置时返回空链接占位 */
    private String todayLink() {
        if (publicHost.isEmpty()) {
            return "http://请在application.yml配置notify.public-host/";
        }
        return publicHost.endsWith("/") ? publicHost + "today.html" : publicHost + "/today.html";
    }

    private void send(String markdown) {
        // 异步：独立线程，失败只记日志（不阻塞派单主流程）
        Thread t = new Thread(() -> {
            String err = sendSync(markdown);
            if (err != null) {
                log.warn("企微通知发送失败（派单不受影响）：{}", err);
            }
        }, "wecom-notify");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 管理端「发送测试通知」：同步发送并回报结果，便于自助验证 webhook 是否配通。
     * @return null 表示发送成功；否则返回失败原因（直接展示给管理员）
     */
    public String sendTest() {
        if (!enabled()) {
            return "未配置 notify.wecom.webhook，或地址不是企业微信群机器人 webhook（需以 https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key= 开头）";
        }
        return sendSync("**通知测试**\n> 在群里看到这条消息，说明派单通知已接通。\n> 之后管理员每次派单，本群都会收到新工单提醒。");
    }

    /** 同步发送：返回 null=成功，否则为错误说明 */
    private String sendSync(String markdown) {
        try {
            String body = "{\"msgtype\":\"markdown\",\"markdown\":{\"content\":" + jsonString(markdown) + "}}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(webhook))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String respBody = res.body() == null ? "" : res.body();
            if (res.statusCode() != 200) {
                return "HTTP " + res.statusCode() + "：" + respBody;
            }
            if (!respBody.contains("\"errcode\":0")) {
                return "企微返回：" + respBody;
            }
            return null;
        } catch (IOException | InterruptedException e) {
            return "网络错误：" + e.getMessage();
        }
    }

    /** 极简 JSON 字符串转义（消息体只有文本与换行，无需引全 JSON 库） */
    private String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

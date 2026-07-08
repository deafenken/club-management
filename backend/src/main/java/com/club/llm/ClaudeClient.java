package com.club.llm;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * LLM API 调用客户端 — 支持 DeepSeek / Anthropic Claude 双 provider
 *
 * DeepSeek 使用 OpenAI 兼容格式：
 *   URL:  https://api.deepseek.com/v1/chat/completions
 *   Auth: Authorization: Bearer <key>
 *   格式: { "model":"deepseek-chat", "messages":[{role:"system",content:...},{role:"user",content:...}] }
 *
 * Anthropic 使用原生 Messages API：
 *   URL:  https://api.anthropic.com/v1/messages
 *   Auth: x-api-key: <key>
 *   格式: { "model":"claude-...", "system":"...", "messages":[{role:"user",content:"..."}] }
 */
@Slf4j
@Component
public class ClaudeClient {

    @Value("${llm.provider:deepseek}")
    private String provider;

    @Value("${llm.api.key:#{null}}")
    private String apiKey;

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.model}")
    private String model;

    @Value("${llm.api.timeout}")
    private int timeout;

    @Value("${llm.api.max-retries}")
    private int maxRetries;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /** 兜底从 .env 文件读取 API Key（裸机部署没有环境变量时用） */
    private String resolveApiKey() {
        if (apiKey != null && !apiKey.isBlank() && !"your-api-key-here".equals(apiKey)) {
            return apiKey;
        }
        // Spring @Value 没取到，尝试从系统环境变量读
        String envKey = System.getenv("LLM_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey;
        envKey = System.getenv("DEEPSEEK_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey;
        // 最后尝试从 .env 文件读取
        try {
            java.nio.file.Path envFile = java.nio.file.Path.of("/opt/club-management/.env");
            if (!java.nio.file.Files.exists(envFile)) {
                envFile = java.nio.file.Path.of(".env");
            }
            if (java.nio.file.Files.exists(envFile)) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(envFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String k = line.substring(0, eq).trim();
                            String v = line.substring(eq + 1).trim();
                            if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                            if ("LLM_API_KEY".equals(k) || "DEEPSEEK_API_KEY".equals(k)) return v;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取 .env 文件失败: {}", e.getMessage());
        }
        return apiKey; // 返回原始值（可能是null）
    }

    /**
     * 发送消息到 LLM API（自动适配 provider）
     * @param systemPrompt 系统提示词（定义Agent角色）
     * @param userMessage  用户消息
     * @return LLM返回的文本内容；失败返回null
     */
    public String sendMessage(String systemPrompt, String userMessage) {
        String key = resolveApiKey();
        if ("your-api-key-here".equals(key) || key == null || key.isBlank()) {
            log.error("LLM API Key 未配置！请设置环境变量 LLM_API_KEY 或在 /opt/club-management/.env 中配置");
            return null;
        }

        Request request = "anthropic".equalsIgnoreCase(provider)
                ? buildAnthropicRequest(systemPrompt, userMessage, key)
                : buildDeepSeekRequest(systemPrompt, userMessage, key);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                long start = System.currentTimeMillis();
                Response response = client.newCall(request).execute();
                long latency = System.currentTimeMillis() - start;

                if (response.isSuccessful() && response.body() != null) {
                    String respBody = response.body().string();
                    String text = "anthropic".equalsIgnoreCase(provider)
                            ? parseAnthropicResponse(respBody)
                            : parseOpenAIResponse(respBody);

                    if (text != null) {
                        log.info("{} API调用成功, model={}, latency={}ms",
                                provider, model, latency);
                        return text;
                    }
                } else {
                    String errBody = response.body() != null ? response.body().string() : "";
                    log.warn("{} API返回异常(第{}次): status={}, body={}",
                            provider, attempt, response.code(), StrUtil.sub(errBody, 0, 300));
                }
            } catch (IOException e) {
                log.warn("{} API网络异常(第{}次): {}", provider, attempt, e.getMessage());
            }

            if (attempt < maxRetries) {
                try { Thread.sleep(2000L * attempt); } catch (InterruptedException ignored) {}
            }
        }

        log.error("{} API调用最终失败，已重试{}次", provider, maxRetries);
        return null;
    }

    // ==================== DeepSeek (OpenAI 兼容格式) ====================

    private Request buildDeepSeekRequest(String systemPrompt, String userMessage, String key) {
        JSONArray messages = new JSONArray();
        messages.add(JSONObject.of("role", "system", "content", systemPrompt));
        messages.add(JSONObject.of("role", "user", "content", userMessage));

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("messages", messages);

        return new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + key)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();
    }

    // ==================== Anthropic Claude 原生格式 ====================

    private Request buildAnthropicRequest(String systemPrompt, String userMessage, String key) {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 4096);
        body.put("system", systemPrompt);
        body.put("messages", JSONArray.of(
            JSONObject.of("role", "user", "content", userMessage)
        ));

        return new Request.Builder()
                .url(apiUrl)
                .addHeader("x-api-key", key)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();
    }

    // ==================== 响应解析 ====================

    /** 解析 OpenAI 兼容格式: choices[0].message.content */
    private String parseOpenAIResponse(String respBody) {
        JSONObject json = JSON.parseObject(respBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message != null) {
                String content = message.getString("content");
                if (content != null) return content;
            }
        }
        log.warn("OpenAI响应解析失败: {}", StrUtil.sub(respBody, 0, 200));
        return null;
    }

    /** 解析 Anthropic 原生格式: content[0].text */
    private String parseAnthropicResponse(String respBody) {
        JSONObject json = JSON.parseObject(respBody);
        JSONArray contentArr = json.getJSONArray("content");
        if (contentArr != null && !contentArr.isEmpty()) {
            String text = contentArr.getJSONObject(0).getString("text");
            if (text != null) return text;
        }
        log.warn("Anthropic响应解析失败: {}", StrUtil.sub(respBody, 0, 200));
        return null;
    }
}

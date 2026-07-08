package com.club.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Langfuse 轻量埋点客户端
 *
 * 不使用官方SDK（避免额外依赖），直接HTTP POST到Langfuse API
 * 记录每次LLM调用的：trace_id、耗时、token数、成功/失败状态
 *
 * 【答辩讲解点】LLMOps可观测性：Langfuse追踪每次Agent调用的全链路数据
 */
@Slf4j
@Component
public class LangfuseClient {

    @Value("${langfuse.enabled:false}")
    private boolean enabled;

    @Value("${langfuse.public-key:}")
    private String publicKey;

    @Value("${langfuse.secret-key:}")
    private String secretKey;

    @Value("${langfuse.host:https://cloud.langfuse.com}")
    private String host;

    @PostConstruct
    public void init() {
        if (enabled && !publicKey.isEmpty()) {
            log.info("Langfuse埋点已启用: {}", host);
        } else {
            log.info("Langfuse埋点未启用（缺少密钥或未开启），LLM调用日志仅存本地数据库");
        }
    }

    /**
     * 上报一次LLM调用追踪
     *
     * @param traceName    追踪名称，如 "PRD-Agent"
     * @param userId       用户ID
     * @param inputPrompt  输入的Prompt（截取前500字符）
     * @param outputText   输出的文本（截取前500字符）
     * @param tokensUsed   消耗的Token数
     * @param latencyMs    耗时（毫秒）
     * @param status       状态: "SUCCESS" / "FAILED" / "TIMEOUT"
     * @return traceId 追踪ID，失败返回null
     */
    public String trace(String traceName, String userId, String inputPrompt,
                         String outputText, int tokensUsed, int latencyMs, String status) {
        if (!enabled || publicKey.isEmpty()) return null;

        String traceId = UUID.randomUUID().toString();
        try {
            // 构造Langfuse兼容的JSON
            String json = String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"userId\":\"%s\",\"metadata\":{" +
                "\"input\":\"%s\",\"output\":\"%s\",\"tokens\":%d,\"latencyMs\":%d,\"status\":\"%s\"}," +
                "\"timestamp\":\"%s\"}",
                traceId, escapeJson(traceName), escapeJson(userId),
                escapeJson(truncate(inputPrompt, 500)),
                escapeJson(truncate(outputText, 500)),
                tokensUsed, latencyMs, escapeJson(status),
                java.time.Instant.now().toString()
            );

            URL url = new URL(host + "/api/public/traces");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization",
                "Basic " + java.util.Base64.getEncoder()
                    .encodeToString((publicKey + ":" + secretKey).getBytes()));
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                log.debug("Langfuse埋点成功: traceId={}, name={}", traceId, traceName);
                return traceId;
            } else {
                log.warn("Langfuse埋点返回非200: {}", code);
            }
        } catch (Exception e) {
            log.warn("Langfuse埋点失败(不影响主流程): {}", e.getMessage());
        }
        return null;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}

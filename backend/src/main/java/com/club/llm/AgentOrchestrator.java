package com.club.llm;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.club.entity.LlmCallLog;
import com.club.mapper.LlmCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 活动策划引擎 — 调用 LLM 生成活动策划方案
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ClaudeClient claudeClient;
    private final LlmCallLogMapper llmCallLogMapper;
    private final LangfuseClient langfuseClient;

    /** PRD Agent：根据活动需求生成完整活动策划方案 */
    private static final String PRD_PROMPT =
        "你是一位经验丰富的校园活动策划专家。用户将描述一个活动需求，请你生成一份完整的活动策划方案。" +
        "方案必须包含以下章节（用Markdown格式）：\n" +
        "## 一、活动概述\n- 活动名称\n- 活动主题\n- 活动目的与意义\n" +
        "## 二、活动详情\n- 活动时间（建议具体日期和时段）\n- 活动地点\n- 目标参与人数\n- 活动形式\n" +
        "## 三、活动流程\n按时间顺序列出活动当天详细流程（每步标注时间和负责人）\n" +
        "## 四、所需资源\n- 场地需求\n- 物资清单（列出具体名称和数量）\n- 经费预算（分项列出预估金额）\n" +
        "## 五、宣传推广方案\n## 六、应急预案\n## 七、预期效果\n" +
        "要求：内容具体、可操作性强，直接给方案不要解释。";

    public JSONObject orchestrate(String userRequirement, Long userId, Long activityId) {
        long startTime = System.currentTimeMillis();
        JSONObject result = new JSONObject();

        log.info("[AI策划] 调用 LLM 生成活动策划方案...");
        String prdRaw = callAgentWithLog("PRD", PRD_PROMPT, userRequirement, userId, activityId);
        if (prdRaw == null) {
            result.put("error", "AI 策划方案生成失败，请确认 API Key 已正确配置");
            return result;
        }

        String prdMarkdown = MermaidUtil.extractMarkdown(prdRaw);
        result.put("prd", prdMarkdown);

        long totalTime = System.currentTimeMillis() - startTime;
        result.put("totalTime", totalTime);
        log.info("[AI策划] 完成，{} 字符，耗时 {}ms", prdMarkdown.length(), totalTime);
        return result;
    }

    private String callAgentWithLog(String agentType, String systemPrompt, String userMessage,
                                     Long userId, Long activityId) {
        long start = System.currentTimeMillis();
        LlmCallLog logEntity = new LlmCallLog();
        logEntity.setUserId(userId);
        logEntity.setActivityId(activityId);
        logEntity.setAgentType(agentType);
        logEntity.setInputPrompt(StrUtil.sub(userMessage, 0, 500));

        try {
            String result = claudeClient.sendMessage(systemPrompt, userMessage);
            long latency = System.currentTimeMillis() - start;

            logEntity.setOutputContent(result != null ? StrUtil.sub(result, 0, 2000) : null);
            logEntity.setLatencyMs((int) latency);
            logEntity.setStatus(result != null ? "SUCCESS" : "FAILED");
            logEntity.setCreateTime(LocalDateTime.now());
            llmCallLogMapper.insert(logEntity);

            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            logEntity.setLatencyMs((int) latency);
            logEntity.setStatus("FAILED");
            logEntity.setErrorMsg(e.getMessage());
            logEntity.setCreateTime(LocalDateTime.now());
            llmCallLogMapper.insert(logEntity);
            return null;
        }
    }
}

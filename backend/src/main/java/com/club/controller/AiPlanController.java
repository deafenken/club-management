package com.club.controller;

import com.alibaba.fastjson2.JSONObject;
import com.club.common.Result;
import com.club.llm.AgentOrchestrator;
import com.club.mapper.LlmCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI智能活动策划控制器 — 核心创新模块
 *
 * 接口 POST /api/ai/plan
 * 请求体: { "requirement": "我想办一场迎新晚会...", "userId": 3, "activityId": 10 }
 * 响应:  { "prd": "Markdown策划方案", "gantt": "Mermaid甘特图代码", "er": "Mermaid ER图代码" }
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiPlanController {

    private final AgentOrchestrator orchestrator;
    private final LlmCallLogMapper llmCallLogMapper;

    /**
     * 触发AI智能策划 — 多Agent协同生成活动策划方案+甘特图+ER图
     */
    @PostMapping("/plan")
    public Result<?> generatePlan(@RequestBody Map<String, Object> body) {
        String requirement = (String) body.get("requirement");
        // 从JWT获取当前用户，防止IDOR（不可信任请求体中的userId）
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = principal instanceof Long ? (Long) principal : null;
        Long activityId = body.get("activityId") != null ?
                Long.valueOf(body.get("activityId").toString()) : null;

        if (requirement == null || requirement.trim().isEmpty()) {
            return Result.fail("请输入活动需求描述");
        }

        JSONObject plan = orchestrator.orchestrate(requirement, userId, activityId);
        if (plan.containsKey("error")) {
            return Result.fail(plan.getString("error"));
        }
        return Result.ok(plan);
    }

    /** 查询当前用户的LLM调用历史记录 */
    @GetMapping("/history")
    public Result<?> getHistory(@RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        var result = llmCallLogMapper.selectPage(
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.club.entity.LlmCallLog>()
                .eq(com.club.entity.LlmCallLog::getUserId, userId)
                .orderByDesc(com.club.entity.LlmCallLog::getCreateTime)
        );
        return Result.ok(result);
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof Long ? (Long) principal : null;
    }
}

package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("llm_call_log")
public class LlmCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    private String agentType;
    private String inputPrompt;
    private String outputContent;
    private String cleanedContent;
    private Integer tokensUsed;
    private Integer latencyMs;
    private String status;
    private String errorMsg;
    private Integer retryCount;
    private Integer mermaidValid;
    private String langfuseTraceId;
    private LocalDateTime createTime;
}

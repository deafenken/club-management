package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("application_draft")
public class ApplicationDraft {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String appType;         // CLUB/ACTIVITY/VENUE/RESOURCE/FUND
    private Integer stepIndex;      // 当前步骤索引
    private String formData;        // JSON草稿
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

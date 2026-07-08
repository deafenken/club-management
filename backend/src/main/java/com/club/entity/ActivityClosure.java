package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_closure")
public class ActivityClosure {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private String signInSheetUrl;
    private String summary;             // 活动总结(TEXT)
    private String expenseReceiptsUrl;
    private String status;              // PENDING/APPROVED
    private Long submittedBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

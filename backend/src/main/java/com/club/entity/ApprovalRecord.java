package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appType;         // CLUB/ACTIVITY/VENUE/RESOURCE/FUND
    private Long businessId;        // 关联业务ID
    private Integer stepOrder;      // 审批层级: 1/2/3/4
    private String stepName;        // 审批节点名称
    private Long approverId;        // 审批人ID
    private String status;          // PENDING/APPROVED/REJECTED
    private String comment;         // 审批意见(TEXT)
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

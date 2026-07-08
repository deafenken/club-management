package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fund_record")
public class FundRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clubId;
    private Long activityId;
    private String type;
    private BigDecimal amount;
    private String category;
    private String description;
    private String status;
    private Long approvedBy;
    private LocalDateTime createTime;

    private Long applicantId;               // 申请人ID
    private Long teacherId;                 // 指导教师ID
    private String teacherName;             // 指导教师姓名
    private String teacherApprovalUrl;      // 教师签字确认单附件URL
    private String fundType;                // 经费类型: ANNUAL/ACTIVITY/EQUIPMENT/TRAVEL/PRIZE/VENUE/PUBLICITY
    private String source;                  // 资金来源
    private String budgetItems;             // JSON: 预算明细 [{name,qty,unitPrice,subtotal,purpose}]
    private BigDecimal totalAmount;         // 申请总金额
    private String attachments;             // JSON: 附件列表 [{url,label}]
    private BigDecimal approvedAmount;      // 批准金额
    private LocalDateTime disbursedTime;    // 拨款时间
    private Long closureId;                 // 关联核销记录ID
    private String closureStatus;           // 核销状态: PENDING/COMPLETED
    private String rejectReason;            // 驳回修改意见(TEXT)
}

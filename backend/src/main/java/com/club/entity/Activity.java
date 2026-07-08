package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long clubId;
    private String title;
    private String description;
    private String category;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private Integer maxParticipants;
    private Integer enrolledCount;
    private String status;
    private String checkinCode;
    private Integer needVenue;
    private Integer needResource;
    private Integer needFund;
    private String aiPlanContent;
    private Long createdBy;
    private Long approvedBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String organizer;               // 主办方
    private String coOrganizer;             // 协办方
    private String targetAudience;          // 面向人群: 全校开放/仅社团内部/指定学院
    private LocalDateTime regDeadline;      // 报名截止时间
    private String planFileUrl;             // 活动策划方案附件
    private String posterUrl;               // 宣传海报附件
    private String safetyContact;           // 安全负责人姓名
    private String safetyPhone;             // 安全负责人电话
    private String safetyPlan;              // 应急处置方案(TEXT)
    private BigDecimal budgetAmount;        // 活动预算金额
    private String fundSource;              // 经费来源
    private Integer isFee;                  // 是否向学生收费(0/1)
    private BigDecimal feeAmount;           // 收费标准
    private String guestName;               // 校外嘉宾姓名
    private String guestOrg;                // 校外嘉宾单位
    private String guestCredential;         // 校外嘉宾身份资质
    private Integer isOffCampus;            // 是否校外活动(0/1)
    private String offCampusLocation;       // 外出地点
    private String offCampusTransport;      // 交通方案
    private String offCampusFiling;         // 外出安全备案(TEXT)
    private Long linkedVenueBookingId;      // 关联场地预约ID
    private Long linkedResourceBorrowId;    // 关联物资借用ID
    private Long linkedFundRecordId;        // 关联经费申请ID
}

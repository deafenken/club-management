package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统通知实体 — 对应 sys_notification 表
 */
@Data
@TableName("sys_notification")
public class SysNotification {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收用户ID */
    private Long userId;

    /** 通知标题 */
    private String title;

    /** 通知正文 */
    private String content;

    /** 业务类型枚举值（NotificationTypeEnum.code） */
    private String businessType;

    /** 关联业务ID，点击可跳转 */
    private Long businessId;

    /** 通知大类：APPROVAL/ACTIVITY/ANNOUNCEMENT/SYSTEM */
    private String type;

    /** 是否已读：0未读/1已读 */
    private Integer isRead;

    /** 逻辑删除：0正常/1已删除 */
    @TableLogic
    private Integer isDelete;

    /** 是否已撤回：0正常/1已撤回 */
    private Integer isRevoked;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 阅读时间 */
    private LocalDateTime readTime;
}

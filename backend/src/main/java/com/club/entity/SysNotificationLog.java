package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知操作日志 — 对应 sys_notification_log 表
 */
@Data
@TableName("sys_notification_log")
public class SysNotificationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联通知ID（清空/批量操作可空） */
    private Long notificationId;

    /** 操作用户ID */
    private Long userId;

    /** 操作类型：READ/MARK_ALL/CLEAR_ALL/RECALL/SEND */
    private String action;

    /** 操作详情 */
    private String detail;

    /** 操作时间 */
    private LocalDateTime createTime;
}

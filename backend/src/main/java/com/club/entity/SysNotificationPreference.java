package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户通知偏好设置 — 对应 sys_notification_preference 表
 */
@Data
@TableName("sys_notification_preference")
public class SysNotificationPreference {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 通知类型（NotificationTypeEnum.code） */
    private String preferType;

    /** 是否启用：0关闭/1开启 */
    private Integer enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

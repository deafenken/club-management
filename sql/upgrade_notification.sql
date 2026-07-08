-- =====================================================
-- sys_notification 通知表升级 SQL（在现有表基础上改造）
-- 执行: mysql -u root -p club_management < upgrade_notification.sql
-- =====================================================

-- 1. 备份旧表（如存在数据）
-- CREATE TABLE IF NOT EXISTS sys_notification_backup AS SELECT * FROM sys_notification;

-- 2. 重建通知表（完整版）
DROP TABLE IF EXISTS sys_notification;
CREATE TABLE sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID，关联sys_user.id',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知正文内容',
    business_type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' COMMENT '业务类型枚举：CLUB_APPROVAL/MEMBER_APPROVAL/JOIN_REQUEST/ACTIVITY_APPROVAL/ACTIVITY_PENDING/VENUE_APPROVAL/VENUE_PENDING/RESOURCE_APPROVAL/RESOURCE_PENDING/FUND_APPROVAL/FUND_PENDING/ANNOUNCEMENT/SYSTEM',
    business_id BIGINT COMMENT '关联业务ID（社团ID/活动ID/场地预约ID等），用于点击跳转',
    type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知大类：APPROVAL审批通知/ACTIVITY活动通知/ANNOUNCEMENT公告通知/SYSTEM系统通知',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0未读/1已读',
    is_delete TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常/1已删除',
    is_revoked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已撤回：0正常/1已撤回',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_time DATETIME COMMENT '阅读时间',
    INDEX idx_user_read (user_id, is_read, is_delete) COMMENT '用户未读查询联合索引',
    INDEX idx_user_type (user_id, business_type, is_delete) COMMENT '用户按类型筛选索引',
    INDEX idx_create_time (create_time) COMMENT '定时清理任务索引',
    INDEX idx_business (business_type, business_id) COMMENT '业务关联查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 3. 用户通知偏好设置表
DROP TABLE IF EXISTS sys_notification_preference;
CREATE TABLE sys_notification_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    prefer_type VARCHAR(30) NOT NULL COMMENT '通知类型，对应NotificationTypeEnum',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0关闭/1开启',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_type (user_id, prefer_type) COMMENT '用户+类型唯一约束',
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知偏好设置表';

-- 4. 通知操作日志表
DROP TABLE IF EXISTS sys_notification_log;
CREATE TABLE sys_notification_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    notification_id BIGINT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    action VARCHAR(30) NOT NULL COMMENT '操作类型：READ/MARK_ALL/CLEAR_ALL/RECALL/SEND',
    detail VARCHAR(500) COMMENT '操作详情',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_time (user_id, create_time),
    FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知操作日志表';

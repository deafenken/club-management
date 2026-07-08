package com.club.enums;

import lombok.Getter;

/**
 * 通知业务类型枚举 — 统一管理所有通知类型常量，禁止代码中使用硬编码字符串
 */
@Getter
public enum NotificationTypeEnum {

    // ===== 审批通知类 =====
    CLUB_APPROVAL("CLUB_APPROVAL", "社团审批", "APPROVAL"),
    MEMBER_APPROVAL("MEMBER_APPROVAL", "成员审批", "APPROVAL"),
    ACTIVITY_APPROVAL("ACTIVITY_APPROVAL", "活动审批", "APPROVAL"),
    VENUE_APPROVAL("VENUE_APPROVAL", "场地审批", "APPROVAL"),
    RESOURCE_APPROVAL("RESOURCE_APPROVAL", "物资审批", "APPROVAL"),
    FUND_APPROVAL("FUND_APPROVAL", "经费审批", "APPROVAL"),

    // ===== 待审批提醒类 =====
    JOIN_REQUEST("JOIN_REQUEST", "入社申请", "APPROVAL"),
    ACTIVITY_PENDING("ACTIVITY_PENDING", "活动待审", "APPROVAL"),
    VENUE_PENDING("VENUE_PENDING", "场地待审", "APPROVAL"),
    RESOURCE_PENDING("RESOURCE_PENDING", "物资待审", "APPROVAL"),
    FUND_PENDING("FUND_PENDING", "经费待审", "APPROVAL"),

    // ===== 公告/系统通知 =====
    ANNOUNCEMENT("ANNOUNCEMENT", "平台公告", "ANNOUNCEMENT"),
    SYSTEM("SYSTEM", "系统通知", "SYSTEM");

    /** 枚举code，存入数据库 */
    private final String code;

    /** 中文描述，前端展示用 */
    private final String label;

    /** 通知大类：APPROVAL / ACTIVITY / ANNOUNCEMENT / SYSTEM */
    private final String category;

    NotificationTypeEnum(String code, String label, String category) {
        this.code = code;
        this.label = label;
        this.category = category;
    }

    /** 根据code查找枚举，找不到返回SYSTEM */
    public static NotificationTypeEnum fromCode(String code) {
        for (NotificationTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return SYSTEM;
    }

    /** 校验code是否有效 */
    public static boolean isValid(String code) {
        for (NotificationTypeEnum e : values()) {
            if (e.code.equals(code)) return true;
        }
        return false;
    }
}

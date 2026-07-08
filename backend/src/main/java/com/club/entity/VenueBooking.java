package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("venue_booking")
public class VenueBooking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long venueId;
    private Long activityId;
    private Long bookingUserId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private String status;
    private Long approvedBy;
    private LocalDateTime createTime;

    private Long clubId;                    // 所属社团ID
    private Integer expectedAttendees;      // 预计到场人数
    private String equipmentNeeds;          // JSON: 配套设备需求
    private Integer earlyArrivalMin;        // 提前进场布置时长(分钟)
    private String cleanupNote;             // 场地复原责任说明
    private String safetyContact;           // 场地安全责任人
    private String safetyPhone;             // 安全责任人电话
    private Integer agreedDamageClause;     // 损坏赔偿承诺(0/1)
    private String returnNote;              // 场地归还验收备注(TEXT)
}

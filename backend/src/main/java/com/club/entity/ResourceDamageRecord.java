package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("resource_damage_record")
public class ResourceDamageRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long borrowId;
    private Long itemId;
    private String damageDesc;          // 损坏描述(TEXT)
    private BigDecimal repairCost;
    private Long handlerId;
    private String status;              // PENDING_PAY/PAID
    private LocalDateTime createTime;
}

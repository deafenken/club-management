package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("resource_borrow")
public class ResourceBorrow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long userId;
    private Long activityId;
    private Integer quantity;
    private LocalDate borrowDate;
    private LocalDate planReturnDate;
    private LocalDate actualReturnDate;
    private String status;
    private Long approvedBy;
    private LocalDateTime createTime;

    private Long clubId;                    // 借用社团ID
    private String borrowerName;            // 借用人姓名
    private String borrowerStudentId;       // 借用人学号
    private String borrowerPhone;           // 借用人联系电话
    private LocalDate borrowStartDate;      // 借用起始日期
    private String custodian;               // 物资保管责任人
    private String usageScenario;           // 使用场景说明
    private Integer isValuable;             // 是否贵重物资(0/1)
    private String attachmentUrl;           // 贵重物资指导教师同意书附件URL
    private String batchItems;              // JSON: 多物资批量借用明细
    private String returnCondition;         // 归还验收: GOOD/DAMAGED
}

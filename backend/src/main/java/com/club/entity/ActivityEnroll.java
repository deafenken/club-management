package com.club.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("activity_enroll")
public class ActivityEnroll {
    @TableId(type=IdType.AUTO) private Long id;
    private Long activityId; private Long userId; private String status;
    private LocalDateTime enrollTime;
}
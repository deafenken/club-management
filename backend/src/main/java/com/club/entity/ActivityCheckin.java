package com.club.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("activity_checkin")
public class ActivityCheckin {
    @TableId(type=IdType.AUTO) private Long id;
    private Long activityId; private Long userId; private LocalDateTime checkinTime;
}
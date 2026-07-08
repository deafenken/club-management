package com.club.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("sys_announcement")
public class SysAnnouncement {
    @TableId(type=IdType.AUTO) private Long id;
    private String title; private String content; private Integer isTop;
    private Long createdBy; private LocalDateTime createTime;
    private String target; // 推送范围: ALL/PRESIDENT/STUDENT/TEACHER/ADMIN
}
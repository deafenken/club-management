package com.club.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("venue")
public class Venue {
    @TableId(type=IdType.AUTO) private Long id;
    private String name; private String location; private Integer capacity;
    private String facilities; private Integer status; private LocalDateTime createTime;
}
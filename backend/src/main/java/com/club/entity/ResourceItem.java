package com.club.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("resource_item")
public class ResourceItem {
    @TableId(type=IdType.AUTO) private Long id;
    private String name; private String category; private Integer total;
    private Integer available; private String unit; private Integer status;
    private LocalDateTime createTime;
}
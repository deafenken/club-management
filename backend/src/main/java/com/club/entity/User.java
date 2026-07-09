package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 系统用户实体 */
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String role;        // STUDENT/PRESIDENT/TEACHER/ADMIN
    private String college;
    private Integer status;
    @TableField("is_super")
    private Integer isSuper;    // 总管理员(群主)：0否 1是，仅其可移除其他管理员
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

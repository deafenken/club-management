package com.club.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("club")
public class Club {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String description;
    private Long presidentId;
    private Long teacherId;
    private Integer memberCount;
    private Integer status;     // 0待审 1通过 2驳回
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 步骤1: 基础信息
    private String foundingBackground;    // 社团成立背景(TEXT)
    private String differentiation;       // 差异化定位(TEXT)
    private Integer recruitScale;         // 预计招新规模
    private String recruitColleges;       // 面向招生学院范围

    // 步骤2: 负责人与指导教师
    private String presidentName;         // 首任社长姓名
    private String presidentStudentId;    // 社长学号
    private String presidentCollege;      // 社长学院专业
    private String presidentGrade;        // 社长年级
    private String presidentPhone;        // 社长手机号
    private String presidentQq;           // 社长QQ
    private String initiators;            // JSON: 核心发起人列表(最少8人)
    private String advisorName;           // 指导教师姓名
    private String advisorTeacherId;      // 指导教师工号
    private String advisorDept;           // 指导教师所属院系
    private String advisorPhone;          // 指导教师办公电话
    private String advisorTitle;          // 指导教师职务
    private String advisorAgreementUrl;   // 指导教师签字同意书附件URL

    // 步骤3: 运营与活动规划
    private String charterText;           // 社团章程文本(TEXT)
    private String charterFileUrl;        // 章程文件附件URL
    private String semesterPlan;          // 首期学期活动方案(TEXT)
    private String clubSafetyPlan;        // 安全应急预案(TEXT)
    private String feeStandard;           // 会费收取标准
    private BigDecimal annualBudget;      // 年度开销预估

    // 步骤4: 合规承诺
    private Integer commitmentSigned;     // 合规承诺已签署(0/1)
    private String attachments;           // JSON: 全部附件汇总
    private String rejectReason;          // 驳回修改意见(TEXT)
}

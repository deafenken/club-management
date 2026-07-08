-- =====================================================
-- 社团管理系统 — 5模块审批流程升级 DDL
-- 执行方式: mysql -uroot -p club_management < migration_v2.sql
-- 兼容重复执行（使用 IF NOT EXISTS / 存储过程安全加列）
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 一、新增表
-- =====================================================

-- 1.1 统一草稿表
CREATE TABLE IF NOT EXISTS application_draft (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  app_type VARCHAR(30) NOT NULL COMMENT 'CLUB/ACTIVITY/VENUE/RESOURCE/FUND',
  step_index INT DEFAULT 0 COMMENT '当前步骤(分步表单用)',
  form_data JSON COMMENT '草稿JSON',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_type (user_id, app_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一申请草稿';

-- 1.2 多级审批流水表
CREATE TABLE IF NOT EXISTS approval_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  app_type VARCHAR(30) NOT NULL COMMENT 'CLUB/ACTIVITY/VENUE/RESOURCE/FUND',
  business_id BIGINT NOT NULL COMMENT '关联业务主键ID',
  step_order INT NOT NULL COMMENT '审批层级: 1初审/2复审/3终审/4校级领导',
  step_name VARCHAR(50) COMMENT '审批节点名称',
  approver_id BIGINT COMMENT '审批人ID(NULL=待审批)',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  comment TEXT COMMENT '审批意见/驳回修改意见',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_business (app_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多级审批流水';

-- 1.3 活动结项表
CREATE TABLE IF NOT EXISTS activity_closure (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL UNIQUE COMMENT '关联活动ID',
  sign_in_sheet_url VARCHAR(500) COMMENT '签到表附件URL',
  summary TEXT COMMENT '活动总结',
  expense_receipts_url VARCHAR(500) COMMENT '报销票据附件URL',
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED',
  submitted_by BIGINT COMMENT '提交人ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动结项';

-- 1.4 场地损坏记录表
CREATE TABLE IF NOT EXISTS venue_damage_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT COMMENT '关联场地预约ID',
  venue_id BIGINT COMMENT '场地ID',
  damage_desc TEXT COMMENT '损坏描述',
  repair_cost DECIMAL(10,2) DEFAULT 0 COMMENT '维修/赔偿金额',
  handler_id BIGINT COMMENT '验收人ID',
  status VARCHAR(20) DEFAULT 'PENDING_PAY' COMMENT 'PENDING_PAY/PAID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地损坏验收记录';

-- 1.5 物资损坏记录表
CREATE TABLE IF NOT EXISTS resource_damage_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  borrow_id BIGINT COMMENT '关联借用记录ID',
  item_id BIGINT COMMENT '物资ID',
  damage_desc TEXT COMMENT '损坏描述',
  repair_cost DECIMAL(10,2) DEFAULT 0,
  handler_id BIGINT COMMENT '验收人ID',
  status VARCHAR(20) DEFAULT 'PENDING_PAY' COMMENT 'PENDING_PAY/PAID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物资损坏验收记录';

-- =====================================================
-- 二、安全加列存储过程（避免重复加列报错）
-- =====================================================

DROP PROCEDURE IF EXISTS safe_add_column;
DELIMITER $$
CREATE PROCEDURE safe_add_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN col_def VARCHAR(500))
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col
  ) THEN
    SET @stmt = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', col, ' ', col_def);
    PREPARE stmt FROM @stmt;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- =====================================================
-- 三、activity 加列
-- =====================================================
CALL safe_add_column('activity', 'organizer',           'VARCHAR(100) COMMENT "主办方"');
CALL safe_add_column('activity', 'co_organizer',        'VARCHAR(100) COMMENT "协办方"');
CALL safe_add_column('activity', 'target_audience',     'VARCHAR(30) DEFAULT "全校开放" COMMENT "面向人群: 全校开放/仅社团内部/指定学院"');
CALL safe_add_column('activity', 'reg_deadline',        'DATETIME COMMENT "报名截止时间"');
CALL safe_add_column('activity', 'plan_file_url',       'VARCHAR(500) COMMENT "活动策划方案附件"');
CALL safe_add_column('activity', 'poster_url',          'VARCHAR(500) COMMENT "活动宣传海报"');
CALL safe_add_column('activity', 'safety_contact',      'VARCHAR(50) COMMENT "安全负责人姓名"');
CALL safe_add_column('activity', 'safety_phone',        'VARCHAR(20) COMMENT "安全负责人电话"');
CALL safe_add_column('activity', 'safety_plan',         'TEXT COMMENT "应急处置方案"');
CALL safe_add_column('activity', 'budget_amount',       'DECIMAL(10,2) DEFAULT 0 COMMENT "活动预算总金额"');
CALL safe_add_column('activity', 'fund_source',         'VARCHAR(50) COMMENT "经费来源: 校拨款/社团会费/校外赞助"');
CALL safe_add_column('activity', 'is_fee',              'TINYINT(1) DEFAULT 0 COMMENT "是否向参与学生收费"');
CALL safe_add_column('activity', 'fee_amount',          'DECIMAL(10,2) DEFAULT 0 COMMENT "收费标准(元/人)"');
CALL safe_add_column('activity', 'guest_name',          'VARCHAR(50) COMMENT "校外嘉宾姓名"');
CALL safe_add_column('activity', 'guest_org',           'VARCHAR(100) COMMENT "校外嘉宾单位"');
CALL safe_add_column('activity', 'guest_credential',    'VARCHAR(200) COMMENT "校外嘉宾身份资质"');
CALL safe_add_column('activity', 'is_off_campus',       'TINYINT(1) DEFAULT 0 COMMENT "是否校外活动"');
CALL safe_add_column('activity', 'off_campus_location', 'VARCHAR(200) COMMENT "外出地点"');
CALL safe_add_column('activity', 'off_campus_transport', 'VARCHAR(200) COMMENT "交通方案"');
CALL safe_add_column('activity', 'off_campus_filing',   'TEXT COMMENT "外出安全备案说明"');
CALL safe_add_column('activity', 'linked_venue_booking_id',  'BIGINT COMMENT "关联场地预约ID"');
CALL safe_add_column('activity', 'linked_resource_borrow_id','BIGINT COMMENT "关联物资借用ID"');
CALL safe_add_column('activity', 'linked_fund_record_id',    'BIGINT COMMENT "关联经费申请ID"');
CALL safe_add_column('activity', 'approved_by',         'BIGINT COMMENT "审批人ID"');

-- =====================================================
-- 四、venue_booking 加列
-- =====================================================
CALL safe_add_column('venue_booking', 'club_id',             'BIGINT COMMENT "所属社团ID"');
CALL safe_add_column('venue_booking', 'activity_id',         'BIGINT COMMENT "关联活动ID(可为空)"');
CALL safe_add_column('venue_booking', 'expected_attendees',  'INT DEFAULT 0 COMMENT "预计到场人数"');
CALL safe_add_column('venue_booking', 'equipment_needs',     'JSON COMMENT "配套设备需求"');
CALL safe_add_column('venue_booking', 'early_arrival_min',   'INT DEFAULT 30 COMMENT "提前进场布置时长(分钟)"');
CALL safe_add_column('venue_booking', 'cleanup_note',        'VARCHAR(500) COMMENT "场地复原责任说明"');
CALL safe_add_column('venue_booking', 'safety_contact',      'VARCHAR(50) COMMENT "场地安全责任人"');
CALL safe_add_column('venue_booking', 'safety_phone',        'VARCHAR(20) COMMENT "安全责任人电话"');
CALL safe_add_column('venue_booking', 'agreed_damage_clause','TINYINT(1) DEFAULT 0 COMMENT "损坏赔偿承诺勾选"');
CALL safe_add_column('venue_booking', 'return_note',         'TEXT COMMENT "场地归还验收备注"');

-- =====================================================
-- 五、resource_borrow 加列
-- =====================================================
CALL safe_add_column('resource_borrow', 'club_id',              'BIGINT COMMENT "借用社团ID"');
CALL safe_add_column('resource_borrow', 'borrower_name',        'VARCHAR(50) COMMENT "借用人姓名"');
CALL safe_add_column('resource_borrow', 'borrower_student_id',  'VARCHAR(30) COMMENT "借用人学号"');
CALL safe_add_column('resource_borrow', 'borrower_phone',       'VARCHAR(20) COMMENT "借用人联系电话"');
CALL safe_add_column('resource_borrow', 'activity_id',          'BIGINT COMMENT "关联活动ID"');
CALL safe_add_column('resource_borrow', 'borrow_start_date',    'DATE COMMENT "借用起始日期"');
CALL safe_add_column('resource_borrow', 'custodian',            'VARCHAR(50) COMMENT "物资保管责任人"');
CALL safe_add_column('resource_borrow', 'usage_scenario',       'VARCHAR(500) COMMENT "使用场景说明"');
CALL safe_add_column('resource_borrow', 'is_valuable',          'TINYINT(1) DEFAULT 0 COMMENT "是否贵重物资"');
CALL safe_add_column('resource_borrow', 'attachment_url',       'VARCHAR(500) COMMENT "贵重物资指导教师同意书附件"');
CALL safe_add_column('resource_borrow', 'batch_items',          'JSON COMMENT "多物资批量借用明细"');
CALL safe_add_column('resource_borrow', 'actual_return_date',   'DATE COMMENT "实际归还日期"');
CALL safe_add_column('resource_borrow', 'return_condition',     'VARCHAR(30) COMMENT "归还验收: GOOD/DAMAGED/WAITING_PAY"');

-- =====================================================
-- 六、fund_record 加列
-- =====================================================
CALL safe_add_column('fund_record', 'applicant_id',         'BIGINT COMMENT "申请人ID"');
CALL safe_add_column('fund_record', 'teacher_id',           'BIGINT COMMENT "指导教师ID"');
CALL safe_add_column('fund_record', 'teacher_name',         'VARCHAR(50) COMMENT "指导教师姓名"');
CALL safe_add_column('fund_record', 'teacher_approval_url', 'VARCHAR(500) COMMENT "教师签字确认单附件"');
CALL safe_add_column('fund_record', 'fund_type',            'VARCHAR(30) COMMENT "经费类型: ANNUAL/ACTIVITY/EQUIPMENT/TRAVEL/PRIZE/VENUE/PUBLICITY"');
CALL safe_add_column('fund_record', 'activity_id',          'BIGINT COMMENT "关联活动ID"');
CALL safe_add_column('fund_record', 'source',               'VARCHAR(50) COMMENT "资金来源: 校团委拨款/社团会费/企业赞助"');
CALL safe_add_column('fund_record', 'budget_items',         'JSON COMMENT "预算明细JSON: [{name,qty,unitPrice,subtotal,purpose}]"');
CALL safe_add_column('fund_record', 'total_amount',         'DECIMAL(10,2) COMMENT "申请总金额"');
CALL safe_add_column('fund_record', 'attachments',          'JSON COMMENT "附件列表JSON: [url,label]"');
CALL safe_add_column('fund_record', 'approved_amount',      'DECIMAL(10,2) COMMENT "批准金额"');
CALL safe_add_column('fund_record', 'disbursed_time',       'DATETIME COMMENT "拨款时间"');
CALL safe_add_column('fund_record', 'closure_id',           'BIGINT COMMENT "关联核销记录ID"');
CALL safe_add_column('fund_record', 'closure_status',       'VARCHAR(20) COMMENT "核销状态: PENDING/COMPLETED"');
CALL safe_add_column('fund_record', 'reject_reason',        'TEXT COMMENT "驳回理由/修改意见"');

-- =====================================================
-- 七、club 加列
-- =====================================================
CALL safe_add_column('club', 'founding_background',  'TEXT COMMENT "社团成立背景"');
CALL safe_add_column('club', 'differentiation',      'TEXT COMMENT "社团差异化定位"');
CALL safe_add_column('club', 'recruit_scale',        'INT COMMENT "预计招新规模"');
CALL safe_add_column('club', 'recruit_colleges',     'VARCHAR(500) COMMENT "面向招生学院范围"');
CALL safe_add_column('club', 'president_name',       'VARCHAR(50) COMMENT "首任社长姓名"');
CALL safe_add_column('club', 'president_student_id', 'VARCHAR(30) COMMENT "社长学号"');
CALL safe_add_column('club', 'president_college',    'VARCHAR(50) COMMENT "社长学院专业"');
CALL safe_add_column('club', 'president_grade',      'VARCHAR(20) COMMENT "社长年级"');
CALL safe_add_column('club', 'president_phone',      'VARCHAR(20) COMMENT "社长手机号"');
CALL safe_add_column('club', 'president_qq',         'VARCHAR(20) COMMENT "社长QQ"');
CALL safe_add_column('club', 'initiators',           'JSON COMMENT "核心发起人列表JSON: [{name,studentId,role}] 至少8人"');
CALL safe_add_column('club', 'advisor_name',         'VARCHAR(50) COMMENT "指导教师姓名"');
CALL safe_add_column('club', 'advisor_teacher_id',   'VARCHAR(30) COMMENT "指导教师工号"');
CALL safe_add_column('club', 'advisor_dept',         'VARCHAR(100) COMMENT "指导教师所属院系"');
CALL safe_add_column('club', 'advisor_phone',        'VARCHAR(20) COMMENT "指导教师办公电话"');
CALL safe_add_column('club', 'advisor_title',        'VARCHAR(50) COMMENT "指导教师职务"');
CALL safe_add_column('club', 'advisor_agreement_url','VARCHAR(500) COMMENT "指导教师签字同意书附件"');
CALL safe_add_column('club', 'charter_text',         'TEXT COMMENT "社团章程文本"');
CALL safe_add_column('club', 'charter_file_url',     'VARCHAR(500) COMMENT "章程文件附件"');
CALL safe_add_column('club', 'semester_plan',        'TEXT COMMENT "首期学期活动方案"');
CALL safe_add_column('club', 'club_safety_plan',     'TEXT COMMENT "活动安全应急预案"');
CALL safe_add_column('club', 'fee_standard',         'VARCHAR(200) COMMENT "会费收取标准"');
CALL safe_add_column('club', 'annual_budget',        'DECIMAL(10,2) COMMENT "年度开销预估"');
CALL safe_add_column('club', 'commitment_signed',    'TINYINT(1) DEFAULT 0 COMMENT "合规承诺已签署"');
CALL safe_add_column('club', 'attachments',          'JSON COMMENT "全部附件汇总JSON"');
CALL safe_add_column('club', 'reject_reason',        'TEXT COMMENT "驳回修改意见"');

-- =====================================================
-- 八、sys_announcement 加 target 字段
-- =====================================================
CALL safe_add_column('sys_announcement', 'target', 'VARCHAR(30) DEFAULT "ALL" COMMENT "推送范围: ALL/PRESIDENT/STUDENT/ADMIN"');

-- =====================================================
-- 清理
-- =====================================================
DROP PROCEDURE IF EXISTS safe_add_column;

SELECT 'Migration v2 completed successfully!' AS result;

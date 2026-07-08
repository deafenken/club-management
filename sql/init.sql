-- =====================================================
-- 高校社团活动组织与资源管理系统 - 数据库初始化
-- 使用方法: mysql -u root -p < init.sql
-- =====================================================
DROP DATABASE IF EXISTS club_management;
CREATE DATABASE club_management DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE club_management;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 系统用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt',
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    college VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 社团表
DROP TABLE IF EXISTS club;
CREATE TABLE club (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    president_id BIGINT,
    teacher_id BIGINT,
    member_count INT DEFAULT 0,
    status TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (president_id) REFERENCES sys_user(id),
    FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 社团成员表
DROP TABLE IF EXISTS club_member;
CREATE TABLE club_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',
    status TINYINT DEFAULT 0,
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (club_id) REFERENCES club(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_cu (club_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 活动表
DROP TABLE IF EXISTS activity;
CREATE TABLE activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    location VARCHAR(200),
    max_participants INT DEFAULT 0,
    enrolled_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'DRAFT',
    checkin_code VARCHAR(6),
    need_venue TINYINT DEFAULT 0,
    need_resource TINYINT DEFAULT 0,
    need_fund TINYINT DEFAULT 0,
    ai_plan_content LONGTEXT,
    created_by BIGINT,
    approved_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (club_id) REFERENCES club(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 活动报名表
DROP TABLE IF EXISTS activity_enroll;
CREATE TABLE activity_enroll (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'ENROLLED',
    enroll_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_au (activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 签到表
DROP TABLE IF EXISTS activity_checkin;
CREATE TABLE activity_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    checkin_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (activity_id) REFERENCES activity(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_ck (activity_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 场地表
DROP TABLE IF EXISTS venue;
CREATE TABLE venue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    capacity INT DEFAULT 0,
    facilities VARCHAR(500),
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 场地预约表
DROP TABLE IF EXISTS venue_booking;
CREATE TABLE venue_booking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    venue_id BIGINT NOT NULL,
    activity_id BIGINT,
    booking_user_id BIGINT NOT NULL,
    booking_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    purpose VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (venue_id) REFERENCES venue(id),
    INDEX idx_vd (venue_id, booking_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 物资表
DROP TABLE IF EXISTS resource_item;
CREATE TABLE resource_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    total INT DEFAULT 0,
    available INT DEFAULT 0,
    unit VARCHAR(20) DEFAULT '件',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 物资借用表
DROP TABLE IF EXISTS resource_borrow;
CREATE TABLE resource_borrow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_id BIGINT,
    quantity INT NOT NULL,
    borrow_date DATE,
    plan_return_date DATE,
    actual_return_date DATE,
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES resource_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 经费表
DROP TABLE IF EXISTS fund_record;
CREATE TABLE fund_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    club_id BIGINT NOT NULL,
    activity_id BIGINT,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    category VARCHAR(50),
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (club_id) REFERENCES club(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. LLM调用日志表
DROP TABLE IF EXISTS llm_call_log;
CREATE TABLE llm_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    activity_id BIGINT,
    agent_type VARCHAR(20) NOT NULL,
    input_prompt TEXT,
    output_content LONGTEXT,
    cleaned_content LONGTEXT,
    tokens_used INT,
    latency_ms INT,
    status VARCHAR(20) DEFAULT 'SUCCESS',
    error_msg TEXT,
    retry_count INT DEFAULT 0,
    mermaid_valid TINYINT DEFAULT 0,
    langfuse_trace_id VARCHAR(100),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 通知表
DROP TABLE IF EXISTS sys_notification;
CREATE TABLE sys_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(20) DEFAULT 'SYSTEM',
    is_read TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. 公告表
DROP TABLE IF EXISTS sys_announcement;
CREATE TABLE sys_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    is_top TINYINT DEFAULT 0,
    created_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 测试数据
-- 密码均为 123456 (BCrypt hash)
-- =====================================================
SET @pwd = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

-- ==================== 用户 (12人) ====================
INSERT INTO sys_user (id, username, password, real_name, phone, role, college, status) VALUES
(1,  'admin',       @pwd, '张管理', '13800000001', 'ADMIN',     '校团委',     1),
(2,  'teacher01',   @pwd, '李老师', '13800000002', 'TEACHER',   '计算机学院',  1),
(3,  'teacher02',   @pwd, '王老师', '13800000007', 'TEACHER',   '艺术学院',    1),
(4,  'president01', @pwd, '赵社长', '13800000003', 'PRESIDENT', '计算机学院',  1),
(5,  'president02', @pwd, '钱社长', '13800000005', 'PRESIDENT', '艺术学院',    1),
(6,  'president03', @pwd, '陈社长', '13800000008', 'PRESIDENT', '体育学院',    1),
(7,  'president04', @pwd, '刘社长', '13800000009', 'PRESIDENT', '经管学院',    1),
(8,  'president05', @pwd, '杨社长', '13800000010', 'PRESIDENT', '文学院',      1),
(9,  'student01',   @pwd, '孙同学', '13800000004', 'STUDENT',   '计算机学院',  1),
(10, 'student02',   @pwd, '周同学', '13800000006', 'STUDENT',   '艺术学院',    1),
(11, 'student03',   @pwd, '吴同学', '13800000011', 'STUDENT',   '体育学院',    1),
(12, 'student04',   @pwd, '郑同学', '13800000012', 'STUDENT',   '经管学院',    1);

-- ==================== 社团 (12个) ====================
INSERT INTO club (id, name, category, description, president_id, teacher_id, member_count, status) VALUES
(1,  '计算机协会',     '学术科技', '探索前沿技术，培养编程与AI实践能力，定期举办技术讲座与Hackathon',                 4,  2, 2,   1),
(2,  '舞蹈社',         '文化艺术', '汇聚校园舞蹈爱好者，涵盖街舞、民族舞、现代舞等多种风格，每周定期排练',             5,  3, 2,   1),
(3,  '志愿者协会',     '志愿服务', '组织校内外公益志愿活动，支教助学、社区服务、环保行动，传递爱心',                   4,  2, 2,   1),
(4,  '篮球协会',       '体育竞技', '热血青春赛场！组织院系篮球联赛、三分球大赛，代表学校参加CUBA',                   6,  2, 2,   1),
(5,  '创新创业俱乐部', '创新创业', '激发创新思维，孵化创业项目，对接校内外资源，定期路演交流',                         7,  2, 2,   1),
(6,  '音乐社团',       '文化艺术', '用旋律点亮校园生活，设吉他、钢琴、声乐等分部，每学期举办专场音乐会',             8,  3, 2,   1),
(7,  '摄影协会',       '文化艺术', '记录校园光影瞬间，定期外拍采风、人像写真，举办年度摄影展',                       5,  3, 2,   1),
(8,  '辩论队',         '学术科技', '舌战群儒，以辩会友。代表学校参加省市级辩论赛，每周模辩训练',                     4,  2, 2,   1),
(9,  '足球社',         '体育竞技', '绿茵场上挥洒汗水，组织院系联赛、五人制挑战赛，征战校际杯赛',                     6,  2, 2,   1),
(10, '环保协会',       '志愿服务', '倡导绿色校园生活，垃圾分类推广、旧物回收、植树造林，为地球减负',                 7,  2, 2,   1),
(11, '话剧社',         '文化艺术', '演绎人生百态，每学期排演经典剧目，开设表演工作坊，培养舞台表现力',               8,  3, 2,   1),
(12, '数学建模协会',   '学术科技', '以数学之眼洞察世界，组织建模培训、参加国赛美赛，屡获佳绩',                       4,  2, 2,   1);

-- ==================== 社团成员 ====================
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(1,4,'PRESIDENT',1),  (1,9,'MEMBER',1),
(2,5,'PRESIDENT',1),  (2,10,'MEMBER',1),
(3,4,'PRESIDENT',1),  (3,9,'MEMBER',1),
(4,6,'PRESIDENT',1),  (4,11,'MEMBER',1),
(5,7,'PRESIDENT',1),  (5,12,'MEMBER',1),
(6,8,'PRESIDENT',1),  (6,10,'MEMBER',1),
(7,5,'PRESIDENT',1),  (7,10,'MEMBER',1),
(8,4,'PRESIDENT',1),  (8,9,'MEMBER',1),
(9,6,'PRESIDENT',1),  (9,11,'MEMBER',1),
(10,7,'PRESIDENT',1), (10,12,'MEMBER',1),
(11,8,'PRESIDENT',1), (11,10,'MEMBER',1),
(12,4,'PRESIDENT',1), (12,9,'MEMBER',1);

-- ==================== 场地 (8个) ====================
INSERT INTO venue (id, name, location, capacity, facilities, status) VALUES
(1, '大学生活动中心多功能厅', '活动中心一楼',   500,  '音响/灯光/投影/舞台/空调',       1),
(2, '第一报告厅',             '图书馆二楼',     300,  '音响/投影/会议桌椅/空调',         1),
(3, '第二报告厅',             '图书馆三楼',     180,  '投影/会议桌椅',                  1),
(4, '操场主席台区域',         '田径场北侧',     2000, '音响/电源/遮阳棚',               1),
(5, '社团活动室A',            '活动中心301',    50,   '桌椅/白板/投影/空调',            1),
(6, '社团活动室B',            '活动中心302',    40,   '桌椅/白板',                     1),
(7, '体育馆篮球场',           '体育馆一楼',     800,  '篮球架/记分牌/灯光/观众席',       1),
(8, '音乐厅排练室',           '艺术楼B1',       60,   '钢琴/音响/镜子/把杆',            1);

-- ==================== 物资 (12项) ====================
INSERT INTO resource_item (id, name, category, total, available, unit) VALUES
(1,  '便携音响',        '音响设备', 10,  8,  '台'),
(2,  '无线话筒',        '音响设备', 20,  15, '支'),
(3,  '折叠桌',          '桌椅帐篷', 50,  42, '张'),
(4,  '折叠椅',          '桌椅帐篷', 200, 168,'把'),
(5,  '帐篷3x3m',        '桌椅帐篷', 20,  18, '顶'),
(6,  '展板架',          '宣传物料', 30,  22, '个'),
(7,  '易拉宝展架',      '宣传物料', 25,  20, '个'),
(8,  '条幅(5m)',        '宣传物料', 40,  35, '条'),
(9,  '篮球',            '体育器材', 30,  25, '个'),
(10, '足球',            '体育器材', 20,  17, '个'),
(11, '对讲机',          '音响设备', 20,  18, '台'),
(12, '投影仪（便携）',  '音响设备', 8,   3,  '台');

-- ==================== 活动 (18个，涵盖多种状态) ====================
INSERT INTO activity (id, club_id, title, description, category, start_time, end_time, location, max_participants, enrolled_count, status, checkin_code, created_by) VALUES
-- 进行中
(1,  1,  '2026春季编程马拉松',            '48小时极限编程挑战，团队协作完成AI应用开发',              '比赛',   '2026-07-10 09:00:00', '2026-07-11 18:00:00', '大学生活动中心多功能厅', 100, 78,  'ONGOING',  'CPH001', 4),
(2,  4,  '院系篮球联赛总决赛',            '计算机学院 vs 经管学院巅峰对决',                         '比赛',   '2026-07-08 19:00:00', '2026-07-08 21:30:00', '体育馆篮球场',           500, 432, 'ONGOING',  'BBL001', 6),
(3,  6,  '夏日音乐节',                   '吉他弹唱、乐队表演、合唱团倾情演出，共赴音乐盛宴',          '演出',   '2026-07-12 18:30:00', '2026-07-12 22:00:00', '操场主席台区域',         800, 650, 'ONGOING',  'MUS001', 8),
-- 报名中
(4,  2,  '校园随机舞蹈挑战',              '随机播放音乐片段，即兴舞蹈battle，全员参与嗨翻全场',       '团建',   '2026-07-15 16:00:00', '2026-07-15 18:30:00', '大学生活动中心多功能厅', 150, 89,  'APPROVED', 'DAN001', 5),
(5,  7,  '校园光影摄影大赛',              '以「夏日校园」为主题，用镜头捕捉最美瞬间，设一二三等奖',    '比赛',   '2026-07-20 00:00:00', '2026-07-30 23:59:00', '线上投稿',               0,   56,  'APPROVED', 'PHO001', 5),
(6,  8,  '新生辩论赛半决赛',              '正方：人工智能终将取代人类工作 / 反方：人工智能无法取代人类', '比赛',   '2026-07-14 14:00:00', '2026-07-14 17:00:00', '第一报告厅',             200, 145, 'APPROVED', 'DEB001', 4),
(7,  5,  '创业项目路演交流会',            '5支学生创业团队展示商业计划书，投资人现场点评指导',        '讲座',   '2026-07-18 14:00:00', '2026-07-18 17:30:00', '第二报告厅',             120, 78,  'APPROVED', 'ENT001', 7),
-- 待审批
(8,  3,  '暑期支教志愿者招募宣讲会',      '介绍暑期赴山区支教计划，招募有爱心的志愿者同学',          '志愿',   '2026-07-22 10:00:00', '2026-07-22 12:00:00', '第一报告厅',             250, 0,   'PENDING',  NULL,    4),
(9,  9,  '校园五人制足球挑战赛',          '自由组队报名，淘汰赛制，冠军获定制奖杯及运动装备',        '比赛',   '2026-07-25 08:00:00', '2026-07-26 18:00:00', '操场主席台区域',         300, 0,   'PENDING',  NULL,    6),
(10, 11, '话剧社年度大戏《雷雨》公演',    '历时三个月排练，经典话剧全新演绎，免票入场',              '演出',   '2026-07-28 19:00:00', '2026-07-28 21:30:00', '大学生活动中心多功能厅', 400, 0,   'PENDING',  NULL,    8),
-- 已结束
(11, 1,  'AI技术前沿讲座',                '邀请知名科技企业工程师分享大模型技术发展趋势',            '讲座',   '2026-06-20 14:00:00', '2026-06-20 16:30:00', '第一报告厅',             200, 180, 'FINISHED', 'AI001',  4),
(12, 3,  '校园环保公益跑',                '每跑1公里捐1元给环保项目，百人参与，为绿色奔跑',          '志愿',   '2026-06-15 07:00:00', '2026-06-15 10:00:00', '田径场',                 300, 256, 'FINISHED', 'RUN001', 4),
(13, 10, '旧物回收绿色交换活动',          '以物易物，旧书旧衣回收再利用，倡导绿色低碳生活',          '志愿',   '2026-06-10 10:00:00', '2026-06-10 16:00:00', '学生广场',               0,   120, 'FINISHED', 'ECO001', 7),
(14, 12, '数学建模竞赛经验分享会',        '国赛一等奖团队分享建模思路与论文写作技巧',               '讲座',   '2026-06-25 19:00:00', '2026-06-25 21:00:00', '第二报告厅',             150, 132, 'FINISHED', 'MAT001', 4),
-- 草稿
(15, 6,  '校园十佳歌手大赛（筹备中）',    '面向全校征集报名，海选+复赛+决赛三阶段，音乐盛宴即将开启', '比赛',   '2026-08-01 00:00:00', '2026-08-15 23:59:00', '大学生活动中心多功能厅', 500, 0,   'DRAFT',    NULL,    8),
(16, 4,  '三分球投篮大赛（策划中）',      '个人三分球挑战，记录最高连中数，赢取专属球鞋',            '比赛',   '2026-08-05 00:00:00', '2026-08-05 23:59:00', '体育馆篮球场',           200, 0,   'DRAFT',    NULL,    6),
-- 已驳回
(17, 2,  '深夜通宵舞蹈派对',              '申请在操场举办通宵派对活动',                             '团建',   '2026-07-01 22:00:00', '2026-07-02 06:00:00', '操场',                   100, 0,   'REJECTED', NULL,    5),
(18, 9,  '校际足球友谊赛',                '与邻校约定友谊赛，申请场地及交通经费',                   '比赛',   '2026-07-05 14:00:00', '2026-07-05 17:00:00', '操场主席台区域',         200, 0,   'REJECTED', NULL,    6);

-- ==================== 活动报名记录 ====================
INSERT INTO activity_enroll (activity_id, user_id, status) VALUES
(1,9,'CHECKED_IN'),  (1,10,'ENROLLED'), (1,11,'ENROLLED'), (1,12,'ENROLLED'),
(2,9,'CHECKED_IN'),  (2,10,'ENROLLED'), (2,11,'ENROLLED'),
(3,9,'ENROLLED'),    (3,10,'ENROLLED'), (3,11,'ENROLLED'), (3,12,'ENROLLED'),
(4,9,'ENROLLED'),    (4,10,'ENROLLED'),
(5,9,'ENROLLED'),    (5,10,'ENROLLED'), (5,11,'ENROLLED'),
(6,9,'ENROLLED'),    (6,12,'ENROLLED'),
(7,10,'ENROLLED'),   (7,12,'ENROLLED'),
(11,9,'CHECKED_IN'), (11,10,'CHECKED_IN'), (11,11,'CHECKED_IN'),
(12,9,'CHECKED_IN'), (12,10,'CHECKED_IN'), (12,12,'CHECKED_IN'),
(13,10,'CHECKED_IN'), (13,11,'CHECKED_IN'),
(14,9,'CHECKED_IN'), (14,12,'CHECKED_IN');

-- ==================== 签到记录 ====================
INSERT INTO activity_checkin (activity_id, user_id) VALUES
(1,9), (2,9), (11,9), (11,10), (11,11), (12,9), (12,10), (12,12), (13,10), (13,11), (14,9), (14,12);

-- ==================== 经费记录 (10条) ====================
INSERT INTO fund_record (id, club_id, type, amount, category, description, status, approved_by) VALUES
(1, 1, 'EXPENSE', 5000.00,  '活动经费', '编程马拉松奖品、茶歇、宣传物料采购',                    'APPROVED', 1),
(2, 1, 'INCOME',  8000.00,  '社团拨款', '2026年春季学期社团活动经费拨款',                      'APPROVED', 1),
(3, 2, 'EXPENSE', 3000.00,  '场地费用', '随机舞蹈活动音响设备租赁及场地布置',                  'APPROVED', 1),
(4, 4, 'EXPENSE', 2500.00,  '奖品礼品', '篮球联赛冠军奖杯及MVP定制球鞋',                       'APPROVED', 1),
(5, 6, 'EXPENSE', 4000.00,  '设备采购', '音乐节舞台灯光租赁及乐器调音费用',                    'PENDING',  NULL),
(6, 7, 'EXPENSE', 1500.00,  '宣传物料', '摄影大赛获奖作品冲印及展板制作',                      'PENDING',  NULL),
(7, 5, 'INCOME',  5000.00,  '社团拨款', '创业俱乐部路演活动专项拨款',                          'APPROVED', 1),
(8, 3, 'EXPENSE', 2000.00,  '交通差旅', '暑期支教团队往返交通费用申请',                        'PENDING',  NULL),
(9, 8, 'EXPENSE', 800.00,   '餐饮招待', '辩论赛评委及参赛选手午餐补贴',                        'REJECTED', 1),
(10,11, 'EXPENSE', 3500.00,  '场地费用', '《雷雨》公演舞台道具制作及服装租赁',                  'PENDING',  NULL);

-- ==================== 场地预约 (5条) ====================
INSERT INTO venue_booking (id, venue_id, booking_user_id, booking_date, start_time, end_time, purpose, status, approved_by) VALUES
(1, 1, 4, '2026-07-10', '09:00:00', '21:00:00', '编程马拉松活动场地',                 'APPROVED', 1),
(2, 7, 6, '2026-07-08', '18:00:00', '22:00:00', '篮球联赛总决赛',                     'APPROVED', 1),
(3, 4, 8, '2026-07-12', '17:00:00', '22:30:00', '夏日音乐节演出',                     'APPROVED', 1),
(4, 1, 8, '2026-07-28', '17:00:00', '22:00:00', '《雷雨》话剧公演',                  'PENDING',  NULL),
(5, 2, 6, '2026-07-25', '08:00:00', '18:00:00', '五人制足球赛开幕式及颁奖',           'PENDING',  NULL);

-- ==================== 物资借用 (4条) ====================
INSERT INTO resource_borrow (id, item_id, user_id, quantity, borrow_date, plan_return_date, status, approved_by) VALUES
(1, 1,  4, 2, '2026-07-09', '2026-07-12', 'APPROVED', 1),
(2, 3,  6, 5, '2026-07-07', '2026-07-09', 'BORROWING', 1),
(3, 12, 8, 2, '2026-07-11', '2026-07-13', 'PENDING',   NULL),
(4, 10, 11,3, '2026-07-06', '2026-07-10', 'APPROVED', 1);

-- ==================== 公告 (8条) ====================
INSERT INTO sys_announcement (id, title, content, is_top, created_by, create_time) VALUES
(1, '关于开展2026年春季学期社团注册工作的通知',
   '各社团负责人请注意：本学期社团注册工作已于7月1日正式开始，请各社团在7月15日前登录系统完成以下事项：\n1. 更新社团基本信息（名称、简介、指导教师）\n2. 提交本学期活动计划\n3. 更新社团成员名单\n逾期未完成注册的社团将暂停活动资格。如有疑问请联系校团委张老师。', 1, 1, '2026-07-01 09:00:00'),

(2, '关于规范社团活动经费申请流程的通知',
   '为进一步规范社团经费管理，即日起所有社团活动经费申请须通过系统提交，审批流程如下：\n1. 社长提交经费申请（附详细预算表）\n2. 指导教师审核\n3. 校团委审批（金额>5000元需加签）\n请各社团严格遵守，避免因流程问题影响活动开展。', 1, 1, '2026-07-03 14:30:00'),

(3, '2026年暑期社会实践项目征集通知',
   '为鼓励学生积极参与社会实践，现面向全校征集暑期社会实践项目，方向包括：乡村振兴调研、科技助农、支教助学、环保公益等。\n入选项目将获得2000-10000元经费资助。请有意向的团队于7月20日前提交项目申报书。', 0, 1, '2026-07-05 10:00:00'),

(4, '关于大学生活动中心场地预约规则调整的说明',
   '为合理分配场地资源，即日起场地预约做如下调整：\n1. 大型活动（>200人）需提前14天预约\n2. 常规活动提前7天预约\n3. 每人每月最多预约4次\n请各社团合理安排活动时间，避免场地使用冲突。', 0, 1, '2026-07-06 08:00:00'),

(5, '校园十佳歌手大赛报名正式启动',
   '一年一度的校园十佳歌手大赛来了！报名时间：7月10日-7月25日。海选：8月1日-3日，复赛：8月8日，决赛：8月15日。\n奖项设置：冠军2000元+奖杯、亚军1000元、季军500元、最佳人气奖。\n报名方式：登录系统→活动管理→校园十佳歌手大赛→报名参加。', 0, 1, '2026-07-06 16:00:00'),

(6, '2026年春季学期优秀社团评选结果公示',
   '经社团自评、材料审核、现场答辩、学生投票四个环节，现将评选结果公示如下：\n五星社团（2个）：计算机协会、志愿者协会\n四星社团（3个）：舞蹈社、音乐社团、篮球协会\n三星社团（5个）：辩论队、摄影协会、足球社、话剧社、数学建模协会\n公示期：7月6日-7月12日，如有异议请向校团委反馈。', 0, 1, '2026-07-06 12:00:00'),

(7, '关于暑期留校学生社团活动安全管理的提醒',
   '暑期将至，请各社团负责人注意以下事项：\n1. 暑期外出活动须购买保险\n2. 大型活动须配备安全员\n3. 涉及校外人员参与的活动需提前报备\n4. 高温天气户外活动注意防暑降温\n安全第一，祝大家度过一个充实安全的暑假！', 0, 1, '2026-07-06 08:30:00'),

(8, '社团管理系统使用指南（2026版）',
   '为帮助各社团更好地使用本系统，校团委整理了以下常用操作指南：\n1. 活动发布：登录→活动管理→发布新活动→填写信息→提交审批\n2. 场地预约：登录→场地预约→选择场地→填写时间→提交\n3. 物资借用：登录→物资管理→选择物资→填写数量及归还日期\n4. 经费申请：登录→经费管理→申请经费→选择社团→填写金额及用途\n如遇系统问题请联系管理员。', 0, 1, '2026-07-02 09:00:00');

-- =====================================================
-- 完成
-- =====================================================
SELECT 'Database initialized successfully!' AS result;
SELECT COUNT(*) AS user_count FROM sys_user;
SELECT COUNT(*) AS club_count FROM club;
SELECT COUNT(*) AS activity_count FROM activity;
SELECT COUNT(*) AS venue_count FROM venue;
SELECT COUNT(*) AS resource_count FROM resource_item;
SELECT COUNT(*) AS announcement_count FROM sys_announcement;

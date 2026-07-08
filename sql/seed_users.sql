-- 批量创建用户 + 分配社团成员（让数据更真实）
-- 密码统一 123456，BCrypt hash
SET @pwd = '$2a$10$yt1kw9Gf5gShfBwpPvBe4.n4ZUD1hItYSTO71PXZzDEgObjMSwz7i';

-- ==================== 新增 48 名学生（总量扩到 60 人） ====================
INSERT INTO sys_user (username, password, real_name, role, college, status) VALUES
('s2024001', @pwd, '陈思远', 'STUDENT', '计算机学院', 1),
('s2024002', @pwd, '林雨桐', 'STUDENT', '计算机学院', 1),
('s2024003', @pwd, '黄浩然', 'STUDENT', '计算机学院', 1),
('s2024004', @pwd, '吴佳琪', 'STUDENT', '计算机学院', 1),
('s2024005', @pwd, '周子涵', 'STUDENT', '艺术学院', 1),
('s2024006', @pwd, '赵文博', 'STUDENT', '艺术学院', 1),
('s2024007', @pwd, '孙雨萱', 'STUDENT', '艺术学院', 1),
('s2024008', @pwd, '刘子轩', 'STUDENT', '艺术学院', 1),
('s2024009', @pwd, '张若琳', 'STUDENT', '体育学院', 1),
('s2024010', @pwd, '王子豪', 'STUDENT', '体育学院', 1),
('s2024011', @pwd, '陈奕迅', 'STUDENT', '体育学院', 1),
('s2024012', @pwd, '李雪琴', 'STUDENT', '体育学院', 1),
('s2024013', @pwd, '赵一鸣', 'STUDENT', '经管学院', 1),
('s2024014', @pwd, '钱思雨', 'STUDENT', '经管学院', 1),
('s2024015', @pwd, '孙子涵', 'STUDENT', '经管学院', 1),
('s2024016', @pwd, '周杰',   'STUDENT', '经管学院', 1),
('s2024017', @pwd, '吴思远', 'STUDENT', '文学院', 1),
('s2024018', @pwd, '郑雨晴', 'STUDENT', '文学院', 1),
('s2024019', @pwd, '王诗涵', 'STUDENT', '文学院', 1),
('s2024020', @pwd, '冯子轩', 'STUDENT', '文学院', 1),
('s2024021', @pwd, '陈思琪', 'STUDENT', '理学院', 1),
('s2024022', @pwd, '褚昊天', 'STUDENT', '理学院', 1),
('s2024023', @pwd, '卫若兰', 'STUDENT', '理学院', 1),
('s2024024', @pwd, '蒋雨辰', 'STUDENT', '理学院', 1),
('s2024025', @pwd, '沈嘉豪', 'STUDENT', '计算机学院', 1),
('s2024026', @pwd, '韩雨桐', 'STUDENT', '计算机学院', 1),
('s2024027', @pwd, '杨博文', 'STUDENT', '计算机学院', 1),
('s2024028', @pwd, '朱思涵', 'STUDENT', '艺术学院', 1),
('s2024029', @pwd, '秦子墨', 'STUDENT', '艺术学院', 1),
('s2024030', @pwd, '尤雨馨', 'STUDENT', '艺术学院', 1),
('s2024031', @pwd, '许文韬', 'STUDENT', '体育学院', 1),
('s2024032', @pwd, '何雨菲', 'STUDENT', '体育学院', 1),
('s2024033', @pwd, '吕嘉诚', 'STUDENT', '经管学院', 1),
('s2024034', @pwd, '施雨萱', 'STUDENT', '经管学院', 1),
('s2024035', @pwd, '张昊然', 'STUDENT', '计算机学院', 1),
('s2024036', @pwd, '孔思源', 'STUDENT', '计算机学院', 1),
('s2024037', @pwd, '曹宇轩', 'STUDENT', '理学院', 1),
('s2024038', @pwd, '严雨婷', 'STUDENT', '理学院', 1),
('s2024039', @pwd, '华博文', 'STUDENT', '文学院', 1),
('s2024040', @pwd, '金雨辰', 'STUDENT', '文学院', 1),
('s2024041', @pwd, '魏思远', 'STUDENT', '体育学院', 1),
('s2024042', @pwd, '陶雨晴', 'STUDENT', '体育学院', 1),
('s2024043', @pwd, '姜浩然', 'STUDENT', '经管学院', 1),
('s2024044', @pwd, '戚思雨', 'STUDENT', '经管学院', 1),
('s2024045', @pwd, '谢博文', 'STUDENT', '艺术学院', 1),
('s2024046', @pwd, '邹雨萱', 'STUDENT', '艺术学院', 1),
('s2024047', @pwd, '喻子轩', 'STUDENT', '计算机学院', 1),
('s2024048', @pwd, '柏雨馨', 'STUDENT', '计算机学院', 1);

-- ==================== 给每个社团分配 5~12 名成员 ====================
-- id从13开始是新用户（12个原始用户id为1-12），新用户id从13到60

-- 计算机协会(id=1): 加 10 名计算机学院学生
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(1,13,'MEMBER',1),(1,14,'MEMBER',1),(1,15,'MEMBER',1),(1,16,'MEMBER',1),
(1,25,'MEMBER',1),(1,26,'MEMBER',1),(1,27,'MEMBER',1),(1,35,'MEMBER',1),
(1,36,'MEMBER',1),(1,47,'MEMBER',1);

-- 舞蹈社(id=2): 加 8 名艺术学院学生
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(2,17,'MEMBER',1),(2,18,'MEMBER',1),(2,19,'MEMBER',1),(2,20,'MEMBER',1),
(2,28,'MEMBER',1),(2,29,'MEMBER',1),(2,45,'MEMBER',1),(2,46,'MEMBER',1);

-- 志愿者协会(id=3): 加 12 名分散各学院
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(3,13,'MEMBER',1),(3,21,'MEMBER',1),(3,22,'MEMBER',1),(3,30,'MEMBER',1),
(3,31,'MEMBER',1),(3,32,'MEMBER',1),(3,33,'MEMBER',1),(3,34,'MEMBER',1),
(3,37,'MEMBER',1),(3,38,'MEMBER',1),(3,43,'MEMBER',1),(3,44,'MEMBER',1);

-- 篮球协会(id=4): 加 10 名体育学院学生
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(4,23,'MEMBER',1),(4,24,'MEMBER',1),(4,25,'MEMBER',1),(4,31,'MEMBER',1),
(4,32,'MEMBER',1),(4,34,'MEMBER',1),(4,35,'MEMBER',1),(4,41,'MEMBER',1),
(4,42,'MEMBER',1),(4,48,'MEMBER',1);

-- 创新创业俱乐部(id=5): 加 7 名经管学院
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(5,15,'MEMBER',1),(5,22,'MEMBER',1),(5,29,'MEMBER',1),(5,33,'MEMBER',1),
(5,36,'MEMBER',1),(5,43,'MEMBER',1),(5,44,'MEMBER',1);

-- 音乐社团(id=6): 加 9 名各学院
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(6,14,'MEMBER',1),(6,17,'MEMBER',1),(6,23,'MEMBER',1),(6,26,'MEMBER',1),
(6,30,'MEMBER',1),(6,37,'MEMBER',1),(6,39,'MEMBER',1),(6,45,'MEMBER',1),
(6,46,'MEMBER',1);

-- 摄影协会(id=7): 加 8 名
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(7,16,'MEMBER',1),(7,18,'MEMBER',1),(7,21,'MEMBER',1),(7,27,'MEMBER',1),
(7,34,'MEMBER',1),(7,38,'MEMBER',1),(7,40,'MEMBER',1),(7,47,'MEMBER',1);

-- 辩论队(id=8): 加 8 名
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(8,13,'MEMBER',1),(8,19,'MEMBER',1),(8,25,'MEMBER',1),(8,32,'MEMBER',1),
(8,36,'MEMBER',1),(8,39,'MEMBER',1),(8,43,'MEMBER',1),(8,48,'MEMBER',1);

-- 足球社(id=9): 加 10 名体育+经管
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(9,15,'MEMBER',1),(9,22,'MEMBER',1),(9,24,'MEMBER',1),(9,31,'MEMBER',1),
(9,33,'MEMBER',1),(9,35,'MEMBER',1),(9,38,'MEMBER',1),(9,41,'MEMBER',1),
(9,42,'MEMBER',1),(9,44,'MEMBER',1);

-- 环保协会(id=10): 加 8 名
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(10,14,'MEMBER',1),(10,20,'MEMBER',1),(10,26,'MEMBER',1),(10,28,'MEMBER',1),
(10,34,'MEMBER',1),(10,39,'MEMBER',1),(10,45,'MEMBER',1),(10,47,'MEMBER',1);

-- 话剧社(id=11): 加 7 名文学院
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(11,17,'MEMBER',1),(11,19,'MEMBER',1),(11,27,'MEMBER',1),(11,37,'MEMBER',1),
(11,40,'MEMBER',1),(11,46,'MEMBER',1),(11,48,'MEMBER',1);

-- 数学建模协会(id=12): 加 8 名理学院+计算机
INSERT INTO club_member (club_id, user_id, role, status) VALUES
(12,16,'MEMBER',1),(12,21,'MEMBER',1),(12,23,'MEMBER',1),(12,29,'MEMBER',1),
(12,35,'MEMBER',1),(12,37,'MEMBER',1),(12,40,'MEMBER',1),(12,42,'MEMBER',1);

-- ==================== 更新 member_count 为真实计数 ====================
UPDATE club c SET c.member_count = (
  SELECT COUNT(*) FROM club_member cm WHERE cm.club_id = c.id AND cm.status = 1
);

SELECT 'Users seeded successfully!' AS result;
SELECT COUNT(*) AS total_users FROM sys_user;
SELECT c.id, c.name, c.member_count FROM club c ORDER BY c.id;

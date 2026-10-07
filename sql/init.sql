-- ============================================
-- 校园社团报名通 · 数据库初始化脚本
-- 适用：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================

DROP DATABASE IF EXISTS club_enroll;
CREATE DATABASE club_enroll
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE club_enroll;

-- ============================================
-- 1. user 用户表（学生 + 管理员公用一张表，用 role 区分）
-- ============================================
CREATE TABLE user (
                      id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                      username    VARCHAR(32)  NOT NULL                COMMENT '用户名（唯一）',
                      password    VARCHAR(64)  NOT NULL                COMMENT '密码（BCrypt 哈希）',
                      role        VARCHAR(16)  NOT NULL                COMMENT '角色：student / admin',
                      real_name   VARCHAR(32)  NOT NULL                COMMENT '真实姓名',
                      phone       VARCHAR(11)  NOT NULL                COMMENT '手机号（11位）',
                      created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
                      PRIMARY KEY (id),
                      UNIQUE KEY uk_username (username),
                      KEY idx_role (role),
                      KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表（学生/管理员）';

-- ============================================
-- 2. club 社团表
-- ============================================
CREATE TABLE club (
                      id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                      name            VARCHAR(64)   NOT NULL                COMMENT '社团名（唯一）',
                      category        VARCHAR(16)   NOT NULL                COMMENT '分类：学术/体育/文艺/公益',
                      intro           VARCHAR(1000) NOT NULL                COMMENT '简介',
                      leader          VARCHAR(32)   NOT NULL                COMMENT '负责人姓名',
                      max_members     INT           NOT NULL                COMMENT '人数上限',
                      current_members INT           NOT NULL DEFAULT 0      COMMENT '当前人数（审批通过自动+1）',
                      is_active       TINYINT(1)    NOT NULL DEFAULT 1      COMMENT '是否在招：1=招新中 0=已下架',
                      created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      PRIMARY KEY (id),
                      UNIQUE KEY uk_club_name (name),
                      KEY idx_category (category),
                      KEY idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社团表';

-- ============================================
-- 3. enrollment 报名记录表
-- ============================================
CREATE TABLE enrollment (
                            id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            user_id       BIGINT       NOT NULL                COMMENT '学生ID',
                            club_id       BIGINT       NOT NULL                COMMENT '社团ID',
                            reason        VARCHAR(200) NOT NULL                COMMENT '报名理由（10-200字）',
                            status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1=待审 2=通过 3=拒绝',
                            apply_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
                            review_time   DATETIME     NULL                    COMMENT '审批时间（未审为NULL）',
                            reviewer_id   BIGINT       NULL                    COMMENT '审批人ID（未审为NULL）',
                            review_remark VARCHAR(200) NULL                    COMMENT '审批备注（通过或拒绝原因）',
                            PRIMARY KEY (id),
    -- 同一学生对同一社团不可重复报名
                            UNIQUE KEY uk_user_club (user_id, club_id),
                            KEY idx_status (status),
                            KEY idx_club_id (club_id),
                            KEY idx_apply_time (apply_time),
                            CONSTRAINT fk_enroll_user     FOREIGN KEY (user_id)     REFERENCES user(id),
                            CONSTRAINT fk_enroll_club     FOREIGN KEY (club_id)     REFERENCES club(id),
                            CONSTRAINT fk_enroll_reviewer FOREIGN KEY (reviewer_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名记录表';

-- ============================================
-- 4. notice 公告表
-- ============================================
CREATE TABLE notice (
                        id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                        club_id      BIGINT      NOT NULL                COMMENT '社团ID',
                        title        VARCHAR(64) NOT NULL                COMMENT '公告标题',
                        content      TEXT        NOT NULL                COMMENT '公告正文',
                        publisher_id BIGINT      NOT NULL                COMMENT '发布人ID（admin）',
                        created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
                        PRIMARY KEY (id),
                        KEY idx_club_id (club_id),
                        KEY idx_created_at (created_at),
                        CONSTRAINT fk_notice_club      FOREIGN KEY (club_id)      REFERENCES club(id),
                        CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ============================================
-- 测试数据初始化
-- ============================================
USE club_enroll;

-- ============ 1. 用户：3 学生 + 1 管理员 ============
-- 仅本地演示：chen/zhang/li 的密码为 123456，admin001 为 admin123；数据库存储 BCrypt 哈希。生产环境请另建账号。
INSERT INTO user (username, password, role, real_name, phone) VALUES
                                                                  ('chen',      '$2a$12$Obt421J7TJdTpWZHGUpmjOY5ssl/MZXFvFJkRA4AfW43jStUu65tC', 'student', '陈同学', '13800138001'),
                                                                  ('zhang',     '$2a$12$StDS1UtpGBnrOD0QsGmYJuFLG24pLUna1aw9So.Idkp4dUec4Dc0y', 'student', '张同学', '13800138002'),
                                                                  ('li',        '$2a$12$6WoBZQ..t95Ux66z0iZc9..Sx8lmHKx8cPvweXZ/3.fKM79sshcby', 'student', '李同学', '13800138003'),
                                                                  ('admin001',  '$2a$12$csNuzr9wUgxszLfftYNnPOaFhHluXgiTqubipxQBky/6ceQkrot4a', 'admin', '社联部张老师', '13900139001');

-- ============ 2. 社团：5 个，含分类多样 ============
INSERT INTO club (name, category, intro, leader, max_members, current_members, is_active) VALUES
                                                                                              ('天文协会',
                                                                                               '学术',
                                                                                               '夜观天象、月亮观测、流星雨夜游山顶。本社团成立于 2015 年，已带新同学露营观星 12 次。配备 5 台望远镜，可在学校天文台借用。',
                                                                                               '王雪',
                                                                                               30, 18, 1),

                                                                                              ('街舞社',
                                                                                               '文艺',
                                                                                               '从 popping、hiphop、breaking 到中国风街舞，每周三、五晚训练，参加校外赛事每年 ≥ 6 场。零基础也欢迎，前辈带后辈。',
                                                                                               '李志',
                                                                                               50, 42, 1),

                                                                                              ('羽毛球俱乐部',
                                                                                               '体育',
                                                                                               '每周六上午体育馆固定 4 块场地，新手老手分组打。每年举办春秋两届校内积分赛，前三名有校外品牌赞助奖品。',
                                                                                               '赵磊',
                                                                                               80, 75, 1),

                                                                                              ('支教联盟',
                                                                                               '公益',
                                                                                               '每月一次周末进山区学校支教，主科辅导 + 美术 / 体育 / 心理课。寒暑假组织为期 15 天的集中支教项目。',
                                                                                               '钱小燕',
                                                                                               40, 12, 1),

                                                                                              ('辩论协会',
                                                                                               '学术',
                                                                                               '主攻华语辩论赛，曾参加海峡两岸辩论邀请赛 4 届。每周四晚训练，新生 9 月会有一次模拟赛选拔。',
                                                                                               '吴启明',
                                                                                               25, 25, 0);     -- 已下架样例：人数已满 + 不在招

-- ============ 3. 报名：8 条记录，覆盖三种状态 ============
-- 陈同学（id=1）报了 3 个社团
INSERT INTO enrollment (user_id, club_id, reason, status, apply_time, review_time, reviewer_id, review_remark) VALUES
                                                                                                                   (1, 1, '我从小学二年级就用爸爸的望远镜看月亮，初二买了第一台天文望远镜，想加入跟大家一起观测。', 2, '2026-05-20 10:00:00', '2026-05-21 14:30:00', 4, '兴趣浓厚，欢迎加入'),
                                                                                                                   (1, 2, '我对街舞完全零基础但很想学，希望能从最基本开始。', 1, '2026-05-22 16:00:00', NULL, NULL, NULL),
                                                                                                                   (1, 4, '高中三年都在做留守儿童家教志愿者，希望大学继续做这件事。', 1, '2026-05-23 09:30:00', NULL, NULL, NULL);

-- 张同学（id=2）报了 2 个，1 通过 1 拒绝
INSERT INTO enrollment (user_id, club_id, reason, status, apply_time, review_time, reviewer_id, review_remark) VALUES
                                                                                                                   (2, 3, '初中羽毛球校队成员，校市级亚军一次。希望进俱乐部继续提高。', 2, '2026-05-18 19:00:00', '2026-05-19 11:00:00', 4, '校队经历优秀，直接通过'),
                                                                                                                   (2, 2, '没有舞蹈基础，但有节奏感。', 3, '2026-05-19 20:00:00', '2026-05-20 10:00:00', 4, '近期人员已招满，下学期欢迎重新报名');

-- 李同学（id=3）报了 3 个
INSERT INTO enrollment (user_id, club_id, reason, status, apply_time, review_time, reviewer_id, review_remark) VALUES
                                                                                                                   (3, 1, '高中参加过两次学校天文社的活动，对深空天体很感兴趣。', 1, '2026-05-25 11:00:00', NULL, NULL, NULL),
                                                                                                                   (3, 4, '我家来自云南山区，深知教育资源不均，想做点事。', 2, '2026-05-21 14:00:00', '2026-05-22 16:00:00', 4, '动机真诚'),
                                                                                                                   (3, 3, '想学打球，零基础。', 1, '2026-05-26 08:00:00', NULL, NULL, NULL);

-- ============ 4. 公告：4 条 ============
INSERT INTO notice (club_id, title, content, publisher_id, created_at) VALUES
                                                                           (1, '6 月 18 日狮子座流星雨观测活动报名',
                                                                            '亲爱的天文协会成员们：本周六（6 月 18 日）晚 9 点至次日凌晨 3 点，我们将在校天文台进行狮子座流星雨观测。请准备保暖衣物、手电筒。统一在天文台门口集合。已报名通过的同学请到 QQ 群签到。',
                                                                            4, '2026-06-10 14:00:00'),

                                                                           (2, '街舞社年度汇演彩排通知',
                                                                            '汇演彩排时间：6 月 25 日 19:00-22:00，地点：学生活动中心 3 号厅。所有正式社员必须参加，请提前到 10 分钟。',
                                                                            4, '2026-06-12 10:00:00'),

                                                                           (3, '羽毛球春季积分赛颁奖',
                                                                            '本届春季积分赛圆满结束！前三名：王明、刘洋、孙小路。颁奖时间：6 月 20 日 12:00 在体育馆门口。奖品由××体育用品赞助。',
                                                                            4, '2026-06-13 16:30:00'),

                                                                           (4, '7 月集中支教招募志愿者',
                                                                            '本社团将于 7 月 15 日—7 月 30 日组织赴云南宁蒗县大兴小学的集中支教，需志愿者 15 名。报名截止 6 月 30 日，请已通过审批的成员到 QQ 群报名。',
                                                                            4, '2026-06-14 09:00:00');

-- 在现有 club_enroll 库执行；只新增表，不删除原有数据。新安装也应在 init.sql 后执行。
CREATE TABLE IF NOT EXISTS student_identity (
 user_id BIGINT PRIMARY KEY,
 student_no VARCHAR(32) COLLATE utf8mb4_bin NOT NULL,
 real_name VARCHAR(32) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 approved_no VARCHAR(32) COLLATE utf8mb4_bin GENERATED ALWAYS AS (CASE WHEN status='APPROVED' THEN student_no ELSE NULL END) STORED,
 version INT NOT NULL DEFAULT 1,
 remark VARCHAR(300), reviewer_id BIGINT, reviewed_at DATETIME,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_approved_student (approved_no),
 FOREIGN KEY(user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS identity_audit (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 student_no VARCHAR(32) NOT NULL, real_name VARCHAR(32) NOT NULL,
 action VARCHAR(16) NOT NULL, remark VARCHAR(300) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS club_manager (
 club_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 PRIMARY KEY(club_id,user_id), FOREIGN KEY(club_id) REFERENCES club(id), FOREIGN KEY(user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS visit_activity (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, club_id BIGINT NOT NULL, notice_id BIGINT NOT NULL UNIQUE,
 title VARCHAR(64) NOT NULL, content TEXT NOT NULL, location VARCHAR(200) NOT NULL,
 starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL,
 opens_at DATETIME NOT NULL, closes_at DATETIME NOT NULL, cancel_until DATETIME NOT NULL,
 capacity INT NOT NULL, booked_count INT NOT NULL DEFAULT 0,
 status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED', publisher_id BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(capacity > 0 AND booked_count >= 0 AND booked_count <= capacity),
 FOREIGN KEY(club_id) REFERENCES club(id), FOREIGN KEY(notice_id) REFERENCES notice(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS visit_reservation (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, activity_id BIGINT NOT NULL, user_id BIGINT NOT NULL,
 student_no VARCHAR(32) COLLATE utf8mb4_bin NOT NULL, real_name VARCHAR(32) NOT NULL,
 status VARCHAR(16) NOT NULL, version INT NOT NULL DEFAULT 1,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, cancelled_at DATETIME,
 UNIQUE KEY uk_visit_student(activity_id,student_no),
 FOREIGN KEY(activity_id) REFERENCES visit_activity(id), FOREIGN KEY(user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 保存操作结果，防止取消后的旧预约请求重试再次占名额。
CREATE TABLE IF NOT EXISTS visit_request (
 user_id BIGINT NOT NULL, request_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
 activity_id BIGINT NOT NULL, reservation_id BIGINT NOT NULL,
 PRIMARY KEY(user_id,request_id), FOREIGN KEY(reservation_id) REFERENCES visit_reservation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 为新增报名保存已核验身份，保留原 enrollment 表及历史数据。
CREATE TABLE IF NOT EXISTS enrollment_identity (
 enrollment_id BIGINT PRIMARY KEY, club_id BIGINT NOT NULL,
 student_no VARCHAR(32) COLLATE utf8mb4_bin NOT NULL, real_name VARCHAR(32) NOT NULL,
 UNIQUE KEY uk_enroll_student(club_id,student_no),
 FOREIGN KEY(enrollment_id) REFERENCES enrollment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

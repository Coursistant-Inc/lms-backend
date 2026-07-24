-- Group module for lms_v2 (GroupSet → CourseGroup → GroupMembership + audit)

CREATE TABLE IF NOT EXISTS group_set (
  id INT NOT NULL AUTO_INCREMENT,
  course_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  default_capacity INT NOT NULL,
  join_opens_at DATETIME NULL,
  join_closes_at DATETIME NULL,
  locked TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_group_set_course (course_id),
  CONSTRAINT fk_group_set_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT chk_group_set_default_capacity CHECK (default_capacity >= 1 AND default_capacity <= 200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_group (
  id INT NOT NULL AUTO_INCREMENT,
  group_set_id INT NOT NULL,
  course_id INT NOT NULL,
  name VARCHAR(255) NOT NULL,
  capacity_override INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_course_group_set (group_set_id),
  KEY idx_course_group_course (course_id),
  CONSTRAINT fk_course_group_set FOREIGN KEY (group_set_id) REFERENCES group_set (id) ON DELETE CASCADE,
  CONSTRAINT fk_course_group_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT chk_course_group_capacity_override CHECK (
    capacity_override IS NULL OR (capacity_override >= 1 AND capacity_override <= 200)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_membership (
  id INT NOT NULL AUTO_INCREMENT,
  group_id INT NOT NULL,
  group_set_id INT NOT NULL,
  course_id INT NOT NULL,
  user_id INT NOT NULL,
  joined_at DATETIME NOT NULL,
  added_by_type ENUM('Self', 'Staff') NOT NULL,
  added_by_user_id INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_membership_set_user (group_set_id, user_id),
  KEY idx_membership_group (group_id),
  KEY idx_membership_course_user (course_id, user_id),
  CONSTRAINT fk_membership_group FOREIGN KEY (group_id) REFERENCES course_group (id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_set FOREIGN KEY (group_set_id) REFERENCES group_set (id) ON DELETE CASCADE,
  CONSTRAINT fk_membership_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE RESTRICT,
  CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES user (id),
  CONSTRAINT fk_membership_added_by FOREIGN KEY (added_by_user_id) REFERENCES user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS group_membership_audit (
  id INT NOT NULL AUTO_INCREMENT,
  tenant_id INT NOT NULL,
  course_id INT NOT NULL,
  group_set_id INT NULL,
  group_id INT NULL,
  target_user_id INT NOT NULL,
  actor_type ENUM('USER', 'ADMIN', 'SYSTEM') NOT NULL,
  actor_user_id INT NULL,
  action VARCHAR(64) NOT NULL,
  before_json TEXT NULL,
  after_json TEXT NULL,
  detail_json TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_group_membership_audit_course (course_id),
  KEY idx_group_membership_audit_set (group_set_id),
  KEY idx_group_membership_audit_target (target_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

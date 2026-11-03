-- =============================================
-- NullBot Database Schema (SQLite)
-- Mirrors schema-mysql.sql. Applied automatically.
-- =============================================

-- 1. Config
CREATE TABLE IF NOT EXISTS `config` (
    `id`            INTEGER         PRIMARY KEY AUTOINCREMENT,
    `key`           VARCHAR(100)    NOT NULL,
    `value`         TEXT            DEFAULT NULL,
    `description`   VARCHAR(255)    DEFAULT NULL,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_config_key` ON `config` (`key`);

-- 2. User
CREATE TABLE IF NOT EXISTS `user` (
    `id`                      VARCHAR(64)  NOT NULL PRIMARY KEY,
    `name`                    VARCHAR(255) NOT NULL,
    `access`                  TINYINT      NOT NULL DEFAULT 0,
    `private_cmd_enabled`     BOOLEAN      NOT NULL DEFAULT 0,
    `private_ai_chat_enabled` BOOLEAN      NOT NULL DEFAULT 0
);

-- 3. User Setting
CREATE TABLE IF NOT EXISTS `user_setting` (
    `id`                         INTEGER     PRIMARY KEY AUTOINCREMENT,
    `user_id`                    VARCHAR(64) NOT NULL,
    `private_ai_prompt`          TEXT        DEFAULT NULL,
    `private_ai_effort`          TINYINT     NOT NULL DEFAULT 3,
    `private_ai_injection_guard` BOOLEAN     NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_user_setting_user_id` ON `user_setting` (`user_id`);

-- 4. Group
CREATE TABLE IF NOT EXISTS `group` (
    `id`              VARCHAR(64)     NOT NULL PRIMARY KEY,
    `name`            VARCHAR(255)    NOT NULL,
    `access`          TINYINT         NOT NULL DEFAULT 0,
    `cmd_enabled`     BOOLEAN         NOT NULL DEFAULT 1,
    `ai_chat_enabled` BOOLEAN         NOT NULL DEFAULT 1
);

-- 5. Group Setting
CREATE TABLE IF NOT EXISTS `group_setting` (
    `id`                   INTEGER     PRIMARY KEY AUTOINCREMENT,
    `group_id`             VARCHAR(64) NOT NULL,
    `language`             TINYINT     NOT NULL DEFAULT 0,
    `join_notice_enabled`  BOOLEAN     NOT NULL DEFAULT 0,
    `leave_notice_enabled` BOOLEAN     NOT NULL DEFAULT 0,
    `anti_recall_enabled`  BOOLEAN     NOT NULL DEFAULT 0,
    `limit_enabled`        BOOLEAN     NOT NULL DEFAULT 1,
    `limit_scope`          TINYINT     NOT NULL DEFAULT 0,
    `limit_capacity`       INT         NOT NULL DEFAULT 5,
    `limit_refill`         INT         NOT NULL DEFAULT 2,
    `limit_interval`       INT         NOT NULL DEFAULT 1,
    `ai_prompt`            TEXT        DEFAULT NULL,
    `ai_effort`            TINYINT     NOT NULL DEFAULT 3,
    `ai_injection_guard`   BOOLEAN     NOT NULL DEFAULT 1,
    `ai_auto_chat_enabled` BOOLEAN     NOT NULL DEFAULT 0,
    `ai_chat_frequency`    DOUBLE      NOT NULL DEFAULT 0.001
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_group_setting_group_id` ON `group_setting` (`group_id`);

-- 6. Group Keyword
CREATE TABLE IF NOT EXISTS `group_keyword` (
    `id`            INTEGER         PRIMARY KEY AUTOINCREMENT,
    `group_id`      VARCHAR(64)     NOT NULL,
    `keyword`       VARCHAR(255)    NOT NULL,
    `cmd`           VARCHAR(100)    NOT NULL,
    `args`          TEXT            DEFAULT NULL,
    `enabled`       BOOLEAN         NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_group_keyword` ON `group_keyword` (`group_id`, `keyword`);

-- 7. Command Disable
CREATE TABLE IF NOT EXISTS `cmd_disable` (
    `id`            INTEGER         PRIMARY KEY AUTOINCREMENT,
    `chat_type`     TINYINT         NOT NULL,              -- 0 = group, 1 = private
    `chat_id`       VARCHAR(64)     NOT NULL,              -- group id or private user id
    `user_id`       VARCHAR(64)     NOT NULL,              -- target user; '*' for whole group
    `cmd`           VARCHAR(100)    NOT NULL,
    `expires_at`    DATETIME        DEFAULT NULL           -- NULL means permanent
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_cmd_disable`
    ON `cmd_disable` (`chat_type`, `chat_id`, `cmd`, `user_id`);

-- 8. File
CREATE TABLE IF NOT EXISTS `file` (
    `id`            INTEGER         PRIMARY KEY AUTOINCREMENT,
    `user_id`       VARCHAR(64)     NOT NULL,
    `original`      VARCHAR(255)    NOT NULL,
    `stored`        VARCHAR(255)    NOT NULL,
    `description`   VARCHAR(255)    DEFAULT NULL,
    `size`          BIGINT          NOT NULL DEFAULT 0,
    `type`          TINYINT         NOT NULL DEFAULT 0,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS `idx_file_user_id` ON `file` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_file_type` ON `file` (`type`);
CREATE INDEX IF NOT EXISTS `idx_file_created_at` ON `file` (`created_at`);

-- 9. Message
CREATE TABLE IF NOT EXISTS `message` (
    `id`            VARCHAR(64)     NOT NULL PRIMARY KEY,
    `chat_id`       VARCHAR(64)     NOT NULL,
    `user_id`       VARCHAR(64)     NOT NULL,
    `role`          TINYINT         NOT NULL DEFAULT 0,
    `nickname`      VARCHAR(255)    NOT NULL,
    `content`       TEXT            DEFAULT NULL,
    `reasoning`     TEXT            DEFAULT NULL,
    `files`         TEXT            DEFAULT NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS `idx_message_chat_created` ON `message` (`chat_id`, `created_at`);
CREATE INDEX IF NOT EXISTS `idx_message_created_at` ON `message` (`created_at`);

-- 10. Quote
CREATE TABLE IF NOT EXISTS `quote` (
    `id`            INTEGER         PRIMARY KEY AUTOINCREMENT,
    `time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user_id`       VARCHAR(64)     NOT NULL,
    `nickname`      VARCHAR(255)    NOT NULL,
    `text`          VARCHAR(1000)   NOT NULL
);
CREATE INDEX IF NOT EXISTS `idx_quote_user_id` ON `quote` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_quote_time` ON `quote` (`time`);

-- 11. Cmd Usage
CREATE TABLE IF NOT EXISTS `cmd_usage` (
    `id`            INTEGER         PRIMARY KEY AUTOINCREMENT,
    `time`          DATE            NOT NULL,              -- statistics date
    `chat_type`     TINYINT         NOT NULL,              -- 0 = group, 1 = private
    `target_id`     VARCHAR(64)     NOT NULL,              -- raw group id or user id
    `user_id`       VARCHAR(64)     NOT NULL,              -- operator user id
    `cmd`           VARCHAR(100)    NOT NULL,              -- command name
    `count`         BIGINT          NOT NULL DEFAULT 0     -- usage count
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_cmd_usage_key`
    ON `cmd_usage` (`time`, `chat_type`, `target_id`, `user_id`, `cmd`);

-- 12. AI Usage
CREATE TABLE IF NOT EXISTS `ai_usage` (
    `id`             INTEGER         PRIMARY KEY AUTOINCREMENT,
    `time`           DATE            NOT NULL,              -- statistics date
    `chat_type`      TINYINT         NOT NULL,              -- 0 = group, 1 = private
    `target_id`      VARCHAR(64)     NOT NULL,              -- raw group id or user id
    `user_id`        VARCHAR(64)     NOT NULL,              -- triggering user id
    `source`         TINYINT         NOT NULL,              -- 0=group_chat, 1=group_auto_chat, 2=group_poke, 3=private_chat, 4=private_poke
    `count`          BIGINT          NOT NULL DEFAULT 0,    -- AI turn count
    `input_tokens`   BIGINT          NOT NULL DEFAULT 0,
    `output_tokens`  BIGINT          NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_ai_usage_key`
    ON `ai_usage` (`time`, `chat_type`, `target_id`, `user_id`, `source`);

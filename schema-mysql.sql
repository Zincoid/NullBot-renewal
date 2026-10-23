-- =============================================
-- NullBot Database Schema (MySQL)
-- Mirrors core/src/main/resources/schema/schema-sqlite.sql.
-- Applied manually.
-- Version: 1.0
-- Engine: InnoDB, Charset: utf8mb4
-- =============================================

CREATE DATABASE IF NOT EXISTS nullbot_renewal DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nullbot_renewal;

-- =============================================
-- 1. Config Table
-- =============================================
CREATE TABLE IF NOT EXISTS `config` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `key`           VARCHAR(100)    NOT NULL                 COMMENT 'Configuration key',
    `value`         TEXT            DEFAULT NULL             COMMENT 'Configuration value',
    `description`   VARCHAR(255)    DEFAULT NULL             COMMENT 'Description of the config item',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Config table';

-- =============================================
-- 2. Usage Table
-- =============================================
CREATE TABLE IF NOT EXISTS `usage` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `time`          DATE            NOT NULL                 COMMENT 'Statistics date',
    `group_id`      VARCHAR(64)     NOT NULL                 COMMENT 'Group platform id, 0 for private chat',
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Operator platform user id',
    `cmd`           VARCHAR(100)    NOT NULL                 COMMENT 'Command name',
    `count`         BIGINT          NOT NULL DEFAULT 0       COMMENT 'Usage count',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_time_group_user_cmd` (`time`, `group_id`, `user_id`, `cmd`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Daily command usage table';

-- =============================================
-- 3. File Table
-- =============================================
CREATE TABLE IF NOT EXISTS `file` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Uploader platform user id',
    `original`      VARCHAR(255)    NOT NULL                 COMMENT 'Original filename',
    `stored`        VARCHAR(255)    NOT NULL                 COMMENT 'Stored flat filename',
    `tag`           VARCHAR(255)    DEFAULT NULL             COMMENT 'File tag',
    `size`          BIGINT          NOT NULL DEFAULT 0       COMMENT 'File size in bytes',
    `type`          TINYINT         NOT NULL DEFAULT 0       COMMENT 'Business type: 0=IMG_COLL, 1=VID_COLL, 2=AUD_COLL, 3=CHAT, 4=MEME, 5=ACG',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File table';

-- =============================================
-- 4. User Table
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`                 VARCHAR(64)  NOT NULL                 COMMENT 'Platform user id',
    `name`               VARCHAR(255) NOT NULL                 COMMENT 'User nickname',
    `access`             TINYINT      NOT NULL DEFAULT 0       COMMENT 'Access level: -2=BANNED, -1=RESTRICTED, 0=NORMAL, 1=MANAGER, 2=ADMIN',
    `allow_private_chat` BOOLEAN      NOT NULL DEFAULT FALSE   COMMENT 'Whether private chat is allowed',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- =============================================
-- 5. User Setting Table
-- =============================================
CREATE TABLE IF NOT EXISTS `user_setting` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `user_id`            VARCHAR(64) NOT NULL                 COMMENT 'Platform user id',
    `ai_prompt`          TEXT        DEFAULT NULL             COMMENT 'Private chat system prompt, blank uses the global prompt',
    `ai_effort`          TINYINT     NOT NULL DEFAULT 3       COMMENT 'AI reasoning effort: 0=MINIMAL, 1=LOW, 2=MEDIUM, 3=HIGH, 4=XHIGH, 5=MAX, 6=ULTRA',
    `ai_injection_guard` BOOLEAN     NOT NULL DEFAULT TRUE    COMMENT 'Anti prompt injection',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User setting table';

-- =============================================
-- 6. Group Table
-- =============================================
CREATE TABLE IF NOT EXISTS `group` (
    `id`          VARCHAR(64)  NOT NULL                 COMMENT 'Platform group id',
    `name`        VARCHAR(255) NOT NULL                 COMMENT 'Group name',
    `access`      TINYINT      NOT NULL DEFAULT 0       COMMENT 'Access level: -2=BANNED, -1=RESTRICTED, 0=NORMAL, 1=MANAGER, 2=ADMIN',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Group table';

-- =============================================
-- 7. Group Setting Table
-- =============================================
CREATE TABLE IF NOT EXISTS `group_setting` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `group_id`           VARCHAR(64) NOT NULL                 COMMENT 'Platform group id',
    `language`           TINYINT     NOT NULL DEFAULT 0       COMMENT 'Reply language: 0=AUTO, 1=ZH, 2=EN',
    `limit_enabled`      BOOLEAN     NOT NULL DEFAULT TRUE    COMMENT 'Rate limit enabled',
    `limit_scope`        TINYINT     NOT NULL DEFAULT 0       COMMENT 'Rate limit scope: 0=USER, 1=GROUP, 2=CMD',
    `limit_capacity`     INT         NOT NULL DEFAULT 5       COMMENT 'Rate limit capacity',
    `limit_refill`       INT         NOT NULL DEFAULT 2       COMMENT 'Rate limit refill amount',
    `limit_interval`     INT         NOT NULL DEFAULT 1       COMMENT 'Rate limit refill interval in minutes',
    `ai_prompt`          TEXT        DEFAULT NULL             COMMENT 'Group system prompt, blank uses the global prompt',
    `ai_effort`          TINYINT     NOT NULL DEFAULT 3       COMMENT 'AI reasoning effort: 0=MINIMAL, 1=LOW, 2=MEDIUM, 3=HIGH, 4=XHIGH, 5=MAX, 6=ULTRA',
    `ai_injection_guard` BOOLEAN     NOT NULL DEFAULT TRUE    COMMENT 'Anti prompt injection',
    `ai_auto_chat`      BOOLEAN     NOT NULL DEFAULT FALSE   COMMENT 'AI auto chat',
    `ai_chat_frequency` DOUBLE      NOT NULL DEFAULT 0.001   COMMENT 'AI auto chat frequency (0..1)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Group setting table';

-- =============================================
-- 8. Group Keyword Table
-- =============================================
CREATE TABLE IF NOT EXISTS `group_keyword` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `group_id`      VARCHAR(64)     NOT NULL                 COMMENT 'Platform group id',
    `keyword`       VARCHAR(255)    NOT NULL                 COMMENT 'Keyword to match',
    `cmd`           VARCHAR(100)    NOT NULL                 COMMENT 'Command name to dispatch',
    `args`          JSON            DEFAULT NULL             COMMENT 'Command arguments (JSON array)',
    `enabled`       BOOLEAN         NOT NULL DEFAULT TRUE    COMMENT 'Rule enabled',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_keyword` (`group_id`, `keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Group keyword rule table';

-- =============================================
-- 9. Message Table
-- =============================================
CREATE TABLE IF NOT EXISTS `message` (
    `id`            VARCHAR(64)     NOT NULL                 COMMENT 'Platform message id',
    `chat_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Chat identifier',
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Sender platform user id',
    `role`          TINYINT         NOT NULL DEFAULT 0       COMMENT 'Message role: 0=USER, 1=ASSISTANT',
    `nickname`      VARCHAR(255)    NOT NULL                 COMMENT 'Sender nickname',
    `content`       TEXT            DEFAULT NULL             COMMENT 'Text content (nullable when files present)',
    `reasoning`     TEXT            DEFAULT NULL             COMMENT 'Assistant reasoning content for thinking models',
    `files`         JSON            DEFAULT NULL             COMMENT 'Attached file record ids (JSON array)',
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_chat_created` (`chat_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat message table';

-- =============================================
-- 10. Quote Table
-- =============================================
CREATE TABLE IF NOT EXISTS `quote` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    `time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record time',
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Author platform user id',
    `nickname`      VARCHAR(255)    NOT NULL                 COMMENT 'Author nickname',
    `text`          VARCHAR(1000)   NOT NULL                 COMMENT 'Quote content',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Quote table';

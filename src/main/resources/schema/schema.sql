-- =============================================
-- NullBot Database Schema
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
    `group_id`      VARCHAR(64)     NOT NULL                 COMMENT 'Group platform ID, 0 for private chat',
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Operator platform user ID',
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
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Uploader platform user ID',
    `original`      VARCHAR(255)    NOT NULL                 COMMENT 'Original file name',
    `stored`        VARCHAR(255)    NOT NULL                 COMMENT 'Stored flat file name',
    `description`   VARCHAR(255)    DEFAULT NULL             COMMENT 'File description',
    `size`          BIGINT          NOT NULL DEFAULT 0       COMMENT 'File size in bytes',
    `type`          TINYINT         NOT NULL DEFAULT 0       COMMENT 'Business type: 0=IMG_COLL, 1=VID_COLL, 2=AUD_COLL, 3=CHAT',
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
    `id`          VARCHAR(64)  NOT NULL                 COMMENT 'Platform user ID',
    `name`        VARCHAR(255) NOT NULL                 COMMENT 'User nickname',
    `access`      TINYINT      NOT NULL DEFAULT 0       COMMENT 'Access level: -2=BANNED, -1=RESTRICTED, 0=NORMAL, 1=MANAGER, 2=ADMIN',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User table';

-- =============================================
-- 5. Group Table
-- =============================================
CREATE TABLE IF NOT EXISTS `group` (
    `id`          VARCHAR(64)  NOT NULL                 COMMENT 'Platform group ID',
    `name`        VARCHAR(255) NOT NULL                 COMMENT 'Group name',
    `access`      TINYINT      NOT NULL DEFAULT 0       COMMENT 'Access level: -2=BANNED, -1=RESTRICTED, 0=NORMAL, 1=MANAGER, 2=ADMIN',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Group table';

-- =============================================
-- 6. Message Table
-- =============================================
CREATE TABLE IF NOT EXISTS `message` (
    `id`            VARCHAR(64)     NOT NULL                 COMMENT 'Platform message ID',
    `chat_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Chat identifier',
    `user_id`       VARCHAR(64)     NOT NULL                 COMMENT 'Sender platform user ID',
    `content`       TEXT            DEFAULT NULL             COMMENT 'Text content (nullable when files present)',
    `files`         JSON            DEFAULT NULL             COMMENT 'Attached file record IDs (JSON array)',
    `created_at`    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Created at',
    PRIMARY KEY (`id`),
    KEY `idx_chat_created` (`chat_id`, `created_at`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Chat message table';

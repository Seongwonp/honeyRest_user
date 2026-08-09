-- Align refresh/password-reset token tables with their current JPA entities.
-- Legacy columns are preserved as nullable columns so the migration does not
-- discard existing data. When current columns are missing, they are added and
-- backfilled from the legacy values.

-- refresh_token.token_value
SET @has_column = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'refresh_token' AND column_name = 'token_value'
);
SET @sql = IF(@has_column = 0,
    'ALTER TABLE `refresh_token` ADD COLUMN `token_value` VARCHAR(255) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `refresh_token` SET `token_value` = `token` WHERE `token_value` IS NULL;
ALTER TABLE `refresh_token`
    MODIFY COLUMN `token_value` VARCHAR(255) NOT NULL,
    MODIFY COLUMN `token` VARCHAR(255) NULL;

SET @has_unique_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'refresh_token'
      AND column_name = 'token_value' AND non_unique = 0
);
SET @sql = IF(@has_unique_index = 0,
    'CREATE UNIQUE INDEX `uk_refresh_token_value` ON `refresh_token` (`token_value`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- refresh_token.expiry_date
SET @has_column = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'refresh_token' AND column_name = 'expiry_date'
);
SET @sql = IF(@has_column = 0,
    'ALTER TABLE `refresh_token` ADD COLUMN `expiry_date` DATETIME(6) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `refresh_token` SET `expiry_date` = `expires_at` WHERE `expiry_date` IS NULL;
ALTER TABLE `refresh_token` MODIFY COLUMN `expires_at` DATETIME(6) NULL;

-- password_reset_token.token_value
SET @has_column = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'password_reset_token' AND column_name = 'token_value'
);
SET @sql = IF(@has_column = 0,
    'ALTER TABLE `password_reset_token` ADD COLUMN `token_value` VARCHAR(255) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `password_reset_token` SET `token_value` = `token` WHERE `token_value` IS NULL;
ALTER TABLE `password_reset_token`
    MODIFY COLUMN `token_value` VARCHAR(255) NOT NULL,
    MODIFY COLUMN `token` VARCHAR(255) NULL;

SET @has_unique_index = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'password_reset_token'
      AND column_name = 'token_value' AND non_unique = 0
);
SET @sql = IF(@has_unique_index = 0,
    'CREATE UNIQUE INDEX `uk_password_reset_token_value` ON `password_reset_token` (`token_value`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- password_reset_token.expiry_date and is_used
SET @has_column = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'password_reset_token' AND column_name = 'expiry_date'
);
SET @sql = IF(@has_column = 0,
    'ALTER TABLE `password_reset_token` ADD COLUMN `expiry_date` DATETIME(6) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `password_reset_token` SET `expiry_date` = `expires_at` WHERE `expiry_date` IS NULL;

SET @has_column = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'password_reset_token' AND column_name = 'is_used'
);
SET @sql = IF(@has_column = 0,
    'ALTER TABLE `password_reset_token` ADD COLUMN `is_used` BIT(1) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `password_reset_token` SET `is_used` = `used` WHERE `is_used` IS NULL;
ALTER TABLE `password_reset_token`
    MODIFY COLUMN `expires_at` DATETIME(6) NULL,
    MODIFY COLUMN `used` BIT(1) NULL;

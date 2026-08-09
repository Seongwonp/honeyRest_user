-- Align the legacy email verification token schema with EmailVerificationToken.
--
-- Some existing databases contain both the legacy columns (token, expires_at,
-- used) and the Hibernate-created columns (token_value, expiry_date,
-- is_verified). A clean V1 database contains only the legacy columns, so this
-- migration supports both states without discarding token data.

SET @has_token_value = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'email_verification_token'
      AND column_name = 'token_value'
);
SET @sql = IF(
    @has_token_value = 0,
    'ALTER TABLE `email_verification_token` CHANGE COLUMN `token` `token_value` VARCHAR(255) NOT NULL',
    'ALTER TABLE `email_verification_token` DROP COLUMN `token`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_expiry_date = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'email_verification_token'
      AND column_name = 'expiry_date'
);
SET @sql = IF(
    @has_expiry_date = 0,
    'ALTER TABLE `email_verification_token` CHANGE COLUMN `expires_at` `expiry_date` DATETIME(6) NOT NULL',
    'ALTER TABLE `email_verification_token` DROP COLUMN `expires_at`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_is_verified = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'email_verification_token'
      AND column_name = 'is_verified'
);
SET @sql = IF(
    @has_is_verified = 0,
    'ALTER TABLE `email_verification_token` CHANGE COLUMN `used` `is_verified` BIT(1) NOT NULL DEFAULT 0',
    'ALTER TABLE `email_verification_token` DROP COLUMN `used`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_token_type = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'email_verification_token'
      AND column_name = 'token_type'
);
SET @sql = IF(
    @has_token_type = 0,
    'ALTER TABLE `email_verification_token` ADD COLUMN `token_type` VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

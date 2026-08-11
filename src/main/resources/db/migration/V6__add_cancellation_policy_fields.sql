-- CancellationPolicy 엔티티는 BaseEntity(createdAt/updatedAt)를 상속하고 policyName/detail 필드를 쓰지만,
-- V1__baseline.sql의 cancellation_policy 테이블은 다른 세대의 설계(days_before/refund_rate/description)로
-- 만들어져 있어 엔티티가 요구하는 컬럼이 존재하지 않는다. 기존 컬럼은 데이터 손실 위험이 있어 삭제하지 않고
-- 엔티티가 필요로 하는 컬럼만 추가한다.

SET @has_policy_name = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'cancellation_policy' AND column_name = 'policy_name'
);
SET @sql = IF(@has_policy_name = 0,
    'ALTER TABLE `cancellation_policy` ADD COLUMN `policy_name` VARCHAR(255) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_detail = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'cancellation_policy' AND column_name = 'detail'
);
SET @sql = IF(@has_detail = 0,
    'ALTER TABLE `cancellation_policy` ADD COLUMN `detail` TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_created_at = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'cancellation_policy' AND column_name = 'created_at'
);
SET @sql = IF(@has_created_at = 0,
    'ALTER TABLE `cancellation_policy` ADD COLUMN `created_at` DATETIME(6) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_updated_at = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'cancellation_policy' AND column_name = 'updated_at'
);
SET @sql = IF(@has_updated_at = 0,
    'ALTER TABLE `cancellation_policy` ADD COLUMN `updated_at` DATETIME(6) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

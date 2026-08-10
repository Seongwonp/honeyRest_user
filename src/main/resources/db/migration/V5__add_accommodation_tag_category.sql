-- AccommodationTag 엔티티의 category 필드가 V1__baseline.sql의 accommodation_tag 테이블 정의에서 누락되어 있었다.
-- 개발 DB는 과거 ddl-auto=update로 이미 컬럼이 채워져 있을 수 있으므로, 새로 생성되는 DB(예: 테스트 스키마)와
-- 상태가 다른 두 경우를 모두 안전하게 처리한다. 기존 행에는 'GENERAL'로 백필한 뒤 NOT NULL을 건다.

SET @has_category = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'accommodation_tag'
      AND column_name = 'category'
);
SET @sql = IF(
    @has_category = 0,
    'ALTER TABLE `accommodation_tag` ADD COLUMN `category` VARCHAR(50) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `accommodation_tag` SET `category` = 'GENERAL' WHERE `category` IS NULL;

SET @is_not_null = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'accommodation_tag'
      AND column_name = 'category'
      AND is_nullable = 'YES'
);
SET @sql = IF(
    @is_not_null = 1,
    'ALTER TABLE `accommodation_tag` MODIFY COLUMN `category` VARCHAR(50) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

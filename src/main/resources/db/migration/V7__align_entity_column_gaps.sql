-- 엔티티(WishList/Event/Inquiry/Notification/PointHistory)와 V1 baseline 스키마 사이에
-- 이름 변경·컬럼 신설이 누락되어 있던 항목들을 보정한다. 기존에 이미 적용된 마이그레이션은 건드리지 않는다.
-- 개발 DB는 과거 ddl-auto=update로 이미 새 컬럼/테이블명을 갖고 있을 수 있으므로 모든 변경은 존재 여부를
-- information_schema로 확인한 뒤에만 실행한다.

-- 1) wish_list -> wishlist (WishList 엔티티가 @Table(name="wishlist")로 매핑됨)
SET @has_wishlist = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'wishlist'
);
SET @has_wish_list = (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'wish_list'
);
SET @sql = IF(@has_wishlist = 0 AND @has_wish_list = 1,
    'RENAME TABLE `wish_list` TO `wishlist`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_wishlist_id_col = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'wishlist' AND column_name = 'wishlist_id'
);
SET @has_wish_list_id_col = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'wishlist' AND column_name = 'wish_list_id'
);
SET @sql = IF(@has_wishlist_id_col = 0 AND @has_wish_list_id_col = 1,
    'ALTER TABLE `wishlist` CHANGE COLUMN `wish_list_id` `wishlist_id` BIGINT NOT NULL AUTO_INCREMENT',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_wishlist_updated_at = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'wishlist' AND column_name = 'updated_at'
);
SET @sql = IF(@has_wishlist_updated_at = 0,
    'ALTER TABLE `wishlist` ADD COLUMN `updated_at` DATETIME(6) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) event: description/target_url 신설 (Event 엔티티)
SET @has_event_description = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'event' AND column_name = 'description'
);
SET @sql = IF(@has_event_description = 0,
    'ALTER TABLE `event` ADD COLUMN `description` TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_event_target_url = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'event' AND column_name = 'target_url'
);
SET @sql = IF(@has_event_target_url = 0,
    'ALTER TABLE `event` ADD COLUMN `target_url` VARCHAR(255) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) inquiry: accommodation_id/category/reply/is_replied 신설 (Inquiry 엔티티는 answer/answered_at 대신
--    reply/is_replied를 사용). 기존 answer 값이 있으면 reply로 백필한다.
SET @has_inquiry_accommodation_id = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'inquiry' AND column_name = 'accommodation_id'
);
SET @sql = IF(@has_inquiry_accommodation_id = 0,
    'ALTER TABLE `inquiry` ADD COLUMN `accommodation_id` BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_inquiry_category = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'inquiry' AND column_name = 'category'
);
SET @sql = IF(@has_inquiry_category = 0,
    'ALTER TABLE `inquiry` ADD COLUMN `category` VARCHAR(50) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_inquiry_reply = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'inquiry' AND column_name = 'reply'
);
SET @sql = IF(@has_inquiry_reply = 0,
    'ALTER TABLE `inquiry` ADD COLUMN `reply` TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_inquiry_is_replied = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'inquiry' AND column_name = 'is_replied'
);
SET @sql = IF(@has_inquiry_is_replied = 0,
    'ALTER TABLE `inquiry` ADD COLUMN `is_replied` BIT(1) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_answer = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'inquiry' AND column_name = 'answer'
);
SET @sql = IF(@has_answer = 1,
    'UPDATE `inquiry` SET `reply` = `answer` WHERE `reply` IS NULL AND `answer` IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `inquiry` SET `is_replied` = (`reply` IS NOT NULL) WHERE `is_replied` IS NULL;

SET @has_inquiry_acc_fk = (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE() AND table_name = 'inquiry' AND constraint_type = 'FOREIGN KEY'
      AND constraint_name = 'fk_inquiry_accommodation'
);
SET @sql = IF(@has_inquiry_acc_fk = 0,
    'ALTER TABLE `inquiry` ADD CONSTRAINT `fk_inquiry_accommodation` FOREIGN KEY (`accommodation_id`) REFERENCES `accommodation` (`accommodation_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) notification: content/target_url 신설 (Notification 엔티티는 message 대신 content 사용).
--    기존 message 값이 있으면 content로 백필한다.
SET @has_notification_content = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'notification' AND column_name = 'content'
);
SET @sql = IF(@has_notification_content = 0,
    'ALTER TABLE `notification` ADD COLUMN `content` TEXT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_notification_target_url = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'notification' AND column_name = 'target_url'
);
SET @sql = IF(@has_notification_target_url = 0,
    'ALTER TABLE `notification` ADD COLUMN `target_url` VARCHAR(50) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_message = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'notification' AND column_name = 'message'
);
SET @sql = IF(@has_message = 1,
    'UPDATE `notification` SET `content` = `message` WHERE `content` IS NULL AND `message` IS NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) point_history: balance(NOT NULL)/updated_at 신설. 과거 데이터의 실제 잔액은 복원할 수 없으므로
--    0으로 백필한다(운영 데이터 정정은 별도 배치로 처리 필요, 이 마이그레이션의 책임 범위 밖).
SET @has_point_history_balance = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'point_history' AND column_name = 'balance'
);
SET @sql = IF(@has_point_history_balance = 0,
    'ALTER TABLE `point_history` ADD COLUMN `balance` INT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_point_history_updated_at = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'point_history' AND column_name = 'updated_at'
);
SET @sql = IF(@has_point_history_updated_at = 0,
    'ALTER TABLE `point_history` ADD COLUMN `updated_at` DATETIME(6) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `point_history` SET `balance` = 0 WHERE `balance` IS NULL;

SET @point_history_balance_nullable = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'point_history' AND column_name = 'balance' AND is_nullable = 'YES'
);
SET @sql = IF(@point_history_balance_nullable = 1,
    'ALTER TABLE `point_history` MODIFY COLUMN `balance` INT NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

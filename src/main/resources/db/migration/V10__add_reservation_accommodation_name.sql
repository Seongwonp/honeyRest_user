-- reservation.accommodation_name (숙소명 스냅샷) 컬럼 추가.
-- 호스트 저장소(honeyRest_host)의 Reservation 엔티티는 accommodation_name 을 VARCHAR(255) NOT NULL 로 매핑하고
-- ddl-auto=validate 로 기동하므로, V1~V9 에 이 컬럼이 없으면 호스트 앱이 스키마 검증에서 실패한다.
--
-- 결정: 호스트 매핑과 같게 NOT NULL 로 둔다.
--   1) 컬럼이 없으면 NULL 허용으로 먼저 추가한다.
--   2) 기존 예약 행은 accommodation.name 으로 채운다 (accommodation_id 는 NOT NULL FK 라 모든 행이 채워진다).
--   3) 그 다음 NOT NULL 로 바꾼다.
-- 사용자 저장소도 Reservation 엔티티에 accommodation_name 을 매핑하고 ReserveService.createReservation 에서
-- Room.accommodation.name 을 넣도록 함께 수정했다 (NOT NULL 이라 매핑 없이 INSERT 하면 실패한다).
-- 개발 DB 는 과거 ddl-auto=update 로 이미 컬럼이 있을 수 있어 모든 단계는 재실행해도 안전하게 작성한다.

SET @has_accommodation_name = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'reservation'
      AND column_name = 'accommodation_name'
);
SET @sql = IF(
    @has_accommodation_name = 0,
    'ALTER TABLE `reservation` ADD COLUMN `accommodation_name` VARCHAR(255) NULL AFTER `accommodation_id`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 행 백필 (비어 있는 행만)
UPDATE `reservation` r
    JOIN `accommodation` a ON a.`accommodation_id` = r.`accommodation_id`
SET r.`accommodation_name` = a.`name`
WHERE r.`accommodation_name` IS NULL OR r.`accommodation_name` = '';

-- NOT NULL 전환 (이미 NOT NULL 이어도 같은 정의로 다시 적용되므로 안전)
ALTER TABLE `reservation` MODIFY COLUMN `accommodation_name` VARCHAR(255) NOT NULL;

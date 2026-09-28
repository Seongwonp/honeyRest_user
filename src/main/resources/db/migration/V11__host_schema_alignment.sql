-- 호스트 관리자 앱(honeyRest_host)과의 공유 스키마 정합.
-- 호스트 앱은 Flyway 없이 ddl-auto=validate 로 기동하므로, 호스트 엔티티가 요구하는 테이블/컬럼이
-- 이 저장소의 마이그레이션에 없으면 호스트가 기동 시점(또는 첫 쓰기 시점)에 실패한다.
-- 호스트 엔티티와 V1~V10 DDL 을 정적 비교해 찾은 항목 중 스키마 쪽에서 고쳐야 하는 것만 여기서 다룬다.
--   1) error_log 테이블: 호스트 ErrorLog 엔티티(전역 예외 기록/관리 화면)가 쓰지만 V1~V10 에 없다.
--      -> validate 에서 "missing table [error_log]" 로 호스트 기동 실패. 엔티티 매핑과 같은 정의로 생성한다.
--   2) cancellation_policy.days_before / refund_rate: V1 의 구세대 컬럼이 NOT NULL(기본값 없음)로 남아 있다.
--      두 저장소의 CancellationPolicy 엔티티는 policy_name/detail 만 매핑하므로, 호스트
--      CancellationPolicyServiceImpl 이 새 정책을 INSERT 하면 MySQL strict 모드에서
--      "Field 'days_before' doesn't have a default value" 로 실패한다(validate 는 통과해서 런타임에만 드러남).
--      기존 데이터 보존을 위해 컬럼은 지우지 않고 NULL 허용으로만 완화한다.
-- (호스트 AccommodationTag 의 icon 컬럼은 호스트 매핑 오타라 호스트 쪽에서 icon_name 으로 고쳤다. 스키마 변경 없음.)
--
-- 개발 DB 는 과거 ddl-auto=update 로 이미 error_log 가 있을 수 있어 V10 과 같이 모든 단계를 재실행해도 안전하게 작성한다.

-- 1) error_log (호스트 ErrorLog 엔티티와 1:1)
CREATE TABLE IF NOT EXISTS `error_log` (
    `error_log_id`   BIGINT        NOT NULL AUTO_INCREMENT,
    `occurred_at`    DATETIME(6)   NOT NULL,
    `request_url`    VARCHAR(500),
    `request_method` VARCHAR(10),
    `error_class`    VARCHAR(200),
    `message`        VARCHAR(2000),
    `stack_trace`    TEXT,
    `resolved`       BIT(1)        NOT NULL DEFAULT 0,
    PRIMARY KEY (`error_log_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 관리 화면 목록 조회(resolved 필터 + occurred_at 내림차순)용 인덱스
SET @has_error_log_idx = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'error_log'
      AND index_name = 'idx_error_log_resolved_occurred_at'
);
SET @sql = IF(
    @has_error_log_idx = 0,
    'CREATE INDEX `idx_error_log_resolved_occurred_at` ON `error_log` (`resolved`, `occurred_at`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) cancellation_policy 구세대 NOT NULL 컬럼 완화 (컬럼이 없거나 이미 NULL 허용이면 건너뛴다)
SET @days_before_not_null = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cancellation_policy'
      AND column_name = 'days_before'
      AND is_nullable = 'NO'
);
SET @sql = IF(
    @days_before_not_null = 1,
    'ALTER TABLE `cancellation_policy` MODIFY COLUMN `days_before` INT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @refund_rate_not_null = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'cancellation_policy'
      AND column_name = 'refund_rate'
      AND is_nullable = 'NO'
);
SET @sql = IF(
    @refund_rate_not_null = 1,
    'ALTER TABLE `cancellation_policy` MODIFY COLUMN `refund_rate` DECIMAL(5, 2) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

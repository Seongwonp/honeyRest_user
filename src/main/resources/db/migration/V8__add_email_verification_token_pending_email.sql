-- 이메일 변경 토큰이 실제로 어떤 새 이메일로의 변경을 승인한 것인지 서버에 저장하지 않고 있었다.
-- 그 결과 확정 API가 토큰 타입/대상 이메일을 검증하지 못해, 아무 유효한 토큰으로나
-- 임의의 newEmail 파라미터를 넘겨 이메일을 바꿀 수 있었다(P0-8).

SET @has_pending_email = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'email_verification_token'
      AND column_name = 'pending_email'
);
SET @sql = IF(
    @has_pending_email = 0,
    'ALTER TABLE `email_verification_token` ADD COLUMN `pending_email` VARCHAR(255) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

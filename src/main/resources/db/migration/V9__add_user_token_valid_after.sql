-- 로그아웃/비밀번호 변경 후에도 이미 발급된 access token이 만료 시각까지 계속 유효했다(P1-10).
-- 이 컬럼보다 먼저 발급된(iat < token_valid_after) 토큰은 JwtTokenProvider에서 거부한다.

SET @has_token_valid_after = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user'
      AND column_name = 'token_valid_after'
);
SET @sql = IF(
    @has_token_valid_after = 0,
    'ALTER TABLE `user` ADD COLUMN `token_valid_after` DATETIME(6) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

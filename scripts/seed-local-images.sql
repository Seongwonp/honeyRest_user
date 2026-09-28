-- HoneyRest 로컬 이미지 시드
-- ---------------------------------------------------------------------------
-- 목적: DB에 저장된 Firebase Storage 이미지 URL(firebasestorage.googleapis.com)을
--       로컬 플레이스홀더 경로(/uploads/placeholder/*.svg)로 치환한다.
--       app.storage.type=local 로 실행할 때 Firebase 접근 없이 화면을 확인하기 위한 용도다.
--
-- 전제:
--   - 플레이스홀더 파일은 저장소의 uploads/placeholder/ 에 있으며,
--     백엔드가 app.storage.local.dir=./uploads(기본값)로 실행되면 /uploads/** 로 서빙된다.
--   - Firebase URL만 대상으로 하므로 여러 번 실행해도 안전하다(멱등).
--   - 되돌릴 수 없으므로 운영 DB에서는 실행하지 말 것. 필요하면 먼저 백업한다.
--
-- 사용 예: mysql -u root -p honeyrest_db < scripts/seed-local-images.sql
-- ---------------------------------------------------------------------------

-- MySQL Workbench 등의 safe update 모드에서도 실행되도록 일시 해제
SET @old_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- 숙소 대표 이미지 (stay-1 ~ stay-4 순환)
UPDATE accommodation
SET thumbnail = CONCAT('/uploads/placeholder/stay-', MOD(accommodation_id, 4) + 1, '.svg')
WHERE thumbnail LIKE '%firebasestorage%';

-- 숙소 상세 이미지 (stay-1 ~ stay-4 순환)
UPDATE accommodation_image
SET image_url = CONCAT('/uploads/placeholder/stay-', MOD(accommodation_image_id, 4) + 1, '.svg')
WHERE image_url LIKE '%firebasestorage%';

-- 객실 이미지 (room-1 ~ room-2 순환)
UPDATE room_image
SET image_url = CONCAT('/uploads/placeholder/room-', MOD(room_image_id, 2) + 1, '.svg')
WHERE image_url LIKE '%firebasestorage%';

-- 리뷰 이미지 (review-1 ~ review-2 순환)
UPDATE review_image
SET image_url = CONCAT('/uploads/placeholder/review-', MOD(review_image_id, 2) + 1, '.svg')
WHERE image_url LIKE '%firebasestorage%';

-- 배너 이미지 (banner-1 ~ banner-2 순환)
UPDATE banner
SET image_url = CONCAT('/uploads/placeholder/banner-', MOD(banner_id, 2) + 1, '.svg')
WHERE image_url LIKE '%firebasestorage%';

-- 이벤트 이미지
UPDATE event
SET image_url = '/uploads/placeholder/event-1.svg'
WHERE image_url LIKE '%firebasestorage%';

-- 지역 이미지 (region-1 ~ region-2 순환)
UPDATE region
SET image_url = CONCAT('/uploads/placeholder/region-', MOD(region_id, 2) + 1, '.svg')
WHERE image_url LIKE '%firebasestorage%';

-- 사용자 프로필 이미지
UPDATE `user`
SET profile_image = '/uploads/placeholder/profile-1.svg'
WHERE profile_image LIKE '%firebasestorage%';

COMMIT;

SET SQL_SAFE_UPDATES = @old_safe_updates;

-- 확인용: 남아 있는 Firebase URL 개수 (모두 0이어야 한다)
SELECT 'accommodation.thumbnail' AS target, COUNT(*) AS remaining FROM accommodation WHERE thumbnail LIKE '%firebasestorage%'
UNION ALL SELECT 'accommodation_image', COUNT(*) FROM accommodation_image WHERE image_url LIKE '%firebasestorage%'
UNION ALL SELECT 'room_image', COUNT(*) FROM room_image WHERE image_url LIKE '%firebasestorage%'
UNION ALL SELECT 'review_image', COUNT(*) FROM review_image WHERE image_url LIKE '%firebasestorage%'
UNION ALL SELECT 'banner', COUNT(*) FROM banner WHERE image_url LIKE '%firebasestorage%'
UNION ALL SELECT 'event', COUNT(*) FROM event WHERE image_url LIKE '%firebasestorage%'
UNION ALL SELECT 'region', COUNT(*) FROM region WHERE image_url LIKE '%firebasestorage%'
UNION ALL SELECT 'user.profile_image', COUNT(*) FROM `user` WHERE profile_image LIKE '%firebasestorage%';

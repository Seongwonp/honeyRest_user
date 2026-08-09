-- HoneyRest local demo data
-- Safe to run repeatedly. It only owns rows prefixed with DEMO- and demo.review*@honeyrest.local.

START TRANSACTION;

INSERT INTO `user` (
    created_at, updated_at, email, password_hash, social_type, social_id,
    name, phone, marketing_agree, point, role, status, is_verified
)
VALUES
    (NOW(6), NOW(6), 'demo.review01@honeyrest.local', NULL, 'DEMO', 'demo-review-01', '여행하는 민지', '01000001001', b'0', 0, 'USER', 'ACTIVE', b'1'),
    (NOW(6), NOW(6), 'demo.review02@honeyrest.local', NULL, 'DEMO', 'demo-review-02', '주말여행가',   '01000001002', b'0', 0, 'USER', 'ACTIVE', b'1'),
    (NOW(6), NOW(6), 'demo.review03@honeyrest.local', NULL, 'DEMO', 'demo-review-03', '포레스트러버', '01000001003', b'0', 0, 'USER', 'ACTIVE', b'1'),
    (NOW(6), NOW(6), 'demo.review04@honeyrest.local', NULL, 'DEMO', 'demo-review-04', '쉼표하나',     '01000001004', b'0', 0, 'USER', 'ACTIVE', b'1'),
    (NOW(6), NOW(6), 'demo.review05@honeyrest.local', NULL, 'DEMO', 'demo-review-05', '초록산책',     '01000001005', b'0', 0, 'USER', 'ACTIVE', b'1'),
    (NOW(6), NOW(6), 'demo.review06@honeyrest.local', NULL, 'DEMO', 'demo-review-06', '달빛캠퍼',     '01000001006', b'0', 0, 'USER', 'ACTIVE', b'1')
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at),
    name = VALUES(name),
    status = 'ACTIVE',
    is_verified = b'1';

SET @demo_accommodation_id = (
    SELECT accommodation_id FROM accommodation WHERE name = '블랙우드' ORDER BY accommodation_id DESC LIMIT 1
);
SET @demo_room_id = (
    SELECT room_id FROM room WHERE accommodation_id = @demo_accommodation_id ORDER BY room_id LIMIT 1
);
SET @demo_room_name = (SELECT name FROM room WHERE room_id = @demo_room_id);
SET @demo_accommodation_name = (SELECT name FROM accommodation WHERE accommodation_id = @demo_accommodation_id);

CREATE TEMPORARY TABLE demo_review_fixture (
    seq INT PRIMARY KEY,
    email VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    days_ago INT NOT NULL,
    rating DECIMAL(3,2) NOT NULL,
    cleanliness DECIMAL(3,2) NOT NULL,
    service DECIMAL(3,2) NOT NULL,
    facilities DECIMAL(3,2) NOT NULL,
    location_score DECIMAL(3,2) NOT NULL,
    like_count INT NOT NULL,
    content TEXT NOT NULL,
    reply TEXT NULL
);

INSERT INTO demo_review_fixture VALUES
    (1,  'demo.review01@honeyrest.local', 86, 4.90, 5.00, 4.80, 4.90, 4.90, 18, '사진에서 본 분위기보다 실제 숲 전망이 더 좋았어요. 침구가 포근하고 객실도 아주 조용해서 오랜만에 푹 쉬었습니다.', '정성스러운 후기 감사합니다. 다음 방문도 편안하게 준비해 두겠습니다.'),
    (2,  'demo.review02@honeyrest.local', 79, 4.70, 4.80, 4.70, 4.60, 4.70, 11, '체크인이 빠르고 안내가 친절했습니다. 아침에 창문을 열었을 때 나무 향이 나는 점이 특히 좋았어요.', NULL),
    (3,  'demo.review03@honeyrest.local', 70, 5.00, 5.00, 5.00, 4.90, 5.00, 27, '조용히 쉬고 싶어서 선택했는데 완벽했습니다. 객실 동선이 편하고 주변 산책 코스도 가까워서 부모님도 만족하셨어요.', '가족 여행에 좋은 기억을 드릴 수 있어 기쁩니다.'),
    (4,  'demo.review04@honeyrest.local', 62, 4.60, 4.70, 4.80, 4.40, 4.50, 9,  '주차 공간이 넉넉하고 객실이 깔끔해요. 저녁에는 조명이 따뜻해서 사진도 예쁘게 나왔습니다.', NULL),
    (5,  'demo.review05@honeyrest.local', 55, 4.80, 4.90, 4.70, 4.80, 4.80, 16, '침구와 욕실이 깨끗했고 필요한 비품이 잘 준비되어 있었어요. 다음에는 이틀 이상 머물고 싶습니다.', NULL),
    (6,  'demo.review06@honeyrest.local', 48, 4.50, 4.60, 4.50, 4.50, 4.40, 7,  '가볍게 떠난 주말 여행이었는데 기대 이상으로 편안했습니다. 늦은 체크인 문의에도 답변이 빨랐어요.', '편안한 주말이 되셨다니 다행입니다. 다시 뵙겠습니다.'),
    (7,  'demo.review01@honeyrest.local', 39, 4.90, 4.90, 5.00, 4.80, 4.90, 21, '두 번째 방문인데 여전히 관리가 잘 되어 있네요. 숲을 바라보며 마시는 아침 커피가 정말 좋았습니다.', NULL),
    (8,  'demo.review02@honeyrest.local', 31, 4.40, 4.50, 4.60, 4.20, 4.50, 5,  '객실이 아늑하고 직원분들이 친절했어요. 주변이 조용해서 혼자 쉬기에도 부담 없었습니다.', NULL),
    (9,  'demo.review03@honeyrest.local', 24, 4.80, 4.80, 4.90, 4.70, 4.80, 14, '아이와 함께 갔는데 객실이 넓고 안전해서 좋았어요. 자연 속 숙소를 찾는 가족에게 추천합니다.', NULL),
    (10, 'demo.review04@honeyrest.local', 17, 4.70, 4.80, 4.70, 4.60, 4.70, 10, '전체적으로 차분하고 정돈된 느낌입니다. 예약부터 체크아웃까지 안내가 명확해서 편했어요.', NULL),
    (11, 'demo.review05@honeyrest.local', 10, 5.00, 5.00, 4.90, 5.00, 5.00, 31, '기념일 여행으로 방문했는데 정말 만족했습니다. 객실 컨디션, 전망, 서비스 모두 훌륭했어요.', '소중한 날에 블랙우드를 선택해 주셔서 감사합니다.'),
    (12, 'demo.review06@honeyrest.local',  4, 4.90, 5.00, 4.90, 4.80, 4.90, 23, '도심에서 멀지 않은데도 완전히 다른 곳에 온 기분이었어요. 재방문 의사 있습니다.', NULL);

INSERT INTO reservation (
    created_at, updated_at, user_id, room_id, accommodation_id, room_name,
    reservation_number, check_in_date, check_out_date, guest_count,
    guest_name, guest_phone, price, original_price, discount_amount,
    status, special_requests, accommodation_name, version
)
SELECT
    DATE_SUB(NOW(6), INTERVAL f.days_ago DAY), DATE_SUB(NOW(6), INTERVAL f.days_ago DAY),
    u.user_id, @demo_room_id, @demo_accommodation_id, @demo_room_name,
    CONCAT('DEMO-REVIEW-30-', LPAD(f.seq, 3, '0')),
    DATE_SUB(CURDATE(), INTERVAL f.days_ago DAY),
    DATE_ADD(DATE_SUB(CURDATE(), INTERVAL f.days_ago DAY), INTERVAL 1 DAY),
    2, u.name, u.phone, 74000.00, 74000.00, 0.00,
    'COMPLETED', '로컬 데모 데이터', @demo_accommodation_name, 0
FROM demo_review_fixture f
JOIN `user` u ON u.email = f.email
WHERE @demo_accommodation_id IS NOT NULL AND @demo_room_id IS NOT NULL
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);

INSERT INTO review (
    created_at, updated_at, reservation_id, user_id, accommodation_id, room_id,
    rating, cleanliness_rating, service_rating, facilities_rating, location_rating,
    content, reply, like_count, status
)
SELECT
    DATE_SUB(NOW(6), INTERVAL f.days_ago DAY), DATE_SUB(NOW(6), INTERVAL f.days_ago DAY),
    r.reservation_id, u.user_id, @demo_accommodation_id, @demo_room_id,
    f.rating, f.cleanliness, f.service, f.facilities, f.location_score,
    f.content, f.reply, f.like_count, 'PUBLISHED'
FROM demo_review_fixture f
JOIN `user` u ON u.email = f.email
JOIN reservation r ON r.reservation_number = CONCAT('DEMO-REVIEW-30-', LPAD(f.seq, 3, '0'))
WHERE NOT EXISTS (SELECT 1 FROM review existing WHERE existing.reservation_id = r.reservation_id);

-- Synchronize rows owned by this fixture when they already existed from an
-- earlier demo run.
UPDATE review rv
JOIN reservation r ON r.reservation_id = rv.reservation_id
JOIN demo_review_fixture f
  ON r.reservation_number = CONCAT('DEMO-REVIEW-30-', LPAD(f.seq, 3, '0'))
JOIN `user` u ON u.email = f.email
SET rv.updated_at = DATE_SUB(NOW(6), INTERVAL f.days_ago DAY),
    rv.user_id = u.user_id,
    rv.accommodation_id = @demo_accommodation_id,
    rv.room_id = @demo_room_id,
    rv.rating = f.rating,
    rv.cleanliness_rating = f.cleanliness,
    rv.service_rating = f.service,
    rv.facilities_rating = f.facilities,
    rv.location_rating = f.location_score,
    rv.content = f.content,
    rv.reply = f.reply,
    rv.like_count = f.like_count,
    rv.status = 'PUBLISHED';

-- Make every active room searchable for the next 90 days without overwriting
-- prices that a host already configured.
INSERT IGNORE INTO price_calendar (room_id, date, price, created_at, updated_at, available_room)
SELECT
    room.room_id,
    DATE_ADD(CURDATE(), INTERVAL days.n DAY),
    room.price,
    NOW(6),
    NOW(6),
    room.total_rooms
FROM room
CROSS JOIN (
    SELECT ones.n + tens.n * 10 AS n
    FROM
        (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN
        (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8) tens
) days
WHERE room.status = 'ACTIVE';

-- Keep denormalized list values consistent with the current room/review data.
UPDATE accommodation a
JOIN (
    SELECT accommodation_id, MIN(price) AS min_price
    FROM room
    WHERE status = 'ACTIVE'
    GROUP BY accommodation_id
) room_price ON room_price.accommodation_id = a.accommodation_id
SET a.min_price = room_price.min_price;

UPDATE accommodation a
JOIN (
    SELECT accommodation_id, ROUND(AVG(rating), 1) AS average_rating
    FROM review
    WHERE status = 'PUBLISHED'
    GROUP BY accommodation_id
) review_rating ON review_rating.accommodation_id = a.accommodation_id
SET a.rating = review_rating.average_rating;

-- Correct the known Hongcheon demo accommodation region only when the address
-- and target hierarchy match, so the statement stays safe to rerun.
UPDATE accommodation a
JOIN region main_region ON main_region.region_id = a.main_region_id
JOIN region target_region
  ON target_region.parent_id = main_region.region_id
 AND target_region.name = '홍천군'
SET a.sub_region_id = target_region.region_id
WHERE a.name = '숲속글램핑'
  AND a.address LIKE '%홍천군%';

DROP TEMPORARY TABLE demo_review_fixture;
COMMIT;

-- HoneyRest local DB quality audit
-- Read-only: every statement returns aggregate evidence or a small target list.

SELECT 'accommodations' AS check_name, COUNT(*) AS affected_count, 100.00 AS affected_rate
FROM accommodation
UNION ALL
SELECT 'active_rooms', COUNT(*), 100.00
FROM room
WHERE status = 'ACTIVE'
UNION ALL
SELECT 'rooms_without_images', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM room WHERE status = 'ACTIVE'), 1), 2)
FROM room r
WHERE r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM room_image ri WHERE ri.room_id = r.room_id)
UNION ALL
SELECT 'min_price_mismatch', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM accommodation), 1), 2)
FROM accommodation a
JOIN (
    SELECT accommodation_id, MIN(price) AS expected
    FROM room
    WHERE status = 'ACTIVE'
    GROUP BY accommodation_id
) x ON x.accommodation_id = a.accommodation_id
WHERE a.min_price IS NULL OR a.min_price <> x.expected
UNION ALL
SELECT 'region_hierarchy_mismatch', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM accommodation), 1), 2)
FROM accommodation a
JOIN region sub ON sub.region_id = a.sub_region_id
WHERE sub.parent_id IS NULL OR sub.parent_id <> a.main_region_id
UNION ALL
SELECT 'rating_mismatch', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM accommodation), 1), 2)
FROM accommodation a
JOIN (
    SELECT accommodation_id, ROUND(AVG(rating), 1) AS expected
    FROM review
    WHERE status = 'PUBLISHED'
    GROUP BY accommodation_id
) x ON x.accommodation_id = a.accommodation_id
WHERE a.rating IS NULL OR a.rating <> x.expected
UNION ALL
SELECT 'invalid_review_rating', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM review), 1), 2)
FROM review
WHERE rating < 0 OR rating > 5
UNION ALL
SELECT 'reviews_missing_timestamp', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM review), 1), 2)
FROM review
WHERE created_at IS NULL
UNION ALL
SELECT 'duplicate_user_emails', COALESCE(SUM(duplicate_count - 1), 0), 0
FROM (
    SELECT COUNT(*) AS duplicate_count
    FROM user
    GROUP BY LOWER(TRIM(email))
    HAVING COUNT(*) > 1
) duplicates
UNION ALL
SELECT 'orphan_reservations', COUNT(*),
       ROUND(100 * COUNT(*) / GREATEST((SELECT COUNT(*) FROM reservation), 1), 2)
FROM reservation r
LEFT JOIN room rm ON rm.room_id = r.room_id
LEFT JOIN accommodation a ON a.accommodation_id = r.accommodation_id
WHERE rm.room_id IS NULL OR a.accommodation_id IS NULL;

SELECT r.room_id, a.name AS accommodation_name, r.name AS room_name
FROM room r
JOIN accommodation a ON a.accommodation_id = r.accommodation_id
WHERE r.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM room_image ri WHERE ri.room_id = r.room_id)
ORDER BY r.room_id;

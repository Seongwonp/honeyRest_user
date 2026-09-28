-- =====================================================================================
-- e2e 프로필 시드 데이터 (H2 MySQL 모드 전용)
-- -------------------------------------------------------------------------------------
-- application-e2e.properties 의 spring.sql.init 으로 Hibernate 스키마 생성(create-drop) 직후 실행된다.
-- 프론트엔드 Playwright 사용자 여정 테스트(honeyrest_user_react/e2e)가 아래 값에 의존하므로
-- 이름/가격/객실 수를 바꾸면 e2e/fixtures/seed.js 도 함께 고칠 것.
--
-- 요약
--   지역      : 강원(1) > 강릉(2), 제주(3) > 서귀포(4)
--   숙소 1    : 허니레스트 강릉 오션 호텔 (강릉)
--               - 객실 101 '오션 스위트 (1실 한정)'  total_rooms=1  1박 100,000원  ← 중복 예약 409 시나리오용
--               - 객실 102 '스탠다드 더블'           total_rooms=5  1박  80,000원
--   숙소 2    : 허니레스트 서귀포 힐링 펜션 (서귀포)
--               - 객실 201 '패밀리 룸'               total_rooms=3  1박 120,000원
--   태그      : 오션뷰, 무료 주차, 바비큐
--   회원      : e2e.user@honeyrest.test / Honey1234!  (이메일 인증 완료, 포인트 5,000)
--   쿠폰      : 'E2E 5천원 할인' (정액 5,000원, 최소 50,000원) — 위 회원에게 발급
--
-- 비밀번호 해시 재생성 방법 (BCrypt, cost 10 — SecurityConfig 의 BCryptPasswordEncoder 기본값과 동일):
--   C=$(find ~/.gradle/caches -name 'spring-security-crypto-6*.jar' | head -1)
--   J=$(find ~/.gradle/caches -name 'spring-jcl-6*.jar' | head -1)
--   echo 'public class H{public static void main(String[] a){System.out.println(
--     new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(a[0]));}}' > /tmp/H.java
--   java -cp "$C:$J" /tmp/H.java 'Honey1234!'
--
-- 명시적 ID 로 넣은 뒤 마지막에 IDENTITY 시작값을 옮겨, 앱이 새로 INSERT 하는 행(회원 가입 등)과 충돌하지 않게 한다.
-- JSON 컬럼은 H2 에서 문자열이 JSON 문자열 값으로 바뀌지 않도록 FORMAT JSON 으로 넣는다.
-- =====================================================================================

-- ---- 지역 ----
INSERT INTO region (region_id, parent_id, name, level, is_popular, image_url) VALUES
  (1, NULL, '강원', 1, TRUE, '/images/bg1.jpg'),
  (2, 1, '강릉', 2, TRUE, '/images/bg2.jpg'),
  (3, NULL, '제주', 1, TRUE, '/images/bg3.jpg'),
  (4, 3, '서귀포', 2, FALSE, '/images/bg4.jpg');

-- ---- 숙소 카테고리 ----
INSERT INTO accommodation_category (category_id, name, icon_url, sort_order) VALUES
  (1, '호텔', NULL, 1),
  (2, '펜션', NULL, 2);

-- ---- 업체 ----
INSERT INTO company (company_id, name, business_number, owner_name, phone, email, address, bank_info, commission_rate, status, created_at, updated_at) VALUES
  (1, '허니레스트 E2E 호스팅', '123-45-67890', '김허니', '02-000-0000', 'host@honeyrest.test', '서울특별시 중구 테스트로 1',
   '{"bank":"테스트은행","account":"000-000-000000"}' FORMAT JSON, 10.00, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 숙소 ----
INSERT INTO accommodation (accommodation_id, company_id, category_id, main_region_id, sub_region_id, name, address, latitude, longitude,
                           thumbnail, description, amenities, check_in_time, check_out_time, rating, min_price, status) VALUES
  (1, 1, 1, 1, 2, '허니레스트 강릉 오션 호텔', '강원특별자치도 강릉시 해안로 100', 37.795200, 128.918300,
   '/images/bg1.jpg', '경포 바다가 한눈에 보이는 E2E 테스트용 호텔입니다.',
   '["무료 와이파이","주차장","조식"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 80000.00, 'ACTIVE'),
  (2, 1, 2, 3, 4, '허니레스트 서귀포 힐링 펜션', '제주특별자치도 서귀포시 중문로 200', 33.250300, 126.412000,
   '/images/bg3.jpg', '귤밭 사이의 조용한 E2E 테스트용 펜션입니다.',
   '["바비큐장","주차장"]' FORMAT JSON, TIMESTAMP '2025-01-01 16:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 120000.00, 'ACTIVE');

INSERT INTO accommodation_image (image_id, accommodation_id, image_url, image_type, sort_order, created_at, updated_at) VALUES
  (1, 1, '/images/bg1.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, '/images/bg2.jpg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 2, '/images/bg3.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 객실 (101 은 total_rooms=1: 같은 날짜 두 번째 예약은 409) ----
INSERT INTO room (room_id, accommodation_id, name, type, price, max_occupancy, standard_occupancy, extra_person_fee,
                  bed_info, amenities, description, total_rooms, status, created_at, updated_at) VALUES
  (101, 1, '오션 스위트 (1실 한정)', 'SUITE', 100000.00, 4, 2, 20000.00,
   '["킹 베드 1개"]' FORMAT JSON, '["오션뷰","욕조","미니바"]' FORMAT JSON,
   '바다를 정면으로 바라보는 단 하나뿐인 스위트룸입니다.', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, 1, '스탠다드 더블', 'DOUBLE', 80000.00, 2, 2, 0.00,
   '["더블 베드 1개"]' FORMAT JSON, '["TV","냉장고"]' FORMAT JSON,
   '합리적인 가격의 기본 객실입니다.', 5, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (201, 2, '패밀리 룸', 'FAMILY', 120000.00, 6, 4, 10000.00,
   '["퀸 베드 2개"]' FORMAT JSON, '["주방","바비큐 그릴"]' FORMAT JSON,
   '가족 단위 여행객을 위한 넓은 객실입니다.', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO room_image (image_id, room_id, image_url, sort_order, created_at, updated_at) VALUES
  (1, 101, '/images/bg2.jpg', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 102, '/images/bg1.jpg', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 201, '/images/bg4.jpg', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 취소 정책 ----
INSERT INTO cancellation_policy (policy_id, accommodation_id, policy_name, detail, created_at, updated_at) VALUES
  (1, 1, '무료 취소', '체크인 3일 전까지 전액 환불', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, 1, '부분 환불', '체크인 2일 전 취소 시 50% 환불', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, 2, '무료 취소', '체크인 3일 전까지 전액 환불', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 태그 ----
INSERT INTO accommodation_tag (tag_id, name, category, icon_name) VALUES
  (1, '오션뷰', '전망', 'FaWater'),
  (2, '무료 주차', '편의', 'FaParking'),
  (3, '바비큐', '편의', 'FaFire');

INSERT INTO accommodation_tag_map (map_id, accommodation_id, tag_id) VALUES
  (1, 1, 1),
  (2, 1, 2),
  (3, 2, 2),
  (4, 2, 3);

-- ---- 회원 (이메일 인증 완료) — 비밀번호: Honey1234! ----
INSERT INTO user (user_id, email, password_hash, name, phone, birth_date, gender, marketing_agree, point, role, status,
                  is_verified, created_at, updated_at) VALUES
  (1, 'e2e.user@honeyrest.test', '$2a$10$qXTzNL9boI9cv203LgjZe.5Brrlso5HPR2Er9ni38VbdaV2NKw4QW', '이투이', '01012345678',
   DATE '1995-05-05', 'FEMALE', FALSE, 5000, 'USER', 'ACTIVE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 쿠폰 ----
INSERT INTO coupon (coupon_id, name, code, discount_type, discount_value, min_order_amount, max_discount_amount, target_type, target_id,
                    start_date, end_date, is_active, created_at, updated_at) VALUES
  (1, 'E2E 5천원 할인', 'E2E-5000', 'FIXED', 5000.00, 50000.00, NULL, 'ALL', NULL,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_coupon (user_coupon_id, user_id, coupon_id, status, issued_at, used_at, expired_at) VALUES
  (1, 1, 1, 'ISSUED', CURRENT_TIMESTAMP, NULL, TIMESTAMP '2099-12-31 23:59:59');

-- ---- IDENTITY 시작값 이동 (앱이 만드는 행과 PK 충돌 방지) ----
ALTER TABLE region ALTER COLUMN region_id RESTART WITH 100;
ALTER TABLE accommodation_category ALTER COLUMN category_id RESTART WITH 100;
ALTER TABLE company ALTER COLUMN company_id RESTART WITH 100;
ALTER TABLE accommodation ALTER COLUMN accommodation_id RESTART WITH 100;
ALTER TABLE accommodation_image ALTER COLUMN image_id RESTART WITH 100;
ALTER TABLE room ALTER COLUMN room_id RESTART WITH 1000;
ALTER TABLE room_image ALTER COLUMN image_id RESTART WITH 100;
ALTER TABLE cancellation_policy ALTER COLUMN policy_id RESTART WITH 100;
ALTER TABLE accommodation_tag ALTER COLUMN tag_id RESTART WITH 100;
ALTER TABLE accommodation_tag_map ALTER COLUMN map_id RESTART WITH 100;
ALTER TABLE user ALTER COLUMN user_id RESTART WITH 100;
ALTER TABLE coupon ALTER COLUMN coupon_id RESTART WITH 100;
ALTER TABLE user_coupon ALTER COLUMN user_coupon_id RESTART WITH 100;

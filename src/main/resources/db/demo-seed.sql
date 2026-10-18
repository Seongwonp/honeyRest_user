-- =====================================================================================
-- demo 프로필 시드 데이터 (H2 MySQL 모드 전용) — Render 무료 데모(보여주기용)
-- -------------------------------------------------------------------------------------
-- application-demo.properties 의 spring.sql.init 으로 Hibernate 스키마 생성(create-drop) 직후 실행된다.
-- 컨테이너가 재시작(무료 플랜은 15분 무접속 시 잠듦)될 때마다 이 상태로 초기화된다 → 의도된 동작.
-- 날짜는 CURRENT_DATE 기준 상대값이라 언제 띄워도 "지난 예약 / 다가오는 예약"이 유지된다.
-- 데이터 모양은 호스트 저장소의 db/screenshot-seed.sql(업체·숙소 이름/이메일)과 e2e-seed.sql(컬럼 형식)을 따른다.
--
-- 요약
--   지역      : 서울(종로구·마포구), 부산(해운대구), 제주(제주시·서귀포시), 강원(강릉시·속초시), 경북(경주시)
--   숙소      : 10곳 (ID 1~10), 숙소마다 객실 2개 (객실 ID = 숙소ID*100 + 1, +2)
--   이미지    : /uploads/demo/* (Dockerfile.render 가 uploads/demo 를 이미지에 넣는다)
--               사진(*.jpg)은 프론트엔드 public/images 의 사진을 줄이거나 잘라 만든 것, 일러스트(*.svg)는 데모용으로 그린 것(글자 없음)
--   데모 회원 : demo@honeyrest.app / Demo1234!  (이메일 인증 완료, 포인트 10,200P, 쿠폰 2장)
--               예약 5건: 이용 완료 2(1건 리뷰 작성됨, 1건 리뷰 작성 가능), 예약 확정 2(다가오는 일정), 취소 1
--   그 외     : 리뷰 작성자 회원 4명(비밀번호 없음 → 로그인 불가), 지난 이용 완료 예약 30건 + 리뷰
--   배너/이벤트/태그/취소 정책/쿠폰/찜/문의/포인트 이력
--
-- 비밀번호 해시 재생성 방법은 db/e2e-seed.sql 머리말 참고 (BCrypt cost 10).
-- 명시적 ID 로 넣은 뒤 마지막에 IDENTITY 시작값을 옮겨, 앱이 새로 INSERT 하는 행(가입·예약 등)과 충돌하지 않게 한다.
-- JSON 컬럼은 H2 에서 문자열이 JSON 문자열 값으로 바뀌지 않도록 FORMAT JSON 으로 넣는다.
-- =====================================================================================

-- ---- 지역 ----
INSERT INTO region (region_id, parent_id, name, level, is_popular, image_url) VALUES
  (1, NULL, '서울', 1, TRUE,  '/uploads/demo/region-seoul.jpg'),
  (2, NULL, '부산', 1, TRUE,  '/uploads/demo/region-busan.svg'),
  (3, NULL, '제주', 1, TRUE,  '/uploads/demo/region-jeju.svg'),
  (4, NULL, '강원', 1, TRUE,  '/uploads/demo/region-gangwon.svg'),
  (5, NULL, '경북', 1, FALSE, '/uploads/demo/stay-gyeongju.jpg'),
  (11, 1, '종로구',   2, TRUE,  '/uploads/demo/stay-hanok.jpg'),
  (12, 1, '마포구',   2, FALSE, '/uploads/demo/stay-mapo-night.jpg'),
  (21, 2, '해운대구', 2, TRUE,  '/uploads/demo/region-busan.svg'),
  (31, 3, '제주시',   2, TRUE,  '/uploads/demo/region-jeju.svg'),
  (32, 3, '서귀포시', 2, TRUE,  '/uploads/demo/stay-pension-tangerine.svg'),
  (41, 4, '강릉시',   2, TRUE,  '/uploads/demo/stay-pension-pine.svg'),
  (42, 4, '속초시',   2, FALSE, '/uploads/demo/stay-resort-sea.svg'),
  (51, 5, '경주시',   2, FALSE, '/uploads/demo/stay-gyeongju.jpg');

-- ---- 숙소 카테고리 ----
INSERT INTO accommodation_category (category_id, name, icon_url, sort_order) VALUES
  (1, '호텔', NULL, 1),
  (2, '펜션', NULL, 2),
  (3, '한옥', NULL, 3),
  (4, '리조트', NULL, 4);

-- ---- 업체 (호스트 데모와 같은 이름/이메일 — 단, 두 앱의 DB 는 서로 분리되어 있다) ----
INSERT INTO company (company_id, name, business_number, owner_name, phone, email, address, bank_info, commission_rate, status, created_at, updated_at) VALUES
  (1, '허니레스트 스테이', '123-45-67890', '박성원', '02-1234-5678', 'contact@honeyrest.com', '서울특별시 종로구 북촌로 12',
   '{"bank":"국민은행","account":"123-456-789012"}' FORMAT JSON, 10.00, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '씨사이드 호텔', '234-56-78901', '김바다', '051-987-6543', 'info@seasidehotel.com', '부산광역시 해운대구 해운대해변로 30',
   '{"bank":"부산은행","account":"101-2020-3030-04"}' FORMAT JSON, 12.00, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '네이처 리트리트', '456-78-90123', '최자연', '033-111-2222', 'info@natureretreat.com', '강원특별자치도 강릉시 창해로 350',
   '{"bank":"농협은행","account":"301-0000-1111-22"}' FORMAT JSON, 10.00, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 숙소 ----
INSERT INTO accommodation (accommodation_id, company_id, category_id, main_region_id, sub_region_id, name, address, latitude, longitude,
                           thumbnail, description, amenities, check_in_time, check_out_time, rating, min_price, status) VALUES
  (1, 1, 1, 3, 31, '허니레스트 제주 오션뷰', '제주특별자치도 제주시 애월읍 애월해안로 100', 33.462100, 126.309500,
   '/uploads/demo/stay-hotel-entrance.jpg', '애월 바다가 한눈에 보이는 오션뷰 호텔입니다. 루프탑 수영장에서 노을을 감상해 보세요.',
   '["무료 와이파이","주차장","수영장","조식"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 120000.00, 'ACTIVE'),
  (2, 1, 3, 1, 11, '허니레스트 북촌 한옥', '서울특별시 종로구 북촌로 12', 37.582600, 126.983600,
   '/uploads/demo/stay-hanok.jpg', '북촌 골목 안 전통 한옥 스테이. 마당에서 즐기는 정갈한 한식 조식이 자랑입니다.',
   '["무료 와이파이","조식","정원"]' FORMAT JSON, TIMESTAMP '2025-01-01 16:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 180000.00, 'ACTIVE'),
  (3, 2, 1, 2, 21, '씨사이드 해운대 호텔', '부산광역시 해운대구 해운대해변로 30', 35.158700, 129.160400,
   '/uploads/demo/stay-hotel-lobby.jpg', '해운대 해변 바로 앞 비즈니스 호텔. 해운대역 도보 5분.',
   '["무료 와이파이","주차장","피트니스"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 12:00:00', 0.0, 110000.00, 'ACTIVE'),
  (4, 3, 2, 3, 32, '서귀포 귤밭 펜션', '제주특별자치도 서귀포시 남원읍 태위로 200', 33.276500, 126.703100,
   '/uploads/demo/stay-pension-tangerine.svg', '귤밭 사이의 조용한 독채 펜션. 겨울에는 귤 따기 체험을 할 수 있어요.',
   '["바비큐장","주차장","취사 가능"]' FORMAT JSON, TIMESTAMP '2025-01-01 16:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 95000.00, 'ACTIVE'),
  (5, 3, 2, 4, 41, '강릉 솔숲 펜션', '강원특별자치도 강릉시 창해로 350', 37.795100, 128.915600,
   '/uploads/demo/stay-pension-pine.svg', '송정 해변 솔숲 옆 독채 펜션. 바다까지 걸어서 3분.',
   '["무료 와이파이","주차장","바비큐장"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 130000.00, 'ACTIVE'),
  (6, 3, 4, 4, 42, '속초 바다정원 리조트', '강원특별자치도 속초시 해오름로 120', 38.190200, 128.603500,
   '/uploads/demo/stay-resort-sea.svg', '설악산과 동해를 함께 품은 가족형 리조트. 실내 워터파크 무료 이용.',
   '["무료 와이파이","주차장","워터파크","키즈룸"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 160000.00, 'ACTIVE'),
  (7, 1, 3, 5, 51, '경주 황리단 한옥스테이', '경상북도 경주시 포석로 1080', 35.837400, 129.210400,
   '/uploads/demo/stay-gyeongju.jpg', '황리단길 한가운데 자리한 한옥 스테이. 대릉원·첨성대 도보 10분.',
   '["무료 와이파이","조식","자전거 대여"]' FORMAT JSON, TIMESTAMP '2025-01-01 16:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 140000.00, 'ACTIVE'),
  (8, 2, 1, 1, 12, '마포 어반스테이', '서울특별시 마포구 양화로 45', 37.556800, 126.923600,
   '/uploads/demo/stay-mapo-night.jpg', '홍대입구역 1분 거리의 감각적인 부티크 호텔.',
   '["무료 와이파이","라운지","셀프 체크인"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 89000.00, 'ACTIVE'),
  (9, 2, 4, 2, 21, '해운대 마린 리조트', '부산광역시 해운대구 마린시티2로 38', 35.156200, 129.144700,
   '/uploads/demo/stay-city-skyline.jpg', '마린시티 야경이 펼쳐지는 오션뷰 리조트. 인피니티 풀 운영.',
   '["무료 와이파이","주차장","인피니티 풀","스파"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 210000.00, 'ACTIVE'),
  (10, 1, 2, 3, 31, '제주 애월 스테이', '제주특별자치도 제주시 애월읍 한담서길 15', 33.461300, 126.311800,
   '/uploads/demo/stay-pension-sea.svg', '한담 해안 산책로 앞 감성 펜션. 개별 자쿠지가 있는 객실.',
   '["무료 와이파이","주차장","자쿠지"]' FORMAT JSON, TIMESTAMP '2025-01-01 15:00:00', TIMESTAMP '2025-01-01 11:00:00', 0.0, 150000.00, 'ACTIVE');

-- 숙소 이미지: 대표(MAIN) 1장 + 추가(SUB) 2장 — 숙소마다 다르게 보이도록 직접 지정 (대표 이미지는 accommodation.thumbnail 과 같다)
INSERT INTO accommodation_image (image_id, accommodation_id, image_url, image_type, sort_order, created_at, updated_at) VALUES
  (11, 1, '/uploads/demo/stay-hotel-entrance.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (12, 1, '/uploads/demo/region-jeju.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (13, 1, '/uploads/demo/room-suite.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (21, 2, '/uploads/demo/stay-hanok.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (22, 2, '/uploads/demo/room-ondol.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (23, 2, '/uploads/demo/region-seoul.jpg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (31, 3, '/uploads/demo/stay-hotel-lobby.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (32, 3, '/uploads/demo/region-busan.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (33, 3, '/uploads/demo/room-twin.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (41, 4, '/uploads/demo/stay-pension-tangerine.svg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (42, 4, '/uploads/demo/room-house.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (43, 4, '/uploads/demo/region-jeju.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (51, 5, '/uploads/demo/stay-pension-pine.svg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (52, 5, '/uploads/demo/room-house.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (53, 5, '/uploads/demo/region-gangwon.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (61, 6, '/uploads/demo/stay-resort-sea.svg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (62, 6, '/uploads/demo/room-suite.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (63, 6, '/uploads/demo/region-gangwon.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (71, 7, '/uploads/demo/stay-gyeongju.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (72, 7, '/uploads/demo/room-ondol.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (73, 7, '/uploads/demo/stay-hanok.jpg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (81, 8, '/uploads/demo/stay-mapo-night.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (82, 8, '/uploads/demo/stay-city-skyline.jpg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (83, 8, '/uploads/demo/room-double.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (91, 9, '/uploads/demo/stay-city-skyline.jpg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (92, 9, '/uploads/demo/region-busan.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (93, 9, '/uploads/demo/room-suite.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (101, 10, '/uploads/demo/stay-pension-sea.svg', 'MAIN', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, 10, '/uploads/demo/room-double.svg', 'SUB', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (103, 10, '/uploads/demo/region-jeju.svg', 'SUB', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- 객실 (숙소마다 2개) ----
INSERT INTO room (room_id, accommodation_id, name, type, price, max_occupancy, standard_occupancy, extra_person_fee,
                  bed_info, amenities, description, total_rooms, status, created_at, updated_at) VALUES
  (101, 1, '디럭스 오션뷰 더블', 'DOUBLE', 120000.00, 3, 2, 20000.00, '["더블 베드 1개"]' FORMAT JSON, '["오션뷰","욕조","TV"]' FORMAT JSON, '바다를 정면으로 바라보는 더블룸입니다.', 8, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (102, 1, '오션 스위트',        'SUITE',  280000.00, 4, 2, 30000.00, '["킹 베드 1개"]' FORMAT JSON, '["오션뷰","욕조","라운지 이용"]' FORMAT JSON, '거실이 분리된 스위트룸입니다.', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (201, 2, '사랑채 온돌방',      'ONDOL',  180000.00, 2, 2, 0.00,     '["요 2채"]' FORMAT JSON, '["조식","전통 다과"]' FORMAT JSON, '뜨끈한 온돌 바닥의 전통 객실입니다.', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (202, 2, '안채 가족실',        'FAMILY', 240000.00, 5, 4, 25000.00, '["요 4채"]' FORMAT JSON, '["조식","주방"]' FORMAT JSON, '4인 가족이 머물기 좋은 넓은 안채입니다.', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (301, 3, '스탠다드 더블',      'DOUBLE', 110000.00, 2, 2, 15000.00, '["더블 베드 1개"]' FORMAT JSON, '["TV","냉장고"]' FORMAT JSON, '합리적인 가격의 비즈니스 더블룸입니다.', 20, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (302, 3, '오션 디럭스 트윈',   'TWIN',   160000.00, 3, 2, 20000.00, '["싱글 베드 2개"]' FORMAT JSON, '["오션뷰","욕조"]' FORMAT JSON, '해변 전망 트윈룸입니다.', 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (401, 4, '귤밭 독채 A',        'HOUSE',   95000.00, 4, 2, 10000.00, '["퀸 베드 1개"]' FORMAT JSON, '["취사 가능","바비큐 그릴"]' FORMAT JSON, '귤밭이 보이는 아담한 독채입니다.', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (402, 4, '귤밭 독채 B (복층)', 'HOUSE',  135000.00, 6, 4, 10000.00, '["퀸 베드 2개"]' FORMAT JSON, '["취사 가능","바비큐 그릴","복층"]' FORMAT JSON, '복층 구조의 가족형 독채입니다.', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (501, 5, '솔숲 독채',          'HOUSE',  130000.00, 4, 2, 20000.00, '["퀸 베드 1개"]' FORMAT JSON, '["바비큐 그릴","테라스"]' FORMAT JSON, '솔숲 산책로와 바로 이어지는 독채입니다.', 4, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (502, 5, '오션 테라스 룸',     'DOUBLE', 170000.00, 3, 2, 20000.00, '["더블 베드 1개"]' FORMAT JSON, '["오션뷰","테라스"]' FORMAT JSON, '테라스에서 동해 일출을 볼 수 있어요.', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (601, 6, '패밀리 콘도',        'FAMILY', 160000.00, 6, 4, 15000.00, '["퀸 베드 1개","싱글 베드 2개"]' FORMAT JSON, '["주방","워터파크 이용권"]' FORMAT JSON, '취사 가능한 가족형 콘도입니다.', 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (602, 6, '설악 뷰 스위트',     'SUITE',  250000.00, 4, 2, 25000.00, '["킹 베드 1개"]' FORMAT JSON, '["산 전망","욕조","워터파크 이용권"]' FORMAT JSON, '설악산이 보이는 스위트입니다.', 3, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (701, 7, '별채 한옥 2인실',    'ONDOL',  140000.00, 2, 2, 0.00,     '["요 2채"]' FORMAT JSON, '["조식","전통 차"]' FORMAT JSON, '마당이 딸린 별채 한옥입니다.', 4, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (702, 7, '본채 한옥 4인실',    'FAMILY', 200000.00, 4, 4, 20000.00, '["요 4채"]' FORMAT JSON, '["조식","전통 차","툇마루"]' FORMAT JSON, '툇마루가 있는 넓은 본채입니다.', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (801, 8, '어반 싱글',          'SINGLE',  89000.00, 1, 1, 0.00,     '["싱글 베드 1개"]' FORMAT JSON, '["TV","스마트 TV"]' FORMAT JSON, '혼자 여행하기 좋은 컴팩트룸입니다.', 12, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (802, 8, '어반 더블',          'DOUBLE', 119000.00, 2, 2, 15000.00, '["더블 베드 1개"]' FORMAT JSON, '["스마트 TV","커피 머신"]' FORMAT JSON, '도심 전망 더블룸입니다.', 10, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (901, 9, '마린 오션 디럭스',   'DOUBLE', 210000.00, 3, 2, 30000.00, '["킹 베드 1개"]' FORMAT JSON, '["오션뷰","인피니티 풀 이용"]' FORMAT JSON, '마린시티 야경이 보이는 디럭스룸입니다.', 6, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (902, 9, '펜트하우스 스위트',  'SUITE',  450000.00, 4, 2, 50000.00, '["킹 베드 1개"]' FORMAT JSON, '["오션뷰","개별 스파","라운지 이용"]' FORMAT JSON, '최상층 펜트하우스 스위트입니다.', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1001, 10, '자쿠지 더블',      'DOUBLE', 150000.00, 2, 2, 20000.00, '["퀸 베드 1개"]' FORMAT JSON, '["자쿠지","오션뷰"]' FORMAT JSON, '개별 자쿠지가 있는 더블룸입니다.', 4, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (1002, 10, '패밀리 트윈',      'FAMILY', 190000.00, 4, 3, 20000.00, '["퀸 베드 1개","싱글 베드 1개"]' FORMAT JSON, '["자쿠지","주방"]' FORMAT JSON, '가족 여행객을 위한 트윈룸입니다.', 2, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 객실 이미지: 객실 유형별 실내 일러스트
INSERT INTO room_image (image_id, room_id, image_url, sort_order, created_at, updated_at)
SELECT room_id, room_id,
       CASE type
            WHEN 'ONDOL'  THEN '/uploads/demo/room-ondol.svg'
            WHEN 'SUITE'  THEN '/uploads/demo/room-suite.svg'
            WHEN 'TWIN'   THEN '/uploads/demo/room-twin.svg'
            WHEN 'FAMILY' THEN '/uploads/demo/room-twin.svg'
            WHEN 'HOUSE'  THEN '/uploads/demo/room-house.svg'
            ELSE '/uploads/demo/room-double.svg' END,
       1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM room;

-- ---- 취소 정책 (숙소마다 2개) ----
INSERT INTO cancellation_policy (policy_id, accommodation_id, policy_name, detail, created_at, updated_at)
SELECT a.accommodation_id * 10 + 1, a.accommodation_id, '무료 취소', '체크인 3일 전까지 전액 환불', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM accommodation a;
INSERT INTO cancellation_policy (policy_id, accommodation_id, policy_name, detail, created_at, updated_at)
SELECT a.accommodation_id * 10 + 2, a.accommodation_id, '부분 환불', '체크인 2일 전 취소 시 50% 환불, 이후 환불 불가', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM accommodation a;

-- ---- 태그 ----
INSERT INTO accommodation_tag (tag_id, name, category, icon_name) VALUES
  (1, '오션뷰', '전망', 'FaWater'),
  (2, '무료 주차', '편의', 'FaParking'),
  (3, '바비큐', '편의', 'FaFire'),
  (4, '조식 제공', '식사', 'FaCoffee'),
  (5, '수영장', '시설', 'FaSwimmingPool'),
  (6, '반려동물 동반', '정책', 'FaPaw'),
  (7, '가족 여행', '테마', 'FaChild'),
  (8, '역세권', '위치', 'FaSubway');

INSERT INTO accommodation_tag_map (map_id, accommodation_id, tag_id) VALUES
  (1, 1, 1), (2, 1, 2), (3, 1, 4), (4, 1, 5),
  (5, 2, 4), (6, 2, 8),
  (7, 3, 1), (8, 3, 2), (9, 3, 8),
  (10, 4, 2), (11, 4, 3), (12, 4, 6), (13, 4, 7),
  (14, 5, 1), (15, 5, 2), (16, 5, 3), (17, 5, 6),
  (18, 6, 2), (19, 6, 5), (20, 6, 7),
  (21, 7, 4), (22, 7, 7),
  (23, 8, 8),
  (24, 9, 1), (25, 9, 2), (26, 9, 5),
  (27, 10, 1), (28, 10, 2), (29, 10, 3);

-- ---- 회원 ----
-- 1: 데모 계정 (비밀번호 Demo1234!) / 2~5: 리뷰 작성자 (비밀번호 없음 → 로그인 불가)
INSERT INTO user (user_id, email, password_hash, name, phone, birth_date, gender, marketing_agree, point, role, status,
                  is_verified, created_at, updated_at) VALUES
  (1, 'demo@honeyrest.app', '$2a$10$gt282lGkF1GvhKr.A6BISeDntakggtNrYEQJ7Zf0vcrirBOO6oEky', '김허니', '010-1234-5678',
   DATE '1995-05-05', 'FEMALE', TRUE, 10200, 'USER', 'ACTIVE', TRUE, DATEADD('DAY', -120, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (2, 'minji.kim@example.com',    NULL, '김민지', '010-1111-2222', DATE '1994-03-12', 'FEMALE', FALSE, 0, 'USER', 'ACTIVE', TRUE, DATEADD('DAY', -320, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (3, 'junho.lee@example.com',    NULL, '이준호', '010-2222-3333', DATE '1990-07-01', 'MALE',   FALSE, 0, 'USER', 'ACTIVE', TRUE, DATEADD('DAY', -280, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (4, 'seoyeon.park@example.com', NULL, '박서연', '010-3333-4444', DATE '1998-11-23', 'FEMALE', FALSE, 0, 'USER', 'ACTIVE', TRUE, DATEADD('DAY', -150, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (5, 'hyunwoo.choi@example.com', NULL, '최현우', '010-4444-5555', DATE '1987-01-30', 'MALE',   FALSE, 0, 'USER', 'ACTIVE', TRUE, DATEADD('DAY', -90, CURRENT_TIMESTAMP),  CURRENT_TIMESTAMP);

-- ---- 데모 회원의 예약 5건 ----
INSERT INTO reservation (reservation_id, user_id, room_id, accommodation_id, accommodation_name, room_name, reservation_number,
                         check_in_date, check_out_date, guest_count, guest_name, guest_phone, price, original_price, discount_amount,
                         status, cancel_reason, special_requests, created_at, updated_at) VALUES
  (1, 1, 201, 2, '허니레스트 북촌 한옥', '사랑채 온돌방', 'HR-DEMO-0001',
   DATEADD('DAY', -21, CURRENT_DATE), DATEADD('DAY', -19, CURRENT_DATE), 2, '김허니', '010-1234-5678',
   360000.00, 360000.00, 0.00, 'COMPLETED', NULL, '조식 2인 부탁드립니다.', DATEADD('DAY', -40, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (2, 1, 301, 3, '씨사이드 해운대 호텔', '스탠다드 더블', 'HR-DEMO-0002',
   DATEADD('DAY', -9, CURRENT_DATE), DATEADD('DAY', -8, CURRENT_DATE), 2, '김허니', '010-1234-5678',
   110000.00, 110000.00, 0.00, 'COMPLETED', NULL, NULL, DATEADD('DAY', -20, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (3, 1, 101, 1, '허니레스트 제주 오션뷰', '디럭스 오션뷰 더블', 'HR-DEMO-0003',
   DATEADD('DAY', 14, CURRENT_DATE), DATEADD('DAY', 16, CURRENT_DATE), 2, '김허니', '010-1234-5678',
   240000.00, 240000.00, 0.00, 'CONFIRMED', NULL, '늦은 체크인 예정입니다.', DATEADD('DAY', -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (4, 1, 701, 7, '경주 황리단 한옥스테이', '별채 한옥 2인실', 'HR-DEMO-0004',
   DATEADD('DAY', 30, CURRENT_DATE), DATEADD('DAY', 31, CURRENT_DATE), 2, '김허니', '010-1234-5678',
   130000.00, 140000.00, 10000.00, 'CONFIRMED', NULL, NULL, DATEADD('DAY', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (5, 1, 601, 6, '속초 바다정원 리조트', '패밀리 콘도', 'HR-DEMO-0005',
   DATEADD('DAY', 7, CURRENT_DATE), DATEADD('DAY', 9, CURRENT_DATE), 4, '김허니', '010-1234-5678',
   320000.00, 320000.00, 0.00, 'CANCELLED', '일정 변경으로 취소합니다.', NULL, DATEADD('DAY', -12, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP);

-- ---- 다른 회원들의 지난 이용 완료 예약 30건 (숙소마다 3건 → 리뷰용) ----
INSERT INTO reservation (reservation_id, user_id, room_id, accommodation_id, accommodation_name, room_name, reservation_number,
                         check_in_date, check_out_date, guest_count, guest_name, guest_phone, price, original_price, discount_amount,
                         status, cancel_reason, special_requests, created_at, updated_at)
SELECT 100 + s.x, u.user_id, rm.room_id, a.accommodation_id, a.name, rm.name, 'HR-DEMO-' || (1000 + s.x),
       DATEADD('DAY', -(s.x * 3 + 10), CURRENT_DATE), DATEADD('DAY', -(s.x * 3 + 9), CURRENT_DATE),
       2, u.name, u.phone, rm.price, rm.price, 0.00, 'COMPLETED', NULL, NULL,
       DATEADD('DAY', -(s.x * 3 + 25), CURRENT_TIMESTAMP), DATEADD('DAY', -(s.x * 3 + 9), CURRENT_TIMESTAMP)
  FROM SYSTEM_RANGE(1, 30) s
  JOIN accommodation a ON a.accommodation_id = 1 + MOD(s.x - 1, 10)
  JOIN room rm ON rm.room_id = a.accommodation_id * 100 + 1 + MOD(s.x, 2)
  JOIN user u ON u.user_id = 2 + MOD(s.x, 4);

-- ---- 결제 (+ 결제 상세: 마이페이지 예약 상세 화면이 결제 상세를 함께 조회한다. card_company 는 토스 카드사 코드) ----
INSERT INTO payment (payment_id, reservation_id, user_id, amount, payment_method, payment_status, transaction_id, pg_provider,
                     receipt_url, payment_date, created_at, updated_at)
SELECT r.reservation_id, r.reservation_id, r.user_id, r.price, '카드',
       CASE WHEN r.status = 'CANCELLED' THEN 'CANCELED' ELSE 'DONE' END,
       'demo-tx-' || r.reservation_id, 'TOSS', NULL, r.created_at, r.created_at, r.created_at
  FROM reservation r;

INSERT INTO payment_detail (payment_detail_id, payment_id, card_company, card_number, installment_months, toss_payment_key, created_at, updated_at)
SELECT p.payment_id, p.payment_id, '61', '12345678****000*', 0, 'demo_' || p.payment_id, p.created_at, p.created_at
  FROM payment p;

-- ---- 리뷰 ----
-- 데모 회원: 예약 1(북촌 한옥)만 리뷰 작성, 예약 2(해운대)는 "리뷰 작성" 가능한 상태로 남겨 둔다.
INSERT INTO review (review_id, reservation_id, user_id, accommodation_id, room_id, rating, cleanliness_rating, service_rating, facilities_rating, location_rating,
                    content, reply, like_count, status, created_at, updated_at) VALUES
  (1, 1, 1, 2, 201, 5.0, 5.0, 5.0, 4.5, 5.0, '한옥 특유의 분위기가 너무 좋았어요. 조식도 정갈하고 사장님이 친절하셨습니다.',
   '소중한 후기 감사합니다. 다음에도 편히 쉬다 가세요!', 12, 'PUBLISHED', DATEADD('DAY', -18, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP);

INSERT INTO review (review_id, reservation_id, user_id, accommodation_id, room_id, rating, cleanliness_rating, service_rating, facilities_rating, location_rating,
                    content, reply, like_count, status, created_at, updated_at)
SELECT r.reservation_id, r.reservation_id, r.user_id, r.accommodation_id, r.room_id,
       CASE MOD(r.reservation_id, 4) WHEN 0 THEN 4.0 WHEN 1 THEN 5.0 WHEN 2 THEN 4.5 ELSE 3.5 END,
       4.5, 4.5, 4.0, 5.0,
       CASE MOD(r.reservation_id, 5)
            WHEN 0 THEN '전망이 정말 멋졌어요. 다음에 또 오고 싶어요.'
            WHEN 1 THEN '직원분들이 친절하고 객실이 깨끗했습니다.'
            WHEN 2 THEN '위치는 좋은데 주차가 조금 불편했어요.'
            WHEN 3 THEN '가족 여행으로 딱 좋았습니다. 아이들이 좋아했어요.'
            ELSE '가격 대비 만족스러운 숙소였습니다.' END,
       CASE WHEN MOD(r.reservation_id, 3) = 0 THEN '방문해 주셔서 감사합니다!' ELSE NULL END,
       MOD(r.reservation_id, 9), 'PUBLISHED',
       DATEADD('DAY', 1, CAST(r.check_out_date AS TIMESTAMP)), DATEADD('DAY', 1, CAST(r.check_out_date AS TIMESTAMP))
  FROM reservation r
 WHERE r.reservation_id BETWEEN 101 AND 130;

-- 숙소 평균 평점을 리뷰로부터 계산
UPDATE accommodation a
   SET rating = COALESCE((SELECT ROUND(AVG(rv.rating), 1) FROM review rv
                           WHERE rv.accommodation_id = a.accommodation_id AND rv.status = 'PUBLISHED'), 0.0);

-- ---- 쿠폰 ----
INSERT INTO coupon (coupon_id, name, code, discount_type, discount_value, min_order_amount, max_discount_amount, target_type, target_id,
                    start_date, end_date, is_active, created_at, updated_at) VALUES
  (1, '신규 가입 1만원 할인', 'WELCOME-10000', 'FIXED', 10000.00, 100000.00, NULL, 'ALL', NULL,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '주말 여행 10% 할인', 'WEEKEND-10', 'PERCENT', 10.00, 50000.00, 30000.00, 'ALL', NULL,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '제주 오션뷰 5천원 할인', 'JEJU-5000', 'FIXED', 5000.00, 50000.00, NULL, 'ACCOMMODATION', 1,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 데모 회원: 사용 가능 2장(10% / 제주 5천원) + 사용 완료 1장(예약 4 에 1만원 쿠폰 사용)
INSERT INTO user_coupon (user_coupon_id, user_id, coupon_id, status, issued_at, used_at, expired_at) VALUES
  (1, 1, 1, 'USED',   DATEADD('DAY', -120, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP), TIMESTAMP '2099-12-31 23:59:59'),
  (2, 1, 2, 'ISSUED', DATEADD('DAY', -30, CURRENT_TIMESTAMP), NULL, TIMESTAMP '2099-12-31 23:59:59'),
  (3, 1, 3, 'ISSUED', DATEADD('DAY', -10, CURRENT_TIMESTAMP), NULL, TIMESTAMP '2099-12-31 23:59:59');

-- ---- 포인트 이력 (최종 잔액 = user.point 10,200) ----
INSERT INTO point_history (point_history_id, user_id, amount, type, reason, related_id, balance, expires_at, created_at, updated_at) VALUES
  (1, 1, 5000, 'SAVE', '신규 가입 축하 포인트', NULL, 5000, DATEADD('DAY', 245, CURRENT_TIMESTAMP), DATEADD('DAY', -120, CURRENT_TIMESTAMP), DATEADD('DAY', -120, CURRENT_TIMESTAMP)),
  (2, 1, 3600, 'SAVE', '숙박 완료 적립 (허니레스트 북촌 한옥)', 1, 8600, DATEADD('DAY', 346, CURRENT_TIMESTAMP), DATEADD('DAY', -19, CURRENT_TIMESTAMP), DATEADD('DAY', -19, CURRENT_TIMESTAMP)),
  (3, 1, 500, 'SAVE', '리뷰 작성 적립', 1, 9100, DATEADD('DAY', 347, CURRENT_TIMESTAMP), DATEADD('DAY', -18, CURRENT_TIMESTAMP), DATEADD('DAY', -18, CURRENT_TIMESTAMP)),
  (4, 1, 1100, 'SAVE', '숙박 완료 적립 (씨사이드 해운대 호텔)', 2, 10200, DATEADD('DAY', 357, CURRENT_TIMESTAMP), DATEADD('DAY', -8, CURRENT_TIMESTAMP), DATEADD('DAY', -8, CURRENT_TIMESTAMP));

-- ---- 찜 ----
INSERT INTO wishlist (wishlist_id, user_id, accommodation_id, created_at, updated_at) VALUES
  (1, 1, 9, DATEADD('DAY', -5, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (2, 1, 4, DATEADD('DAY', -4, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (3, 1, 10, DATEADD('DAY', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP);

-- ---- 1:1 문의 ----
INSERT INTO inquiry (inquiry_id, user_id, accommodation_id, category, title, content, reply, is_replied, created_at, updated_at) VALUES
  (1, 1, 1, '예약', '체크인 시간 변경 가능할까요?', '비행기 도착이 늦어 밤 11시쯤 체크인해도 될까요?', '네, 프런트는 24시간 운영합니다. 편하게 오세요!', TRUE,
   DATEADD('DAY', -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP),
  (2, 1, 7, '시설', '자전거 대여 문의', '자전거 대여는 투숙객 무료인가요?', NULL, FALSE, DATEADD('HOUR', -6, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP);

-- ---- 배너 / 이벤트 ----
INSERT INTO banner (banner_id, title, image_url, target_url, position, sort_order, start_date, end_date, is_active, created_at, updated_at) VALUES
  (1, '가을 여행 특가 — 최대 30% 할인', '/uploads/demo/hero-namsan.jpg', '/accommodations', 'MAIN_TOP', 1,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '서울 도심 속 달콤한 휴식', '/uploads/demo/hero-seoul.jpg', '/accommodations', 'MAIN_TOP', 2,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '경주 한옥에서 보내는 하루', '/uploads/demo/hero-gyeongju.jpg', '/accommodations', 'MAIN_TOP', 3,
   TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO event (event_id, title, description, image_url, start_date, end_date, target_url, is_active, created_at, updated_at) VALUES
  (1, '첫 예약 1만원 할인 쿠폰', '신규 회원 첫 예약 시 사용할 수 있는 1만원 할인 쿠폰을 드립니다. (데모 이벤트)',
   '/uploads/demo/event-coupon.jpg', TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', '/accommodations', TRUE,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (2, '리뷰 작성하고 포인트 받기', '이용 완료 후 리뷰를 작성하면 500P 를 적립해 드립니다. (데모 이벤트)',
   '/uploads/demo/event-review.jpg', TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', '/user/mypage', TRUE,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (3, '가을 숙박 할인 이벤트', '10월 한 달 동안 전국 펜션·한옥 숙소를 할인된 가격에 만나 보세요. (데모 이벤트)',
   '/uploads/demo/event-discount.jpg', TIMESTAMP '2020-01-01 00:00:00', TIMESTAMP '2099-12-31 23:59:59', '/accommodations', TRUE,
   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---- IDENTITY 시작값 이동 (앱이 만드는 행과 PK 충돌 방지) ----
ALTER TABLE region ALTER COLUMN region_id RESTART WITH 1000;
ALTER TABLE accommodation_category ALTER COLUMN category_id RESTART WITH 1000;
ALTER TABLE company ALTER COLUMN company_id RESTART WITH 1000;
ALTER TABLE accommodation ALTER COLUMN accommodation_id RESTART WITH 1000;
ALTER TABLE accommodation_image ALTER COLUMN image_id RESTART WITH 1000;
ALTER TABLE room ALTER COLUMN room_id RESTART WITH 10000;
ALTER TABLE room_image ALTER COLUMN image_id RESTART WITH 10000;
ALTER TABLE cancellation_policy ALTER COLUMN policy_id RESTART WITH 1000;
ALTER TABLE accommodation_tag ALTER COLUMN tag_id RESTART WITH 1000;
ALTER TABLE accommodation_tag_map ALTER COLUMN map_id RESTART WITH 1000;
ALTER TABLE user ALTER COLUMN user_id RESTART WITH 1000;
ALTER TABLE reservation ALTER COLUMN reservation_id RESTART WITH 1000;
ALTER TABLE payment ALTER COLUMN payment_id RESTART WITH 1000;
ALTER TABLE payment_detail ALTER COLUMN payment_detail_id RESTART WITH 1000;
ALTER TABLE review ALTER COLUMN review_id RESTART WITH 1000;
ALTER TABLE coupon ALTER COLUMN coupon_id RESTART WITH 1000;
ALTER TABLE user_coupon ALTER COLUMN user_coupon_id RESTART WITH 1000;
ALTER TABLE point_history ALTER COLUMN point_history_id RESTART WITH 1000;
ALTER TABLE wishlist ALTER COLUMN wishlist_id RESTART WITH 1000;
ALTER TABLE inquiry ALTER COLUMN inquiry_id RESTART WITH 1000;
ALTER TABLE banner ALTER COLUMN banner_id RESTART WITH 1000;
ALTER TABLE event ALTER COLUMN event_id RESTART WITH 1000;

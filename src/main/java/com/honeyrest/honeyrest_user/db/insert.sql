INSERT INTO accommodation_category (icon_url, name, sort_order)
VALUES
    ('https://cdn-icons-png.flaticon.com/512/235/235861.png', '호텔/리조트', 1),
    ('https://cdn-icons-png.flaticon.com/512/235/235862.png', '펜션/풀빌라', 2),
    ('https://cdn-icons-png.flaticon.com/512/235/235863.png', '모텔', 3),
    ('https://cdn-icons-png.flaticon.com/512/235/235864.png', '게스트하우스', 4),
    ('https://cdn-icons-png.flaticon.com/512/235/235865.png', '캠핑/글램핑', 5);

-- 취향
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('취향', '감성숙소'),
                                                   ('취향', '연인추천'),
                                                   ('취향', '가족여행숙소'),
                                                   ('취향', '친구끼리'),
                                                   ('취향', '혼자여행'),
                                                   ('취향', '프라이빗'),
                                                   ('취향', '힐링'),
                                                   ('취향', '조용한'),
                                                   ('취향', '파티룸'),
                                                   ('취향', '뷰맛집'),
                                                   ('취향', '인생샷'),
                                                   ('취향', '노을맛집'),
                                                   ('취향', '해돋이명소'),
                                                   ('취향', '야경좋은'),
                                                   ('취향', '숲속숙소'),
                                                   ('취향', '오션뷰'),
                                                   ('취향', '루프탑'),
                                                   ('취향', '테라스'),
                                                   ('취향', '복층구조'),
                                                   ('취향', '감성조명'),
                                                   ('취향', '빈티지풍'),
                                                   ('취향', '미드센추리'),
                                                   ('취향', '한옥스타일'),
                                                   ('취향', '모던인테리어'),
                                                   ('취향', '북유럽풍'),
                                                   ('취향', '자연친화적');

-- 객실 내 시설
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('객실내시설', '스파/월풀'),
                                                   ('객실내시설', '객실스파'),
                                                   ('객실내시설', '미니바'),
                                                   ('객실내시설', '무선인터넷'),
                                                   ('객실내시설', '에어컨'),
                                                   ('객실내시설', '욕실용품'),
                                                   ('객실내시설', '샤워실'),
                                                   ('객실내시설', '개인콘센트'),
                                                   ('객실내시설', 'TV'),
                                                   ('객실내시설', '넷플릭스'),
                                                   ('객실내시설', 'OTT'),
                                                   ('객실내시설', '블루투스스피커'),
                                                   ('객실내시설', '커피머신'),
                                                   ('객실내시설', '냉장고'),
                                                   ('객실내시설', '전자레인지'),
                                                   ('객실내시설', '취사도구'),
                                                   ('객실내시설', '침대형 온돌'),
                                                   ('객실내시설', '침대 2개'),
                                                   ('객실내시설', '소파베드'),
                                                   ('객실내시설', '책상'),
                                                   ('객실내시설', '방음시설'),
                                                   ('객실내시설', '흡연가능'),
                                                   ('객실내시설', '금연객실');

-- 공용 시설
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('시설', '수영장'),
                                                   ('시설', '온수풀'),
                                                   ('시설', '바베큐장'),
                                                   ('시설', '사우나'),
                                                   ('시설', '찜질방'),
                                                   ('시설', '피트니스'),
                                                   ('시설', '레스토랑'),
                                                   ('시설', '카페'),
                                                   ('시설', '매점'),
                                                   ('시설', '공용주방'),
                                                   ('시설', '공용샤워실'),
                                                   ('시설', '공용화장실'),
                                                   ('시설', '공용세탁기'),
                                                   ('시설', '건조기'),
                                                   ('시설', '탈수기'),
                                                   ('시설', '라운지'),
                                                   ('시설', '루프탑바'),
                                                   ('시설', '캠프파이어존'),
                                                   ('시설', '개인사물함'),
                                                   ('시설', '스프링클러'),
                                                   ('시설', '엘리베이터'),
                                                   ('시설', '주차장'),
                                                   ('시설', '전기차충전소');

-- 반려동물
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('반려동물', '반려견동반'),
                                                   ('반려동물', '반려묘동반'),
                                                   ('반려동물', '펫 전용 객실'),
                                                   ('반려동물', '펫 전용 어메니티'),
                                                   ('반려동물', '펫 놀이터'),
                                                   ('반려동물', '펫 동반 레스토랑'),
                                                   ('반려동물', '펫 전용 수영장'),
                                                   ('반려동물', '펫 케어 서비스'),
                                                   ('반려동물', '펫 픽업 가능');

-- 서비스/운영
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('운영정보', '조식제공'),
                                                   ('운영정보', '무료주차'),
                                                   ('운영정보', '픽업서비스'),
                                                   ('운영정보', '짐보관가능'),
                                                   ('운영정보', '비대면체크인'),
                                                   ('운영정보', '당일예약 가능'),
                                                   ('운영정보', '24시간 체크인'),
                                                   ('운영정보', '연박특가'),
                                                   ('운영정보', '쿠폰할인'),
                                                   ('운영정보', '할인특가'),
                                                   ('운영정보', '무한대실'),
                                                   ('운영정보', '얼리체크인'),
                                                   ('운영정보', '레이트체크아웃'),
                                                   ('운영정보', '예약확정 빠름'),
                                                   ('운영정보', '실시간예약'),
                                                   ('운영정보', '블랙회원전용'),
                                                   ('운영정보', '리뷰좋은'),
                                                   ('운영정보', '청결우수'),
                                                   ('운영정보', '방역강화');

-- 숙소등급/분류
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('등급', '5성급'),
                                                   ('등급', '4성급'),
                                                   ('등급', '풀빌라'),
                                                   ('등급', '글램핑'),
                                                   ('등급', '캠핑'),
                                                   ('등급', '게스트하우스'),
                                                   ('등급', '호텔'),
                                                   ('등급', '모텔'),
                                                   ('등급', '리조트'),
                                                   ('등급', '한옥'),
                                                   ('등급', '민박');

-- 위치/접근성
INSERT INTO accommodation_tag (category, name) VALUES
                                                   ('위치', '해변근처'),
                                                   ('위치', '산속'),
                                                   ('위치', '도심'),
                                                   ('위치', '역세권'),
                                                   ('위치', '버스터미널 근처'),
                                                   ('위치', '공항근처'),
                                                   ('위치', '스키장근처'),
                                                   ('위치', '관광지근처'),
                                                   ('위치', '맛집거리'),
                                                   ('위치', '카페거리'),
                                                   ('위치', '쇼핑몰근처'),
                                                   ('위치', '조용한동네'),
                                                   ('위치', '번화가'),
                                                   ('위치', '바닷가 산책로'),
                                                   ('위치', '자전거도로 인접'),
                                                   ('위치', '낚시 가능'),
                                                   ('위치', '트레킹 가능'),
                                                   ('위치', '등산로 인접');


-- 시도 등록
INSERT INTO region (region_id, name, level, is_popular)
VALUES (1, '서울특별시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (2, '부산광역시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (3, '대구광역시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (4, '인천광역시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (5, '광주광역시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (6, '대전광역시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (7, '울산광역시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (8, '세종특별자치시', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (9, '경기도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (10, '강원특별자치도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (11, '충청북도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (12, '충청남도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (13, '전북특별자치도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (14, '전라남도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (15, '경상북도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (16, '경상남도', 1, 1);
INSERT INTO region (region_id, name, level, is_popular)
VALUES (17, '제주특별자치도', 1, 1);

-- 서울특별시 전체 자치구
INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('강남구', 2, 1, 1),
('서초구', 2, 1, 1),
('송파구', 2, 1, 1),
('마포구', 2, 1, 1),
('종로구', 2, 1, 1),
('중구', 2, 1, 1),
('영등포구', 2, 1, 1),
('성동구', 2, 1, 1),
('동작구', 2, 1, 1),
('은평구', 2, 1, 1),
('노원구', 2, 1, 1),
('광진구', 2, 1, 1),
('양천구', 2, 1, 1),
('강서구', 2, 1, 1),

-- 비인기 지역 (주거 중심)
('강북구', 2, 1, 0),
('도봉구', 2, 1, 0),
('중랑구', 2, 1, 0),
('성북구', 2, 1, 0),
('구로구', 2, 1, 0),
('금천구', 2, 1, 0);



INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('해운대구', 2, 2, 1),
('수영구', 2, 2, 1),
('부산진구', 2, 2, 1),
('중구', 2, 2, 1),
('남구', 2, 2, 1),
('동래구', 2, 2, 1),
('기장군', 2, 2, 1),

-- 비인기 지역
('서구', 2, 2, 0),
('동구', 2, 2, 0),
('영도구', 2, 2, 0),
('북구', 2, 2, 0),
('사하구', 2, 2, 0),
('금정구', 2, 2, 0),
('강서구', 2, 2, 0),
('연제구', 2, 2, 0),
('사상구', 2, 2, 0);


INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('중구', 2, 3, 1),
('수성구', 2, 3, 1),
('동구', 2, 3, 1),
('남구', 2, 3, 1),

-- 비인기 지역
('서구', 2, 3, 0),
('북구', 2, 3, 0),
('달서구', 2, 3, 0),
('달성군', 2, 3, 0),
('군위군', 2, 3, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('중구', 2, 4, 1),
('연수구', 2, 4, 1),
('남동구', 2, 4, 1),
('부평구', 2, 4, 1),
('계양구', 2, 4, 1),

-- 비인기 지역
('동구', 2, 4, 0),
('서구', 2, 4, 0),
('미추홀구', 2, 4, 0),
('강화군', 2, 4, 0),
('옹진군', 2, 4, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('동구', 2, 5, 1),
('서구', 2, 5, 1),
('남구', 2, 5, 1),

-- 비인기 지역
('북구', 2, 5, 0),
('광산구', 2, 5, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('서구', 2, 6, 1),
('유성구', 2, 6, 1),
('중구', 2, 6, 1),

-- 비인기 지역
('동구', 2, 6, 0),
('대덕구', 2, 6, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('남구', 2, 7, 1),
('중구', 2, 7, 1),
('동구', 2, 7, 1),

-- 비인기 지역
('북구', 2, 7, 0),
('울주군', 2, 7, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역 (행복도시 중심)
('어진동', 2, 8, 1),
('도담동', 2, 8, 1),
('종촌동', 2, 8, 1),
('아름동', 2, 8, 1),
('고운동', 2, 8, 1),
('보람동', 2, 8, 1),
('새롬동', 2, 8, 1),
('다정동', 2, 8, 1),
('한솔동', 2, 8, 1),

-- 비인기 지역 (외곽 읍·면)
('금남면', 2, 8, 0),
('부강면', 2, 8, 0),
('소정면', 2, 8, 0),
('전의면', 2, 8, 0),
('전동면', 2, 8, 0),
('연서면', 2, 8, 0),
('장군면', 2, 8, 0),
('연기면', 2, 8, 0),
('세종동', 2, 8, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('수원시', 2, 9, 1),
('성남시', 2, 9, 1),
('고양시', 2, 9, 1),
('용인시', 2, 9, 1),
('부천시', 2, 9, 1),
('남양주시', 2, 9, 1),
('안산시', 2, 9, 1),
('화성시', 2, 9, 1),
('평택시', 2, 9, 1),
('의정부시', 2, 9, 1),
('시흥시', 2, 9, 1),
('김포시', 2, 9, 1),
('광명시', 2, 9, 1),
('하남시', 2, 9, 1),

-- 비인기 지역
('군포시', 2, 9, 0),
('오산시', 2, 9, 0),
('이천시', 2, 9, 0),
('안성시', 2, 9, 0),
('양주시', 2, 9, 0),
('구리시', 2, 9, 0),
('여주시', 2, 9, 0),
('동두천시', 2, 9, 0),
('과천시', 2, 9, 0),
('연천군', 2, 9, 0),
('가평군', 2, 9, 0),
('양평군', 2, 9, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('강릉시', 2, 10, 1),
('속초시', 2, 10, 1),
('춘천시', 2, 10, 1),
('원주시', 2, 10, 1),
('동해시', 2, 10, 1),
('태백시', 2, 10, 1),

-- 비인기 지역
('홍천군', 2, 10, 0),
('횡성군', 2, 10, 0),
('평창군', 2, 10, 0),
('정선군', 2, 10, 0),
('영월군', 2, 10, 0),
('삼척시', 2, 10, 0),
('인제군', 2, 10, 0),
('고성군', 2, 10, 0),
('양양군', 2, 10, 0),
('철원군', 2, 10, 0),
('화천군', 2, 10, 0),
('양구군', 2, 10, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('청주시', 2, 11, 1),
('충주시', 2, 11, 1),
('제천시', 2, 11, 1),

-- 비인기 지역
('보은군', 2, 11, 0),
('옥천군', 2, 11, 0),
('영동군', 2, 11, 0),
('증평군', 2, 11, 0),
('진천군', 2, 11, 0),
('괴산군', 2, 11, 0),
('단양군', 2, 11, 0),
('음성군', 2, 11, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('천안시', 2, 12, 1),
('아산시', 2, 12, 1),
('서산시', 2, 12, 1),
('당진시', 2, 12, 1),
('공주시', 2, 12, 1),
('보령시', 2, 12, 1),

-- 비인기 지역
('논산시', 2, 12, 0),
('계룡시', 2, 12, 0),
('금산군', 2, 12, 0),
('부여군', 2, 12, 0),
('서천군', 2, 12, 0),
('청양군', 2, 12, 0),
('홍성군', 2, 12, 0),
('예산군', 2, 12, 0),
('태안군', 2, 12, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('전주시', 2, 13, 1),
('군산시', 2, 13, 1),
('익산시', 2, 13, 1),
('남원시', 2, 13, 1),
('정읍시', 2, 13, 1),

-- 비인기 지역
('김제시', 2, 13, 0),
('완주군', 2, 13, 0),
('고창군', 2, 13, 0),
('부안군', 2, 13, 0),
('임실군', 2, 13, 0),
('순창군', 2, 13, 0),
('진안군', 2, 13, 0),
('무주군', 2, 13, 0),
('장수군', 2, 13, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('여수시', 2, 14, 1),
('순천시', 2, 14, 1),
('목포시', 2, 14, 1),
('광양시', 2, 14, 1),
('나주시', 2, 14, 1),

-- 비인기 지역
('담양군', 2, 14, 0),
('곡성군', 2, 14, 0),
('구례군', 2, 14, 0),
('고흥군', 2, 14, 0),
('보성군', 2, 14, 0),
('화순군', 2, 14, 0),
('장흥군', 2, 14, 0),
('강진군', 2, 14, 0),
('해남군', 2, 14, 0),
('영암군', 2, 14, 0),
('무안군', 2, 14, 0),
('함평군', 2, 14, 0),
('영광군', 2, 14, 0),
('장성군', 2, 14, 0),
('완도군', 2, 14, 0),
('진도군', 2, 14, 0),
('신안군', 2, 14, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('경주시', 2, 15, 1),
('포항시', 2, 15, 1),
('안동시', 2, 15, 1),
('구미시', 2, 15, 1),
('영주시', 2, 15, 1),

-- 비인기 지역
('김천시', 2, 15, 0),
('영천시', 2, 15, 0),
('상주시', 2, 15, 0),
('문경시', 2, 15, 0),
('칠곡군', 2, 15, 0),
('의성군', 2, 15, 0),
('청송군', 2, 15, 0),
('영양군', 2, 15, 0),
('영덕군', 2, 15, 0),
('예천군', 2, 15, 0),
('봉화군', 2, 15, 0),
('울진군', 2, 15, 0),
('울릉군', 2, 15, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('창원시', 2, 16, 1),
('김해시', 2, 16, 1),
('거제시', 2, 16, 1),
('통영시', 2, 16, 1),
('진주시', 2, 16, 1),
('양산시', 2, 16, 1),

-- 비인기 지역
('밀양시', 2, 16, 0),
('사천시', 2, 16, 0),
('의령군', 2, 16, 0),
('함안군', 2, 16, 0),
('창녕군', 2, 16, 0),
('고성군', 2, 16, 0),
('남해군', 2, 16, 0),
('하동군', 2, 16, 0),
('산청군', 2, 16, 0),
('함양군', 2, 16, 0),
('거창군', 2, 16, 0),
('합천군', 2, 16, 0);

INSERT INTO region (name, level, parent_id, is_popular)
VALUES
-- 인기 지역
('제주시', 2, 17, 1),
('서귀포시', 2, 17, 1);



INSERT INTO company (
    created_at, updated_at, address, bank_info,
    business_number, commission_rate, email,
    name, owner_name, phone, status
) VALUES (
             NOW(), NOW(), '서울시 강남구 테헤란로 123',
             JSON_OBJECT('bank', '카카오뱅크', 'account', '3333-12-3456789'),
             '123-45-67890', 10.00, 'contact@honeyrest.com',
             '허니레스트컴퍼니', '박성원', '010-1234-5678', 'ACTIVE'
         );

INSERT INTO accommodation (
    address, amenities, check_in_time, check_out_time,
    description, latitude, longitude, min_price,
    name, rating, status, thumbnail,
    category_id, company_id, main_region_id, sub_region_id
) VALUES (
             '강릉시 주문진읍 해변로 123',
             JSON_ARRAY('무선인터넷', '에어컨', '욕실용품'),
             '2025-08-20 15:00:00',
             '2025-08-21 11:00:00',
             '바다뷰와 감성 인테리어가 돋보이는 펜션입니다.',
             37.805123,
             128.905678,
             120000,
             '강릉 바다뷰 펜션',
             4.8,
             'ACTIVE',
             'https://your-cdn.com/images/thumb.jpg',
             2, -- 펜션/풀빌라
             1, -- 회사 ID
             10, -- 강원도
             101 -- 강릉
         );

INSERT INTO accommodation_tag_map (accommodation_id, tag_id)
VALUES
    (LAST_INSERT_ID(), 1),  -- 바다뷰
    (LAST_INSERT_ID(), 2),  -- 감성숙소
    (LAST_INSERT_ID(), 3);  -- 무료주차


INSERT INTO room (
    created_at, updated_at, amenities, bed_info,
    description, extra_person_fee, max_occupancy,
    name, price, standard_occupancy, status,
    total_rooms, type, accommodation_id
) VALUES (
             NOW(), NOW(),
             JSON_ARRAY('TV', '에어컨', '냉장고'),
             JSON_ARRAY('퀸베드 1개'),
             '바다 전망 객실입니다.',
             20000,
             4,
             '오션뷰 룸 A',
             120000,
             2,
             'ACTIVE',
             5,
             '펜션',
             3
         );

-- 스탠다드 룸 (2인 기준, 최대 3인)
INSERT INTO room (
    created_at, updated_at, amenities, bed_info,
    description, extra_person_fee, max_occupancy,
    name, price, standard_occupancy, status,
    total_rooms, type, accommodation_id
) VALUES (
             NOW(), NOW(),
             JSON_ARRAY('TV', '에어컨', '냉장고'),
             JSON_ARRAY('더블베드 1개'),
             '커플 여행에 적합한 스탠다드 룸입니다.',
             15000,
             3,
             '스탠다드 룸',
             90000,
             2,
             'ACTIVE',
             3,
             '펜션',
             3
         );

-- 디럭스 룸 (4인 기준, 최대 6인)
INSERT INTO room (
    created_at, updated_at, amenities, bed_info,
    description, extra_person_fee, max_occupancy,
    name, price, standard_occupancy, status,
    total_rooms, type, accommodation_id
) VALUES (
             NOW(), NOW(),
             JSON_ARRAY('TV', '에어컨', '냉장고', '전자레인지'),
             JSON_ARRAY('퀸베드 2개'),
             '가족 단위 숙박에 적합한 넓은 디럭스 룸입니다.',
             20000,
             6,
             '디럭스 룸',
             150000,
             4,
             'ACTIVE',
             2,
             '펜션',
             3
         );

-- 패밀리 스위트 (6인 기준, 최대 8인)
INSERT INTO room (
    created_at, updated_at, amenities, bed_info,
    description, extra_person_fee, max_occupancy,
    name, price, standard_occupancy, status,
    total_rooms, type, accommodation_id
) VALUES (
             NOW(), NOW(),
             JSON_ARRAY('TV', '에어컨', '냉장고', '주방', '욕조'),
             JSON_ARRAY('퀸베드 2개', '싱글베드 2개'),
             '대가족이나 단체 여행객을 위한 패밀리 스위트입니다.',
             25000,
             8,
             '패밀리 스위트',
             200000,
             6,
             'ACTIVE',
             1,
             '펜션',
             3
         );

INSERT INTO wishlist (
    created_at, updated_at, accommodation_id, user_id
) VALUES (
             NOW(), NOW(), 3, 16
         );



UPDATE accommodation_tag SET icon_name = 'FaHeart' WHERE tag_id = 1;
UPDATE accommodation_tag SET icon_name = 'FaUserFriends' WHERE tag_id = 2;
UPDATE accommodation_tag SET icon_name = 'FaChild' WHERE tag_id = 3;
UPDATE accommodation_tag SET icon_name = 'FaUsers' WHERE tag_id = 4;
UPDATE accommodation_tag SET icon_name = 'FaUser' WHERE tag_id = 5;
UPDATE accommodation_tag SET icon_name = 'FaLock' WHERE tag_id = 6;
UPDATE accommodation_tag SET icon_name = 'FaSpa' WHERE tag_id = 7;
UPDATE accommodation_tag SET icon_name = 'FaVolumeMute' WHERE tag_id = 8;
UPDATE accommodation_tag SET icon_name = 'FaGlassCheers' WHERE tag_id = 9;
UPDATE accommodation_tag SET icon_name = 'FaBinoculars' WHERE tag_id = 10;
UPDATE accommodation_tag SET icon_name = 'FaCameraRetro' WHERE tag_id = 11;
UPDATE accommodation_tag SET icon_name = 'FaSun' WHERE tag_id = 12;
UPDATE accommodation_tag SET icon_name = 'FaRegSun' WHERE tag_id = 13;
UPDATE accommodation_tag SET icon_name = 'FaCity' WHERE tag_id = 14;
UPDATE accommodation_tag SET icon_name = 'FaTree' WHERE tag_id = 15;
UPDATE accommodation_tag SET icon_name = 'FaWater' WHERE tag_id = 16;
UPDATE accommodation_tag SET icon_name = 'FaBuilding' WHERE tag_id = 17;
UPDATE accommodation_tag SET icon_name = 'FaHome' WHERE tag_id = 18;
UPDATE accommodation_tag SET icon_name = 'FaLayerGroup' WHERE tag_id = 19;
UPDATE accommodation_tag SET icon_name = 'FaLightbulb' WHERE tag_id = 20;
UPDATE accommodation_tag SET icon_name = 'FaWarehouse' WHERE tag_id = 21;
UPDATE accommodation_tag SET icon_name = 'FaCouch' WHERE tag_id = 22;
UPDATE accommodation_tag SET icon_name = 'FaPagelines' WHERE tag_id = 23;
UPDATE accommodation_tag SET icon_name = 'FaThLarge' WHERE tag_id = 24;
UPDATE accommodation_tag SET icon_name = 'FaSnowflake' WHERE tag_id = 25;
UPDATE accommodation_tag SET icon_name = 'FaLeaf' WHERE tag_id = 26;


-- 객실내시설
UPDATE accommodation_tag SET icon_name = 'FaHotTub' WHERE tag_id = 27;
UPDATE accommodation_tag SET icon_name = 'FaSpa' WHERE tag_id = 28;
UPDATE accommodation_tag SET icon_name = 'FaGlassMartiniAlt' WHERE tag_id = 29;
UPDATE accommodation_tag SET icon_name = 'FaWifi' WHERE tag_id = 30;
UPDATE accommodation_tag SET icon_name = 'FaSnowflake' WHERE tag_id = 31;
UPDATE accommodation_tag SET icon_name = 'FaBath' WHERE tag_id = 32;
UPDATE accommodation_tag SET icon_name = 'FaShower' WHERE tag_id = 33;
UPDATE accommodation_tag SET icon_name = 'FaPlug' WHERE tag_id = 34;
UPDATE accommodation_tag SET icon_name = 'FaTv' WHERE tag_id = 35;
UPDATE accommodation_tag SET icon_name = 'FaFilm' WHERE tag_id = 36;
UPDATE accommodation_tag SET icon_name = 'FaPlayCircle' WHERE tag_id = 37;
UPDATE accommodation_tag SET icon_name = 'FaVolumeUp' WHERE tag_id = 38;
UPDATE accommodation_tag SET icon_name = 'FaCoffee' WHERE tag_id = 39;
UPDATE accommodation_tag SET icon_name = 'FaSnowflake' WHERE tag_id = 40;
UPDATE accommodation_tag SET icon_name = 'FaMicrowave' WHERE tag_id = 41;
UPDATE accommodation_tag SET icon_name = 'FaUtensils' WHERE tag_id = 42;
UPDATE accommodation_tag SET icon_name = 'FaBed' WHERE tag_id = 43;
UPDATE accommodation_tag SET icon_name = 'FaBed' WHERE tag_id = 44;
UPDATE accommodation_tag SET icon_name = 'FaCouch' WHERE tag_id = 45;
UPDATE accommodation_tag SET icon_name = 'FaBook' WHERE tag_id = 46;
UPDATE accommodation_tag SET icon_name = 'FaVolumeMute' WHERE tag_id = 47;
UPDATE accommodation_tag SET icon_name = 'FaSmoking' WHERE tag_id = 48;
UPDATE accommodation_tag SET icon_name = 'FaBan' WHERE tag_id = 49;

-- 시설
UPDATE accommodation_tag SET icon_name = 'FaSwimmingPool' WHERE tag_id = 50;
UPDATE accommodation_tag SET icon_name = 'FaWater' WHERE tag_id = 51;
UPDATE accommodation_tag SET icon_name = 'FaFire' WHERE tag_id = 52;
UPDATE accommodation_tag SET icon_name = 'FaHotTub' WHERE tag_id = 53;
UPDATE accommodation_tag SET icon_name = 'FaSpa' WHERE tag_id = 54;
UPDATE accommodation_tag SET icon_name = 'FaDumbbell' WHERE tag_id = 55;
UPDATE accommodation_tag SET icon_name = 'FaUtensils' WHERE tag_id = 56;
UPDATE accommodation_tag SET icon_name = 'FaCoffee' WHERE tag_id = 57;
UPDATE accommodation_tag SET icon_name = 'FaStore' WHERE tag_id = 58;
UPDATE accommodation_tag SET icon_name = 'FaKitchenSet' WHERE tag_id = 59;
UPDATE accommodation_tag SET icon_name = 'FaShower' WHERE tag_id = 60;
UPDATE accommodation_tag SET icon_name = 'FaToilet' WHERE tag_id = 61;
UPDATE accommodation_tag SET icon_name = 'FaTshirt' WHERE tag_id = 62;
UPDATE accommodation_tag SET icon_name = 'FaTshirt' WHERE tag_id = 63;
UPDATE accommodation_tag SET icon_name = 'FaSync' WHERE tag_id = 64;
UPDATE accommodation_tag SET icon_name = 'FaChair' WHERE tag_id = 65;
UPDATE accommodation_tag SET icon_name = 'FaGlassCheers' WHERE tag_id = 66;
UPDATE accommodation_tag SET icon_name = 'FaCampground' WHERE tag_id = 67;
UPDATE accommodation_tag SET icon_name = 'FaLock' WHERE tag_id = 68;
UPDATE accommodation_tag SET icon_name = 'FaFireExtinguisher' WHERE tag_id = 69;
UPDATE accommodation_tag SET icon_name = 'FaSortAmountUp' WHERE tag_id = 70;
UPDATE accommodation_tag SET icon_name = 'FaCar' WHERE tag_id = 71;
UPDATE accommodation_tag SET icon_name = 'FaChargingStation' WHERE tag_id = 72;

-- 반려동물
UPDATE accommodation_tag SET icon_name = 'FaDog' WHERE tag_id = 73;
UPDATE accommodation_tag SET icon_name = 'FaCat' WHERE tag_id = 74;
UPDATE accommodation_tag SET icon_name = 'FaHome' WHERE tag_id = 75;
UPDATE accommodation_tag SET icon_name = 'FaBone' WHERE tag_id = 76;
UPDATE accommodation_tag SET icon_name = 'FaPaw' WHERE tag_id = 77;
UPDATE accommodation_tag SET icon_name = 'FaUtensils' WHERE tag_id = 78;
UPDATE accommodation_tag SET icon_name = 'FaSwimmingPool' WHERE tag_id = 79;
UPDATE accommodation_tag SET icon_name = 'FaHandsHelping' WHERE tag_id = 80;
UPDATE accommodation_tag SET icon_name = 'FaCarSide' WHERE tag_id = 81;

-- 운영정보
UPDATE accommodation_tag SET icon_name = 'FaBreadSlice' WHERE tag_id = 82;
UPDATE accommodation_tag SET icon_name = 'FaParking' WHERE tag_id = 83;
UPDATE accommodation_tag SET icon_name = 'FaCarSide' WHERE tag_id = 84;
UPDATE accommodation_tag SET icon_name = 'FaSuitcase' WHERE tag_id = 85;
UPDATE accommodation_tag SET icon_name = 'FaUserSecret' WHERE tag_id = 86;
UPDATE accommodation_tag SET icon_name = 'FaCalendarDay' WHERE tag_id = 87;
UPDATE accommodation_tag SET icon_name = 'FaClock' WHERE tag_id = 88;
UPDATE accommodation_tag SET icon_name = 'FaTags' WHERE tag_id = 89;
UPDATE accommodation_tag SET icon_name = 'FaTicketAlt' WHERE tag_id = 90;
UPDATE accommodation_tag SET icon_name = 'FaPercent' WHERE tag_id = 91;
UPDATE accommodation_tag SET icon_name = 'FaInfinity' WHERE tag_id = 92;
UPDATE accommodation_tag SET icon_name = 'FaArrowCircleUp' WHERE tag_id = 93;
UPDATE accommodation_tag SET icon_name = 'FaArrowCircleDown' WHERE tag_id = 94;
UPDATE accommodation_tag SET icon_name = 'FaBolt' WHERE tag_id = 95;
UPDATE accommodation_tag SET icon_name = 'FaClock' WHERE tag_id = 96;
UPDATE accommodation_tag SET icon_name = 'FaUserShield' WHERE tag_id = 97;
UPDATE accommodation_tag SET icon_name = 'FaThumbsUp' WHERE tag_id = 98;
UPDATE accommodation_tag SET icon_name = 'FaBroom' WHERE tag_id = 99;
UPDATE accommodation_tag SET icon_name = 'FaShieldVirus' WHERE tag_id = 100;

-- 등급
UPDATE accommodation_tag SET icon_name = 'FaCrown' WHERE tag_id = 101;
UPDATE accommodation_tag SET icon_name = 'FaMedal' WHERE tag_id = 102;
UPDATE accommodation_tag SET icon_name = 'FaHome' WHERE tag_id = 103;
UPDATE accommodation_tag SET icon_name = 'FaCampground' WHERE tag_id = 104;
UPDATE accommodation_tag SET icon_name = 'FaCampground' WHERE tag_id = 105;
UPDATE accommodation_tag SET icon_name = 'FaUserFriends' WHERE tag_id = 106;
UPDATE accommodation_tag SET icon_name = 'FaHotel' WHERE tag_id = 107;
UPDATE accommodation_tag SET icon_name = 'FaBed' WHERE tag_id = 108;
UPDATE accommodation_tag SET icon_name = 'FaUmbrellaBeach' WHERE tag_id = 109;
UPDATE accommodation_tag SET icon_name = 'FaPagelines' WHERE tag_id = 110;
UPDATE accommodation_tag SET icon_name = 'FaHome' WHERE tag_id = 111;

-- 위치
UPDATE accommodation_tag SET icon_name = 'FaUmbrellaBeach' WHERE tag_id = 112;
UPDATE accommodation_tag SET icon_name = 'FaMountain' WHERE tag_id = 113;
UPDATE accommodation_tag SET icon_name = 'FaCity' WHERE tag_id = 114;
UPDATE accommodation_tag SET icon_name = 'FaTrain' WHERE tag_id = 115;
UPDATE accommodation_tag SET icon_name = 'FaBus' WHERE tag_id = 116;
UPDATE accommodation_tag SET icon_name = 'FaPlane' WHERE tag_id = 117;
UPDATE accommodation_tag SET icon_name = 'FaSkiing' WHERE tag_id = 118;
UPDATE accommodation_tag SET icon_name = 'FaLandmark' WHERE tag_id = 119;
UPDATE accommodation_tag SET icon_name = 'FaUtensils' WHERE tag_id = 120;
UPDATE accommodation_tag SET icon_name = 'FaCoffee' WHERE tag_id = 121;
UPDATE accommodation_tag SET icon_name = 'FaShoppingBag' WHERE tag_id = 122;
UPDATE accommodation_tag SET icon_name = 'FaHome' WHERE tag_id = 123;
UPDATE accommodation_tag SET icon_name = 'FaMapMarkerAlt' WHERE tag_id = 124;
UPDATE accommodation_tag SET icon_name = 'FaWalking' WHERE tag_id = 125;
UPDATE accommodation_tag SET icon_name = 'FaBicycle' WHERE tag_id = 126;
UPDATE accommodation_tag SET icon_name = 'FaFish' WHERE tag_id = 127;
UPDATE accommodation_tag SET icon_name = 'FaHiking' WHERE tag_id = 128;
UPDATE accommodation_tag SET icon_name = 'FaMountain' WHERE tag_id = 129;
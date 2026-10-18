-- review.status 정규화: 과거 호스트 관리자 앱이 노출 상태로 저장하던 'VISIBLE' 을 'PUBLISHED' 로 바꾼다.
-- 사용자 API 는 리뷰를 PUBLISHED 로 저장하고 PUBLISHED 만 사용자 화면(숙소/객실 상세, 검색 리뷰 수)과
-- 평점 집계(AccommodationService.updateRating)에 쓴다. 그래서 VISIBLE 로 남은 행은
--   1) 사용자 사이트에서 보이지 않고 평점에서도 빠졌으며
--   2) 호스트는 읽을 때 VISIBLE 을 노출로 취급하는 호환 코드를 유지해야 했다.
-- 이제 저장 가능한 값은 PUBLISHED / HIDDEN / DELETED 뿐이다 (공유 도메인 com.honeyrest.domain.type.ReviewStatus).
--
-- 1) VISIBLE(대소문자·앞뒤 공백 무관) → PUBLISHED
-- 2) 허용 값의 대소문자·공백 변형('published', ' HIDDEN ' 등)을 대문자 정규형으로 맞춘다.
--    MySQL 기본 콜레이션은 대소문자를 구분하지 않아 조회는 이미 맞지만, 값 자체를 정규형으로 두어
--    애플리케이션의 대소문자 구분 비교(Java equals 등)와 어긋나지 않게 한다. BINARY 비교로 실제로 다른 행만 바꾼다.
-- 그 밖의 알 수 없는 값은 건드리지 않는다(사용자 화면에는 PUBLISHED 가 아니므로 노출되지 않는다).
-- 두 UPDATE 모두 조건에 맞는 행이 없으면 아무것도 바꾸지 않으므로 V10/V11 과 같이 재실행해도 안전하다.

UPDATE `review`
SET `status` = 'PUBLISHED'
WHERE UPPER(TRIM(`status`)) = 'VISIBLE';

UPDATE `review`
SET `status` = UPPER(TRIM(`status`))
WHERE UPPER(TRIM(`status`)) IN ('PUBLISHED', 'HIDDEN', 'DELETED')
  AND BINARY `status` <> BINARY UPPER(TRIM(`status`));

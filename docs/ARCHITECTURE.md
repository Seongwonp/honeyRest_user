# 🗂️ HoneyRest – 아키텍처 & DB 설계

---

## 프로젝트 구조

```plaintext
HoneyRest/
├── honeyrest_user/         # User API 백엔드 (Spring Boot, Gradle 멀티 프로젝트) ← 현재 레포
│   ├── honeyrest-domain/   # 공유 도메인 모듈 (:honeyrest-domain) — 관리자 앱도 submodule 로 사용
│   │   └── src/main/java/com/honeyrest/domain/
│   │       ├── entity/         # JPA 엔티티 30개 + BaseEntity, ReservationChanges
│   │       └── type/           # ReservationStatus, BannerPosition
│   ├── src/main/java/
│   │   └── com/honeyrest/honeyrest_user/
│   │       ├── config/         # Security, Redis, Firebase, Swagger 설정
│   │       ├── controller/     # REST API 컨트롤러
│   │       ├── service/        # 비즈니스 로직
│   │       ├── repository/     # JPA + QueryDSL 리포지토리
│   │       ├── dto/            # 요청/응답 DTO
│   │       ├── exception/      # GlobalExceptionHandler, ApiException
│   │       └── storage/        # 파일 저장소 추상화 (FileStorage: local / firebase)
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-local.properties     # 로컬 개발 프로필 (SQL 로그 등)
│   │   ├── application_security.properties  (gitignore)
│   │   └── db/migration/
│   │       └── V1__baseline.sql             # Flyway 스키마
│   └── src/test/                            # JUnit5 단위 테스트
│
├── honeyrest_user_react/   # User 프론트엔드 (React) → 별도 레포
└── honeyRest_host/         # Admin 시스템 (Thymeleaf) → 별도 레포
```

---

## 🗃️ 데이터베이스 설계 (ERD)

HoneyRest의 데이터베이스는 사용자, 숙소, 예약, 리뷰, 관리자 기능까지 포함하는
**도메인 중심의 관계형 구조**로 설계되었습니다.
정규화된 테이블 구조로 확장성과 무결성을 확보하고, JPA 기반 ORM으로 엔티티와 DB를 연결합니다.

### ERD 이미지

![HoneyRest ERD](https://github.com/user-attachments/assets/f9be0cb6-0e3b-43c4-800c-58c6c78a55f5)

---

## 📋 주요 테이블 요약

| 테이블명 | 설명 |
|----------|------|
| `user` | 사용자 정보, 권한, 소셜 로그인, 포인트 |
| `accommodation` | 숙소 정보, 위치, 카테고리, 태그, 이미지 |
| `room` | 객실 정보, 재고, 기준/최대 인원, 가격 |
| `reservation` | 예약 내역, 상태, 체크인/아웃, 결제 금액 |
| `payment` | 결제 정보 (Toss), 결제 수단, 상태 |
| `payment_detail` | 카드 / 가상계좌 상세 |
| `review` | 리뷰 내용, 평점, 좋아요, 신고 여부 |
| `coupon` / `user_coupon` | 할인 쿠폰 정의 및 사용자 보유 현황 |
| `region` | 지역 마스터 (계층 구조: 도 → 시/군) |
| `accommodation_category` | 숙소 카테고리 (호텔, 펜션, 한옥 등) |
| `company` | 업체 정보 (사업자 번호, 수수료율 등) |
| `banner` | 메인 페이지 배너 이미지 |
| `event` | 이벤트/프로모션 정보 |
| `wish_list` | 관심 숙소 저장 |
| `notification` | 이메일/앱 알림 내역 |

> 전체 컬럼 상세 명세: [DB_SCHEMA.md](../DB_SCHEMA.md)

---

## 🧠 설계 포인트

### 정규화 & 무결성
- 중복 최소화, FK 제약 조건으로 데이터 정합성 보장
- `reservation_number` UNIQUE — 예약 코드 중복 방지

### 확장성
- `region` 계층 구조 (`parent_id` 자기 참조) — 도 / 시 / 구 단계별 확장 가능
- `coupon.target_type` + `target_id` 조합 — 숙소/카테고리/전체 쿠폰 유연 대응

### 성능
- Redis ZSet: 카테고리별 인기 숙소 조회수 관리 (`popular:accommodation:{category}`)
- `@Cacheable("banners")`: 배너 목록 캐싱으로 반복 쿼리 제거
- 캐시 키·TTL·무효화 트리거는 아래 "캐시 키 & 무효화" 표 참고
- JOIN FETCH: `accommodation` + `category` N+1 해결

### 캐시 키 & 무효화

Redis `KEYS` 명령은 사용하지 않는다(운영 Redis 블로킹). 숙소 단위 키는 `service/redis/AccommodationCacheKeys` 에서만 만든다.

| 캐시 | 키 | TTL | 무효화 트리거 |
|------|----|-----|--------------|
| 숙소 상세 | `accommodation:detail:{id}` | 없음 | 리뷰 작성/수정/삭제 → `RatingCacheService.evictAllAccommodationCache` |
| 숙소 이미지 | `accommodation:images:{id}` (List) | 없음 | 〃 |
| 숙소 태그(상세용) | `accommodation:tags:{id}` | 없음 | 〃, 태그 매핑 추가/삭제 |
| 숙소 태그맵 | `accommodation:tagmap:{id}` | 6h | 〃, 태그 매핑 추가/삭제 (`AccommodationTagMapService`) |
| 평점 | `accommodation:rating:{id}` | 없음 | 리뷰 작성/수정/삭제 |
| 리뷰 수 | `reviewCount:accommodation:{id}` | 3m | 리뷰 작성/수정/삭제 |
| 리뷰 목록 | `reviewList:accommodation:{id}` | 5m | 리뷰 작성/수정/삭제 (좋아요 수는 TTL 동안 지연 반영) |
| 취소 정책 | `cancellationPolicy:accommodation:{id}` | 없음 | 리뷰 변경 시 함께 삭제 (정책 변경은 호스트 저장소 몫) |
| 검색 결과 | `search:recommend:v3:ver={N}:…조건…` (+`:total`) | 6h | `search:recommend:version` INCR (커밋 후): 예약 생성, 리뷰 작성/수정/삭제, 태그 매핑 변경 |
| 리뷰 좋아요 수 | `review:like:{reviewId}` | 없음 | 좋아요 설정/해제 시 증감, DB `like_count` 에도 반영 |
| 리뷰 좋아요 사용자 | `review:like:users:{reviewId}` (Set) | 없음 | 좋아요 설정/해제 |
| 인기 숙소 | `popular:accommodation:{category}` (ZSet) | 없음 | 조회 시 점수 증가, 전체 조회는 `SCAN` |
| 지역/태그/카테고리 목록 | `RegionService`·`accommodation:tags:all`·`category:accommodation` | 12h/12h/없음 | 없음 (정적 데이터) |
| 배너 | `@Cacheable("banners")` | `spring.cache.redis.time-to-live` | `@CacheEvict("banners", allEntries)` — `saveBanner` |
| 메일 발송 한도 | `rate:email:{용도}:{이메일}` | 10m | 윈도 만료 |

- 검색 결과 캐시는 체크인·인원·필터·페이지 조합이라 예약 1건이 영향을 주는 키를 특정할 수 없으므로 **세대 번호(key versioning)** 로 한꺼번에 무효화한다. 이전 세대 키는 TTL 로 소멸한다.
- 사용자 측 취소 요청(`CANCEL_REQUEST`)은 재고를 계속 점유하므로 세대를 올리지 않는다. **호스트 저장소에서 예약을 `CANCELLED` 로 바꿀 때 `search:recommend:version` 을 INCR 해야** 검색 재고가 즉시 반영된다 (그 전까지는 최대 6h 지연).
- 검색 결과의 찜 여부(user 별 키)는 찜 토글 시 무효화하지 않는다 (최대 6h 지연, 알려진 한계).

### 보안
- JWT Access/Refresh 토큰 분리, RefreshToken DB 저장
- 민감 정보 `application_security.properties` 분리 (gitignore)
- 권한 기반 접근 제어 (USER / ADMIN / COMPANY)

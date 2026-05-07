# 🗂️ HoneyRest – 아키텍처 & DB 설계

---

## 프로젝트 구조

```plaintext
HoneyRest/
├── honeyrest_user/         # User API 백엔드 (Spring Boot) ← 현재 레포
│   ├── src/main/java/
│   │   └── com/honeyrest/honeyrest_user/
│   │       ├── config/         # Security, Redis, Firebase, Swagger 설정
│   │       ├── controller/     # REST API 컨트롤러
│   │       ├── service/        # 비즈니스 로직
│   │       ├── repository/     # JPA + QueryDSL 리포지토리
│   │       ├── entity/         # JPA 엔티티
│   │       ├── dto/            # 요청/응답 DTO
│   │       ├── exception/      # GlobalExceptionHandler, ApiException
│   │       └── batch/          # Spring Batch Job
│   ├── src/main/resources/
│   │   ├── application.properties
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
- JOIN FETCH: `accommodation` + `category` N+1 해결

### 보안
- JWT Access/Refresh 토큰 분리, RefreshToken DB 저장
- 민감 정보 `application_security.properties` 분리 (gitignore)
- 권한 기반 접근 제어 (USER / ADMIN / COMPANY)

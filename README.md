# 🐝 HoneyRest – 감성 숙소 예약 플랫폼 (User API)
👨‍💻 Created by 박성원 (Seongwon Park) – User 영역 총괄 & 팀장

## 🎥 HoneyRest 광고 영상

[![HoneyRest 광고 영상](https://github.com/Seongwonp/honeyRest_user/blob/main/%E1%84%92%E1%85%A5%E1%84%82%E1%85%B5%E1%84%85%E1%85%A6%E1%84%89%E1%85%B3%E1%84%90%E1%85%B3.gif?raw=true)](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2F%E1%84%92%E1%85%A5%E1%84%82%E1%85%B5%E1%84%85%E1%85%A6%E1%84%89%E1%85%B3%E1%84%90%E1%85%B3.mp4?alt=media&token=1d89a752-00e0-4c82-b6c0-94723c57cc70)

> 🎬 클릭하면 전체 광고 영상을 볼 수 있습니다.

---

## 📖 목차

- [📌 프로젝트 개요](#-프로젝트-개요)
- [🎥 광고 영상](#-honeyrest-광고-영상)
- [🧑‍💻 주요 기능](#-주요-기능)
- [🗂️ 프로젝트 구조](#-프로젝트-구조)
- [🗃️ 데이터베이스 설계 (ERD)](#-데이터베이스-설계-erd)
- [📋 주요 테이블 요약](#-주요-테이블-요약)
- [🧠 설계 포인트](#-설계-포인트)
- [📦 기술 스택 (User API 기준)](#-기술-스택-user-api-기준)
- [⚙️ 실행 전 필수 설정](#️-실행-전-필수-설정)
- [🖥️ 주요 화면 캡처](#-주요-화면-캡처)
- [🎬 사용자 시연 영상](#-사용자-시연-영상)
- [📝 프로젝트 발표 자료](#-프로젝트-발표-자료)
- [🛡️ 안정화](#-안정화)
- [🧪 테스트](#-테스트)
- [⚡ 성능 최적화](#-성능-최적화)
- [🗄️ DB 안정화](#-db-안정화)
- [💭 프로젝트 회고](#-프로젝트-회고)
- [🙋‍♂️ 개발자 정보](#-개발자-정보)

---

## 📌 프로젝트 개요

**HoneyRest**는 감성 숙소 예약을 위한 **풀스택 웹 플랫폼**입니다.  
사용자(User), 업체 관리자(Company Admin), 총 관리자(Super Admin)로 역할을 분리하여  
숙소 검색부터 예약, 리뷰 작성, 결제, 관리자 통계까지  
**전체 사용자 흐름을 하나의 시스템으로 통합 구현했습니다.**

- **프로젝트 기간**: 2025.08.04 ~ 2025.09.04 (총 4주)
- **팀원 구성**:
  - 👤 박성원 (팀장) – 사용자(User) 영역 개발 총괄 & 광고 영상 제작 / 전체 DB 설계 및 ERD 작성 / 전체 시스템 통합 및 코드 리뷰
  - 🏨 김민경 – 업체 관리자(Company Admin) 시스템 개발
  - 🛡️ 설현오 – 총 관리자(Super Admin) 시스템 개발

> 사용자 중심의 UI와 확장 가능한 백엔드 구조로  
> 감성 숙소 예약 경험을 기술적으로 완성했습니다.

---

## 🎥 HoneyRest 광고 영상

> 감성 숙소 예약 플랫폼 HoneyRest의 분위기를 담은 광고 영상입니다.

[📺 광고 영상 보러가기](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2F%E1%84%92%E1%85%A5%E1%84%82%E1%85%B5%E1%84%85%E1%85%A6%E1%84%89%E1%85%B3%E1%84%90%E1%85%B3.mp4?alt=media&token=1d89a752-00e0-4c82-b6c0-94723c57cc70)

---

## 🧑‍💻 주요 기능

### 👤 사용자 (User)
- 숙소 검색 및 예약
- 관심 숙소 등록
- 리뷰 작성 및 확인
- 마이페이지 관리
- Redis 기반 추천/검색 캐싱

### [🏨 업체 관리자 (Company Admin)](https://github.com/Seongwonp/honeyRest_host)
- 숙소 등록/수정/삭제
- 객실 재고 및 가격 캘린더 관리
- 예약 현황 및 리뷰 응답
- 매출 통계 대시보드

### [🛡️ 총 관리자 (Super Admin)](https://github.com/Seongwonp/honeyRest_host)
- 전체 숙소/예약/유저 관리
- 업체 관리자 계정 생성 및 권한 부여
- 신고 리뷰 처리 및 운영 정책 관리

---

## 🗂️ 프로젝트 구조

```plaintext
HoneyRest/
├── frontend/               # 사용자 프론트엔드 (React)
│  
├── user-api-backend/       # 사용자 API 백엔드 (Spring Boot)
│   
├── admin/                  # 관리자 시스템 (Thymeleaf 기반)
│
└── README.md               # 전체 프로젝트 설명
```

- [User_React 바로가기](https://github.com/Seongwonp/honeyrest_user_react)
- [Admin 바로가기](https://github.com/Seongwonp/honeyRest_host)

---

## 🗃️ 데이터베이스 설계 (ERD)

HoneyRest의 데이터베이스는 사용자, 숙소, 예약, 리뷰, 관리자 기능까지 모두 포함하는  
**도메인 중심의 관계형 구조**로 설계되었습니다. 확장성과 무결성을 고려해 정규화된 테이블로 구성되어 있으며,  
JPA 기반의 ORM 매핑을 통해 엔티티와 DB가 유기적으로 연결됩니다.

### 📌 ERD 이미지

![HoneyRest ERD](https://github.com/user-attachments/assets/f9be0cb6-0e3b-43c4-800c-58c6c78a55f5)

> 📄 자세한 ERD 구조는 PDF 문서에서 확인 가능합니다.

---

### 📋 주요 테이블 요약

| 테이블명 | 설명 |
|----------|------|
| `User` | 사용자 정보, 권한, 알림 설정 등 |
| `Accommodation` | 숙소 정보, 위치, 태그, 이미지 |
| `Room` | 객실 정보, 재고, 가격, 캘린더 |
| `Reservation` | 예약 내역, 상태, 결제 정보 |
| `Review` | 리뷰 내용, 평점, 신고 여부 |
| `Admin` | 관리자 계정, 권한, 통계 |
| `Coupon` | 할인 쿠폰, 조건, 유저 연결 |
| `Notification` | 이메일/앱 알림 내역 |
| `WishList` | 관심 숙소 저장 기능 |
| `Policy` | 취소 정책, 운영 기준 |

---

### 🧠 설계 포인트

- **정규화**: 중복 최소화, 관계 명확화
- **확장성**: 관리자 기능, 쿠폰, 알림 등 확장 가능한 구조
- **보안**: 토큰 관리, 민감 정보 분리, 권한 기반 접근 제어
- **성능**: Redis 캐싱, 인기 숙소 조회수 ZSet 구조 활용

---

> "데이터베이스는 서비스의 뼈대입니다. HoneyRest는 그 뼈대를 세심하게 설계했습니다."

---

## 📦 기술 스택 (User API 기준)

| 구분 | 기술 / 라이브러리 | 역할 / 설명 | 사용처 / 특징 |
|------|------------------|-------------|--------------|
| **프레임워크** | Spring Boot | 백엔드 애플리케이션 프레임워크 | REST API, DI, 설정 관리 |
| **보안 / 인증** | Spring Security | 로그인 / 권한 관리 | JWT 인증, 소셜 로그인(Google, Kakao) |
| **JWT** | jjwt | Access/Refresh 토큰 관리 | 만료 관리, 재발급 |
| **OAuth2** | Spring Security OAuth2 | 소셜 로그인 | 인증, 사용자 정보 조회, redirect 처리 |
| **DB** | MariaDB | 관계형 데이터 저장소 | JPA 기반 CRUD, QueryDSL 가능 |
| **ORM** | Spring Data JPA | 엔티티 → DB 매핑 | 엔티티 기반 설계 |
| **캐시** | Redis | 인기 숙소 / 지역 캐시, 조회수 관리 | ZSet, TTL 활용 |
| **파일 업로드** | Firebase Storage | 이미지 / 파일 저장 | 숙소 이미지, 프로필 이미지 |
| **메일 발송** | Spring Mail (Gmail SMTP) | 인증 / 알림 이메일 | 이메일 인증, 알림, 비밀번호 재설정 |
| **API 문서화** | SpringDoc OpenAPI / Swagger UI | REST API 문서 자동화 | `/api-docs`, `/swagger-ui.html` |
| **환경 / 보안 관리** | application_security.properties | 민감 정보 분리 | DB 패스워드, Firebase 키, JWT 시크릿 등 |

---

## ⚙️ 실행 전 필수 설정

HoneyRest 프로젝트를 실행하기 위해 아래 설정을 반드시 완료해주세요.

---

### 1️⃣ Redis 설치 및 실행

HoneyRest는 Redis를 캐싱 서버로 사용합니다.

#### 📌 macOS (Homebrew)

```bash
brew install redis
brew services start redis   # Redis 실행
brew services stop redis    # Redis 중지
```

#### 📌 Ubuntu / Linux

```bash
sudo apt update
sudo apt install redis-server
sudo systemctl enable redis-server.service
sudo systemctl start redis-server
```

#### 📌 Windows
- Redis for Windows (예: Memurai) 설치 후 실행
- 실행 후 기본 포트 6379 사용

#### 설치 확인
```bash
redis-cli ping
# PONG 이 출력되면 정상 실행
```

---

### 2️⃣ 환경 변수 설정 (`application_security.properties`)

본 프로젝트 실행을 위해서는 민감 정보가 담긴 설정 파일을 반드시 직접 작성해야 합니다.  
아래 항목들은 실제 서비스 키 값으로 교체해주세요.

```properties
# Toss Payments widget credentials
com.tjfgusdh.toss.widgetClientKey=YOUR_TOSS_CLIENT_KEY
com.tjfgusdh.toss.widgetSecretKey=YOUR_TOSS_SECRET_KEY

# Database password
spring.datasource.password=YOUR_DB_PASSWORD

# JWT secret key
jwt.secret-key-value=YOUR_SECURE_JWT_SECRET_KEY

# OpenWeather API
weather.api.key=YOUR_OPENWEATHER_API_KEY

# Firebase secretKey.json
fire.base.secretKey=YOUR_SECRET_KEY

# Google OAuth2
google.client-id=YOUR_GOOGLE_CLIENT_ID
google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# Kakao OAuth2
kakao.client-id=YOUR_KAKAO_CLIENT_ID
kakao.client-secret=YOUR_KAKAO_CLIENT_SECRET

# Gmail SMTP
gmail.username=YOUR_GMAIL_ADDRESS
gmail.access-token=YOUR_GMAIL_APP_PASSWORD
```

#### 🚨 주의사항

- `application_security.properties`는 반드시 `.gitignore`에 포함해야 합니다.
- 깃허브 공개 저장소에는 절대 올리지 마세요.
- 팀 협업 시에는 `application_security.properties.example` 파일로 형식만 공유하고, 실제 키 값은 각 개발자가 직접 채워넣어야 합니다.

---

## 🖥️ 주요 화면 캡처

> 사용자 중심의 직관적인 UI와 예약 흐름을 제공합니다.

### 🏠 메인 페이지

![메인 화면](https://github.com/user-attachments/assets/a5e85b19-a87c-47b0-8b9b-91e9d3db2dd1)

- 숙소 검색, 지역 필터, 추천 태그 등 핵심 기능이 배치된 메인 화면
- 반응형 디자인으로 모바일에서도 최적화된 UI 제공

### 📅 예약 흐름

> 숙소 선택 → 객실 확인 → 날짜 선택 → 결제 → 예약 완료

- Toss Payments 연동으로 간편 결제
- 예약 후 이메일 알림 및 마이페이지에서 확인 가능

### 🧑‍💼 마이페이지

- 예약 내역, 리뷰 작성, 프로필 수정, 쿠폰 확인 등
- 사용자 경험을 고려한 기능 배치와 인터페이스 구성

> 추가 화면은 아래 시연 영상에서 확인 가능합니다.

---

## 🎬 사용자 시연 영상

HoneyRest의 사용자 흐름을 실제 화면 기반으로 시연한 영상입니다.  
기능별로 나누어 숙소 검색, 예약, 결제, 마이페이지, 회원가입 등  
전체 사용자 경험을 확인할 수 있습니다.

---

### 📺 숙소 검색 → 예약 → 결제 흐름

> 사용자가 숙소를 검색하고, 객실을 선택해 결제까지 진행하는 전체 흐름을 담은 영상입니다.

🔗 [시연 영상 보러가기](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2FHoneyRest_Pay.mp4?alt=media&token=b96a6897-b48b-4138-a5cf-fdd2e53caefb)

---

## 📝 프로젝트 발표 자료

> HoneyRest의 전체 기획, 기능 흐름, 기술 스택, 시연 화면 등을 담은 발표용 PPT입니다.  
> 자세한 내용은 아래 PDF를 참고해주세요.

📄 [HoneyRest 발표 자료 (PDF)](https://github.com/user-attachments/files/22292418/HoneyRest.pdf)

---

## 🛡️ 안정화

프로젝트 완료 후 서비스 안정성을 높이기 위한 안정화 작업을 진행했습니다.

### 📋 DTO 입력 검증

컨트롤러 레이어에서 잘못된 요청을 조기에 차단합니다.

| DTO | 주요 검증 규칙 |
|-----|--------------|
| `UserSignupRequestDTO` | `@Email`, `@NotBlank`, `@Size(min=8)` (비밀번호), `@Pattern` (전화번호, 생년월일) |
| `UserLoginRequestDTO` | `@NotBlank`, `@Email` |
| `ReservationRequestDTO` | `@NotNull` (roomId, checkIn, checkOut), `@Min(1)` (guests), `@NotBlank` (guestName, guestPhone) |
| `ReviewRequestDTO` | `@DecimalMin("1.0")` ~ `@DecimalMax("5.0")` (평점), `@Size(min=10, max=2000)` (내용) |
| `TossConfirmRequest` | `@NotBlank` (paymentKey, orderId), `@Positive` (amount), `@Valid` (중첩 DTO 연계 검증) |
| `GuestReservationLookupRequestDTO` | `@NotBlank` (reservationCode, guestPhone, guestPassword) |

- 각 컨트롤러 메서드에 `@Valid` 추가 (`AuthController`, `ReviewController`, `PaymentController`, `ReserveController`)

### 🚨 GlobalExceptionHandler 보완

| 예외 타입 | HTTP 상태 | 처리 방식 |
|---------|----------|---------|
| `MethodArgumentNotValidException` | 400 | 필드별 에러 메시지 Map으로 반환 |
| `ConstraintViolationException` | 400 | violation 목록 문자열로 반환 |
| `ApiException` | 커스텀 | 기존 유지 |
| `IllegalArgumentException` | 400 | 기존 유지 |
| `IllegalStateException` | 410 | 기존 유지 |
| `Exception` | 500 | 기존 유지 (맨 마지막 폴백) |

### 🔌 HikariCP 커넥션 풀 명시 설정

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

> Spring Boot 기본값 의존을 제거하고 운영 환경에 맞는 풀 사이즈를 명시합니다.

### 📧 이메일 알림 트랜잭션 분리

Toss 결제 승인 후 이메일 전송은 `TransactionTemplate.execute()` 블록 **외부**에서 호출되며, `EmailService` 메서드는 `@Async`로 비동기 처리됩니다. 트랜잭션 커밋 이후에만 이메일이 전송되므로 DB 롤백 시 잘못된 알림이 발송되지 않습니다.

---

## 🧪 테스트

JUnit 5 + Mockito 기반의 단위 테스트를 작성했습니다. 모든 테스트는 외부 의존성(DB, Redis) 없이 순수 단위 테스트로 실행됩니다.

### 테스트 파일 목록

| 테스트 클래스 | 대상 | 주요 테스트 케이스 |
|------------|------|----------------|
| `UserServiceTest` | `UserService` | 회원가입 성공/중복이메일/만14세미만, 로그인 성공/비밀번호불일치/탈퇴계정/미인증, 비밀번호변경, 포인트차감 |
| `ReserveServiceTest` | `ReserveService` | 비회원 예약 생성/객실없음, 비회원 조회 성공/실패, 예약취소 요청/상태오류 |
| `ReviewServiceTest` | `ReviewService` | 리뷰 작성/예약없음/중복리뷰, 좋아요 추가/취소, 리뷰 삭제/타인삭제불가 |
| `JwtTokenProviderTest` | `JwtTokenProvider` | 토큰 생성, 유효/만료/위변조 검증, userId 추출, RefreshToken UUID 형식 |
| `PaymentDetailServiceTest` | `PaymentDetailService` | 카드결제저장, 가상계좌저장, null결제객체예외, 빈 결제정보 안전처리 |

### 총 테스트 수: 33개

```
✅ UserServiceTest         (10)
✅ ReserveServiceTest       (6)
✅ ReviewServiceTest        (7)
✅ JwtTokenProviderTest     (6)
✅ PaymentDetailServiceTest (4)
```

---

## ⚡ 성능 최적화

HoneyRest는 개발 완료 후 실제 대규모 트래픽 환경에서의 성능 최적화를 단계별로 진행했습니다.

### 🔧 N+1 쿼리 해결

인기 숙소 조회 시 `findAllById()` 후 `a.getCategory().getName()`을 반복 호출해 N+1이 발생하던 문제를 해결했습니다.

```java
// Before: findAllById() → category 접근 시 N번 추가 쿼리 발생
// After: JOIN FETCH로 한 번에 조회
@Query("SELECT a FROM Accommodation a JOIN FETCH a.category WHERE a.accommodationId IN :ids")
List<Accommodation> findAllByIdWithCategory(@Param("ids") List<Long> ids);
```

### 🗃️ 캐싱 레이어 개선

- **`BannerService.getBanners()`**: `@Cacheable("banners")` 적용 — 배너 목록 Redis 캐싱
- **`BannerService.saveBanner()`**: `@CacheEvict(value = "banners", allEntries = true)` — 배너 저장 시 캐시 자동 무효화
- **Redis ZSet 인기 숙소**: 숙소 카테고리별 조회수 ZSet 구조 유지 (`popular:accommodation:{category}`)
- **Querydsl 동적 검색**: 복합 조건 검색 쿼리 성능 개선

### 🔒 트랜잭션 경계 최적화

**외부 API 호출 분리**:
- Toss 결제 승인 API를 `TransactionTemplate` **외부**에서 호출 — 외부 API 지연이 DB 트랜잭션 시간에 영향을 주지 않음

**`@Transactional(readOnly = true)` 누락 메서드 보완**:

| 서비스 | 메서드 |
|------|-------|
| `AccommodationService` | `getPopularByCategory()`, `searchAvailable()`, `getPriceRange()`, `getDetail()` |
| `BannerService` | `getBanners()`, `getRandomBanner()` |
| `ReserveService` | `findGuestReservation()`, `getReservationDetail()` |

> readOnly 트랜잭션은 Hibernate의 flush 모드를 MANUAL로 설정해 스냅샷 비교·dirty checking을 생략하므로 조회 성능이 향상됩니다.

### 🔒 보안 감사

- **DTO 입력 검증**: 모든 주요 요청 DTO에 Bean Validation 적용으로 잘못된 입력값 조기 차단 ✅
- **민감 데이터 로깅 확인**: OAuth 토큰, 비밀번호 등 민감 정보가 로그에 남지 않도록 검증 ✅
- **전역 예외 처리 확장**: `MethodArgumentNotValidException`, `ConstraintViolationException` 핸들러 추가 ✅

---

## 🗄️ DB 안정화

### Flyway 마이그레이션 도입

`ddl-auto=update`는 운영 환경에서 의도치 않은 스키마 변경을 유발할 수 있습니다. Flyway를 도입해 스키마 변경을 버전 관리합니다.

**의존성 추가** (`build.gradle`):
```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

**Flyway 설정** (`application.properties`):
```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true   # 기존 DB와 공존 가능
spring.flyway.baseline-version=0
spring.flyway.locations=classpath:db/migration
```

**마이그레이션 파일**: `src/main/resources/db/migration/V1__baseline.sql`
- 전체 스키마 20+ 테이블 정의 (accommodation, user, reservation, payment, review 등)
- 기존 DB에 `baseline-on-migrate`로 적용 — 이미 존재하는 테이블은 건드리지 않음

### 운영 환경 권장 설정

| 환경 | `ddl-auto` 값 |
|-----|--------------|
| 개발 | `update` (현재) |
| 운영 | `validate` — Flyway가 스키마를 관리, JPA는 검증만 수행 |

### HikariCP 풀 명시 설정

[안정화 섹션](#-안정화) 참고

---

## 💭 프로젝트 회고

HoneyRest는 사용자 중심의 감성 숙소 예약 플랫폼을 목표로,  
기획부터 설계, 구현, 발표까지 전 과정을 경험한 팀 프로젝트였습니다.

사용자(User) 영역을 맡아 프론트엔드와 백엔드를 모두 개발하며  
React와 Spring Boot의 연결 구조, API 설계, 데이터 흐름에 대한 이해를 높일 수 있었고  
Redis 캐싱, JWT 인증, OAuth2 로그인 등 실무에서 활용되는 기술들을 직접 적용해보며  
백엔드 개발 역량을 한층 강화할 수 있었습니다.

팀장으로서 전체 흐름을 조율하고,  
사용자 → 업체 관리자 → 총 관리자까지 이어지는 서비스 구조를  
하나의 시스템 안에서 유기적으로 연결되도록 설계한 점은  
이번 프로젝트의 핵심 경험 중 하나였습니다.

광고 영상과 발표 자료 제작을 통해  
기술적인 완성도뿐 아니라 사용자에게 전달되는 메시지와 UI 흐름까지 고려하게 되었고,  
협업 과정에서는 코드 스타일 통일, 브랜치 전략 수립, 문서화 등  
프로젝트를 하나의 제품처럼 완성해가는 경험을 할 수 있었습니다.

이번 프로젝트를 통해 기술적인 자신감과 팀 내 커뮤니케이션 역량 모두 성장할 수 있었으며,  
향후에는 실시간 기능이나 다양한 사용자 시나리오가 포함된  
대규모 트래픽 환경에서도 안정적으로 동작하는 서비스를 직접 설계해보고자 합니다.

---

## 🙋‍♂️ 개발자 정보

**박성원 (Seongwon Park)** – 팀장 / 사용자(User) 영역 총괄

- **User API 백엔드 및 프론트엔드 전체 설계 및 개발**
- **전체 DB 설계 및 ERD 작성**
- **API 명세서 작성 및 문서화**
- **관리자 시스템 기술 방향 결정 (Thymeleaf 채택) 및 전체 시스템 통합**
- **광고 영상 및 일러스트 제작**
- **프로젝트 발표용 PPT 기획 및 디자인 총괄**

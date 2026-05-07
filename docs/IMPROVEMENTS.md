# ⚡ HoneyRest – 후속 개선 작업

프로젝트 완료 후 서비스 품질을 높이기 위해 진행한 4단계 개선 작업입니다.

---

## 🛡️ Phase 1 — 안정화

### DTO 입력 검증

컨트롤러 레이어에서 잘못된 요청을 조기에 차단합니다.

| DTO | 주요 검증 규칙 |
|-----|--------------|
| `UserSignupRequestDTO` | `@Email`, `@NotBlank`, `@Size(min=8)` (비밀번호), `@Pattern` (전화번호, 생년월일) |
| `UserLoginRequestDTO` | `@NotBlank`, `@Email` |
| `ReservationRequestDTO` | `@NotNull` (roomId, checkIn, checkOut), `@Min(1)` (guests), `@NotBlank` (guestName, guestPhone) |
| `ReviewRequestDTO` | `@DecimalMin("1.0")` ~ `@DecimalMax("5.0")` (평점), `@Size(min=10, max=2000)` (내용) |
| `TossConfirmRequest` | `@NotBlank` (paymentKey, orderId), `@Positive` (amount) |
| `GuestReservationLookupRequestDTO` | `@NotBlank` (reservationCode, guestPhone, guestPassword) |

각 컨트롤러 메서드에 `@Valid` 추가: `AuthController`, `ReviewController`, `PaymentController`, `ReserveController`

### GlobalExceptionHandler 보완

| 예외 타입 | HTTP 상태 | 처리 방식 |
|---------|----------|---------|
| `MethodArgumentNotValidException` | 400 | 필드별 에러 메시지 Map 반환 |
| `ConstraintViolationException` | 400 | violation 목록 문자열 반환 |
| `ApiException` | 커스텀 | 기존 유지 |
| `IllegalArgumentException` | 400 | 기존 유지 |
| `IllegalStateException` | 410 | 기존 유지 |
| `Exception` | 500 | 폴백 |

### HikariCP 커넥션 풀 명시 설정

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### 이메일 알림 트랜잭션 분리

Toss 결제 승인 후 이메일 전송은 `TransactionTemplate.execute()` 블록 **외부**에서 호출되며,
`@Async`로 비동기 처리됩니다. DB 롤백 시 잘못된 이메일이 발송되지 않습니다.

---

## 🧪 Phase 2 — 테스트

JUnit 5 + Mockito 기반 단위 테스트. 외부 의존성(DB, Redis) 없이 순수 단위 테스트로 실행됩니다.

| 테스트 클래스 | 대상 | 주요 테스트 케이스 |
|------------|------|----------------|
| `UserServiceTest` | `UserService` | 회원가입 성공/중복이메일/만14세미만, 로그인 성공/비밀번호불일치/탈퇴계정/미인증, 비밀번호변경, 포인트차감 |
| `ReserveServiceTest` | `ReserveService` | 비회원 예약 생성/객실없음, 비회원 조회 성공/실패, 예약취소 요청/상태오류 |
| `ReviewServiceTest` | `ReviewService` | 리뷰 작성/예약없음/중복리뷰, 좋아요 추가/취소, 리뷰 삭제/타인삭제불가 |
| `JwtTokenProviderTest` | `JwtTokenProvider` | 토큰 생성, 유효/만료/위변조 검증, userId 추출, RefreshToken UUID 형식 |
| `PaymentDetailServiceTest` | `PaymentDetailService` | 카드결제저장, 가상계좌저장, null결제객체예외, 빈 결제정보 안전처리 |

**총 33개 테스트**

```
✅ UserServiceTest         (10)
✅ ReserveServiceTest       (6)
✅ ReviewServiceTest        (7)
✅ JwtTokenProviderTest     (6)
✅ PaymentDetailServiceTest (4)
```

---

## ⚡ Phase 3 — 성능 최적화

### N+1 쿼리 해결

인기 숙소 조회 시 `findAllById()` 후 `a.getCategory().getName()` 반복 호출로 N+1이 발생하던 문제를 해결했습니다.

```java
// Before: category 접근 시마다 추가 쿼리 발생
// After: JOIN FETCH로 한 번에 조회
@Query("SELECT a FROM Accommodation a JOIN FETCH a.category WHERE a.accommodationId IN :ids")
List<Accommodation> findAllByIdWithCategory(@Param("ids") List<Long> ids);
```

### 캐싱 레이어 개선

| 서비스 | 적용 |
|--------|------|
| `BannerService.getBanners()` | `@Cacheable("banners")` — 배너 목록 Redis 캐싱 |
| `BannerService.saveBanner()` | `@CacheEvict(value="banners", allEntries=true)` — 저장 시 캐시 무효화 |
| Redis ZSet | `popular:accommodation:{category}` — 카테고리별 인기 숙소 조회수 관리 |

### `@Transactional(readOnly = true)` 누락 보완

| 서비스 | 메서드 |
|------|-------|
| `AccommodationService` | `getPopularByCategory()`, `searchAvailable()`, `getPriceRange()`, `getDetail()` |
| `BannerService` | `getBanners()`, `getRandomBanner()` |
| `ReserveService` | `findGuestReservation()`, `getReservationDetail()` |

> `readOnly=true`는 Hibernate flush 모드를 MANUAL로 설정해 dirty checking을 생략하므로 조회 성능이 향상됩니다.

### 보안 감사

- DTO 입력 검증: 모든 주요 요청 DTO에 Bean Validation 적용 ✅
- 민감 데이터 로깅 확인: OAuth 토큰, 비밀번호 등이 로그에 남지 않도록 검증 ✅
- 전역 예외 처리 확장: `MethodArgumentNotValidException`, `ConstraintViolationException` 핸들러 추가 ✅

---

## 🗄️ Phase 4 — DB 안정화 (Flyway)

`ddl-auto=update`는 운영 환경에서 의도치 않은 스키마 변경을 유발할 수 있습니다.
Flyway를 도입해 스키마 변경을 버전 관리합니다.

### 의존성 추가 (`build.gradle`)

```groovy
implementation 'org.flywaydb:flyway-core'
implementation 'org.flywaydb:flyway-mysql'
```

### Flyway 설정 (`application.properties`)

```properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
spring.flyway.locations=classpath:db/migration
```

### 마이그레이션 파일

`src/main/resources/db/migration/V1__baseline.sql`
- 전체 스키마 20+ 테이블 정의
- `baseline-on-migrate`로 기존 DB와 공존 가능

### 환경별 권장 설정

| 환경 | `ddl-auto` |
|-----|-----------|
| 개발 | `update` (현재) |
| 운영 | `validate` — Flyway가 스키마 관리, JPA는 검증만 수행 |

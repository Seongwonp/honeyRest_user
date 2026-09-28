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
| `IllegalStateException` | 409 | 상태 충돌 (이미 처리됨 등) |
| `AccessDeniedException` | 403 | 인가 실패 (이전에는 500으로 떨어짐) |
| `AuthenticationException` | 401 | 인증 실패 |
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

JUnit 5 + Mockito 기반 단위 테스트가 중심이며, `./gradlew test` 는 **외부 서비스(MySQL, Redis,
`application_security.properties`, Firebase) 없이** 전체가 통과합니다. GitHub Actions(`.github/workflows/ci.yml`)가
`main` 대상 push/PR 마다 `./gradlew build`(테스트 포함)를 실행합니다.

### test 프로필 구성 (`src/test/resources/application-test.properties`)

`@SpringBootTest` 기반 테스트(`HoneyRestUserApplicationTests`, `AdminWriteApiSecurityTest`)는 `@ActiveProfiles("test")`로
아래 자급자족 설정을 사용합니다.

| 항목 | 운영/개발 | test 프로필 |
|------|----------|------------|
| DB | MySQL + Flyway(`validate`) | H2 인메모리 `MODE=MySQL` (`testRuntimeOnly 'com.h2database:h2'`), Flyway 비활성, `ddl-auto=create-drop` |
| 캐시 | `spring.cache.type=redis` | `simple` (인메모리). RedisTemplate 빈은 생성되지만 Lettuce 지연 연결이라 기동 시 Redis 불필요 |
| 비밀값 | `application_security.properties` | `spring.config.import=optional:...` 로 파일 부재 허용 + 더미 값(JWT 32바이트 이상, Toss, OAuth, Gmail) |
| 파일 저장소 | local / firebase | `app.storage.type=local` 고정 → `FirebaseConfig` 비활성 |

- H2 URL 의 `NON_KEYWORDS=USER,...` 는 `user` 테이블이 H2 예약어라 DDL 이 조용히 실패하는 것을 막기 위함입니다.
- **트레이드오프**: Flyway V10 이 MySQL 전용 구문(`PREPARE`/`EXECUTE`, `information_schema` 조회)을 사용해 H2 에서
  실행할 수 없으므로, 테스트 스키마는 마이그레이션이 아닌 JPA 엔티티로 생성됩니다. 따라서
  (1) 마이그레이션 SQL 자체의 오류, (2) 엔티티 ↔ 실제 MySQL 스키마 불일치(`ddl-auto=validate` 실패),
  (3) MySQL 고유 동작(JSON 함수, 콜레이션, 락) 은 이 테스트로 검출되지 않습니다.
  후속 과제: CI(ubuntu-latest, Docker 사용 가능)에 Testcontainers MySQL 기반 통합 테스트 프로필을 추가해
  Flyway V1~V10 적용 + `validate` 를 검증.
- `ReviewRedisLikeRepositoryImplTest` 는 `RedisTemplate` 을 Mockito 로 대체한 단위 테스트라 실제 Redis 가 필요 없습니다.

| 테스트 클래스 | 대상 | 주요 테스트 케이스 |
|------------|------|----------------|
| `UserServiceTest` | `UserService` | 회원가입 성공/중복이메일/만14세미만, 로그인 성공/비밀번호불일치/탈퇴계정/미인증, 비밀번호변경, 포인트차감 |
| `ReserveServiceTest` | `ReserveService` | 비회원 예약 생성/객실없음, 비회원 조회 성공/실패, 예약취소 요청/상태오류 |
| `ReviewServiceTest` | `ReviewService` | 리뷰 작성/예약없음/중복리뷰, 좋아요 추가/취소, 리뷰 삭제/타인삭제불가 |
| `JwtTokenProviderTest` | `JwtTokenProvider` | 토큰 생성, 유효/만료/위변조 검증, userId 추출, RefreshToken UUID 형식, 토큰 폐기(로그아웃 전 토큰 거부·같은 초 재로그인 허용·비밀번호 변경 무효화·초 단위 비교) |
| `PaymentDetailServiceTest` | `PaymentDetailService` | 카드결제저장, 가상계좌저장, null결제객체예외, 빈 결제정보 안전처리 |

**총 85개 테스트 (15개 클래스, 전체 통과)** — 아래 표는 주요 클래스만 발췌

```
✅ UserServiceTest                   (13)
✅ JwtTokenProviderTest              (15)
✅ ReviewServiceTest                  (9)
✅ ReserveServiceTest                 (8)
✅ PaymentOrchestrationServiceTest    (7)
✅ PriceCalculatorTest                (5)
✅ PaymentDetailServiceTest           (4)
✅ PasswordResetServiceTest           (5)
✅ FileControllerTest                 (4)
✅ LocalFileStorageTest               (4)
✅ TossServiceTest                    (3)
✅ AdminWriteApiSecurityTest          (3)  ← @SpringBootTest (H2)
✅ AccommodationSearchCacheKeyTest    (2)
✅ ReviewRedisLikeRepositoryImplTest  (2)
✅ HoneyRestUserApplicationTests      (1)  ← @SpringBootTest (H2)
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

### 토큰 폐기 시각 비교 정밀도 (`tokenValidAfter` vs JWT `iat`)

- **문제**: `iat`는 초 단위, `tokenValidAfter`는 나노초까지 저장돼 로그아웃 직후 같은 초에 재로그인한 토큰이
  거부될 수 있었다(비교 방식에 따라서는 반대로 로그아웃 직전 같은 초에 발급된 토큰이 살아남을 수 있음).
- **규칙**
  | 단계 | 동작 |
  |------|------|
  | 폐기(로그아웃·비밀번호 변경) | `tokenValidAfter = now.truncatedTo(SECONDS) + 1초` (`User.revokeExistingTokens`) |
  | 발급(로그인·소셜·OAuth2·재발급) | `iat = max(now, tokenValidAfter)`, 만료도 iat 기준 (`JwtTokenProvider.createAccessToken(User)`) |
  | 검증 | epoch 초 단위로 `iat < tokenValidAfter` 이면 거부 (`JwtTokenProvider.isIssuedBefore`) |
- **결과**: 폐기 이전(같은 초 포함) 토큰은 항상 거부, 폐기 직후 같은 초의 재로그인 토큰은 항상 허용.
  대가로 폐기 직후 1초 안에 발급된 토큰은 `iat`가 최대 1초 미래로 잡히고 만료도 그만큼 늘어난다(허용 오차 ≤ 1초).
- **주의**: `tokenValidAfter`가 설정될 수 있는 사용자에게는 `createAccessToken(userId, role)`이 아닌
  `createAccessToken(User)`로 발급해야 한다. 서버 간 시계 차이는 별도로 보정하지 않는다.

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

---

## 🏨 예약 재고·결제 보상·가격 계산

### 1. 이중 예약 방지

- **재고 점유 상태** (`ReservationStatus.OCCUPYING`): `PENDING`, `CONFIRMED`, `CANCEL_REQUEST`, `COMPLETED`, `NO_SHOW`
  — 호스트 저장소의 유효 상태 목록과 동일. `CANCELLED`/`CANCELED`/`REFUNDED` 는 재고를 점유하지 않는다.
  `CANCEL_REQUEST` 는 호스트 승인 전까지 객실을 계속 점유한다.
- **겹침 기준**: `check_in < 요청 체크아웃 AND check_out > 요청 체크인` (체크아웃 당일은 비점유). 예약 1건 = 객실 1개.
- **판정**: 겹치는 점유 예약 수 ≥ `room.total_rooms` 이면 `ApiException(409)`.
- **락**: `ReserveService.createReservation` 이 `RoomRepository.findByIdForUpdate`(`PESSIMISTIC_WRITE`, `SELECT ... FOR UPDATE`)로
  객실 행을 잠근 뒤 재확인한다. 같은 객실의 동시 예약 트랜잭션은 직렬화되고, 락은 결제 저장 트랜잭션 커밋 시 해제된다.
- 객실/숙소 상세의 "예약 가능" 표시 쿼리도 같은 상태 목록을 쓰도록 맞췄다.
- `reservation.version` 컬럼은 Flyway 마이그레이션에 없으므로 사용자 쪽 엔티티에 `@Version` 을 추가하지 않았다.

### 2. 결제 보상 흐름 (`PaymentOrchestrationService.confirmAndSave`)

```
1) 사전 검증 (과금 전)   중복 paymentKey/예약번호, 주문번호·금액, 서버 금액 재계산, 재고(락 없음) → 실패 시 그대로 거절
2) 토스 승인            TossService.confirmPayment (실제 과금). 비 2xx 응답은 토스 에러 메시지로 ApiException
3) 저장 트랜잭션         객실 행 락 + 재고 재확인 → 예약/결제/결제상세 저장
4) 3) 실패 시 보상      TossService.cancelPayment(paymentKey, 사유) → "결제는 자동으로 취소되었습니다" 안내
                        (매진이면 409, 그 외 500)
```

- 같은 `paymentKey` 가 동시 요청으로 이미 저장되어 있으면 **취소하지 않는다** (정상 예약의 결제를 되돌리지 않기 위함).
- 취소 자체가 실패하면 `[결제 보상 실패] 수동 환불 필요` 로 `orderId`/`paymentKey` 를 ERROR 로그에 남기고
  고객센터 안내 500 을 반환한다 (개인정보는 로그에 남기지 않음). 실패 건 테이블 적재는 후속 과제.
- `TossService` 는 `HttpURLConnection` 대신 공용 `RestTemplate` 빈과 스프링 `ObjectMapper` 빈을 사용한다.

### 3. 단일 가격 계산기 (`PriceCalculator`)

| 항목 | 규칙 |
|------|------|
| 1박 요금 | `price_calendar`(room_id, date) 행의 `price` → 없으면 `room.price` |
| 과금 일자 | 체크인일 ~ 체크아웃 전날 |
| 추가 인원 | `extra_person_fee × max(0, 인원 − standard_occupancy) × 박수` (fee 가 null 이면 0) |
| 결과 | `PriceBreakdownDTO` (박수, 일자별 요금, 객실 소계, 추가요금, 합계) |

예약 폼(`ReserveInfoService`)과 결제 검증(`PaymentOrchestrationService`)이 모두 이 계산기를 사용하므로
화면 표시 금액과 결제 검증 금액이 항상 같다. `price_calendar.available_room` 은 재고 판정에 사용하지 않는다
(호스트 화면이 `total_rooms − 예약 수` 를 저장하는 스냅샷 값이라 중복 차감 위험).


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
  → (1)·(2) 는 `integrationTest` 태스크(Testcontainers MySQL 8.0, `schema/FlywayMigrationMySqlIntegrationTest`)가
  Flyway V1~최신 적용 + `validate` 로 검증하며 CI 에서 실행됩니다. 관리자 앱 엔티티와의 교차 검증은
  honeyRest_host 의 `integrationTest` 가 담당합니다. (3) 은 여전히 범위 밖입니다.
- `ReviewRedisLikeRepositoryImplTest` 는 `RedisTemplate` 을 Mockito 로 대체한 단위 테스트라 실제 Redis 가 필요 없습니다.

| 테스트 클래스 | 대상 | 주요 테스트 케이스 |
|------------|------|----------------|
| `UserServiceTest` | `UserService` | 회원가입 성공/중복이메일/만14세미만, 로그인 성공/비밀번호불일치/탈퇴계정/미인증, 비밀번호변경, 포인트차감 |
| `ReserveServiceTest` | `ReserveService` | 비회원 예약 생성/객실없음, 비회원 조회 성공/실패, 예약취소 요청/상태오류 |
| `ReviewServiceTest` | `ReviewService` | 리뷰 작성/예약없음/중복리뷰, 좋아요 추가/취소, 리뷰 삭제/타인삭제불가 |
| `JwtTokenProviderTest` | `JwtTokenProvider` | 토큰 생성, 유효/만료/위변조 검증, userId 추출, RefreshToken UUID 형식, 토큰 폐기(로그아웃 전 토큰 거부·같은 초 재로그인 허용·비밀번호 변경 무효화·초 단위 비교) |
| `PaymentDetailServiceTest` | `PaymentDetailService` | 카드결제저장, 가상계좌저장, null결제객체예외, 빈 결제정보 안전처리 |

**총 118개 테스트 (21개 클래스, 전체 통과)** — `./gradlew test` 기준 (Testcontainers `integrationTest` 제외)

```
✅ JwtTokenProviderTest              (15)
✅ UserServiceTest                   (13)
✅ ReviewServiceTest                 (12)
✅ ReserveServiceTest                 (8)
✅ PasswordResetServiceTest           (8)
✅ PaymentOrchestrationServiceTest    (7)
✅ TossServiceTest                    (6)
✅ PriceCalculatorTest                (5)
✅ EmailVerificationTokenServiceTest  (5)
✅ FileValidatorTest                  (5)
✅ LocalFileStorageTest               (5)
✅ ReviewRedisLikeRepositoryImplTest  (5)
✅ PaymentDetailServiceTest           (4)
✅ FileControllerTest                 (4)
✅ AccommodationSearchCacheKeyTest    (3)
✅ SearchCacheVersionServiceTest      (3)
✅ UnauthenticatedPrincipalTest       (3)
✅ AdminWriteApiSecurityTest          (3)  ← @SpringBootTest (H2)
✅ EmailRateLimiterTest               (2)
✅ RatingCacheServiceTest             (1)
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

---

## 🔒 P1 — 권한·무결성·남용 방지

### 1. 리뷰 권한/무결성 (`ReviewService`, `ReviewController`)
- 작성: 예약 소유자 + `ReservationStatus.COMPLETED` 만 허용. 예약 행을 `findByIdForUpdate`(FOR UPDATE)로 잠가
  동시 작성에서도 "예약당 리뷰 1건·포인트 1회"를 보장한다 (DB 유니크 제약/마이그레이션은 추가하지 않음).
- 수정: 빌더로 새 `Review` 를 만들어 `save`(merge) 하던 방식이 `likeCount`·`reply` 를 null 로 덮어쓰고
  `createdAt` 을 잃었다 → 관리 엔티티의 `updateContent(...)` 로 평점·본문만 변경.
- 좋아요: `review:like:users:{reviewId}` Redis Set 에 userId 를 넣고/빼서 **실제로 상태가 바뀐 경우에만** 카운터를 움직인다
  (사용자별 멱등). 카운터 캐시가 없으면 DB `like_count` 로 먼저 초기화한다. 엔드포인트는 로그인 사용자 기준.
  - 한계: Set 도입 이전에 누른 좋아요는 Set 에 없으므로 그 사용자의 취소 요청은 카운터를 줄이지 않는다.
    `review_like` 테이블이 없어 Redis 가 유실되면 사용자별 상태도 유실된다(영속화는 후속 과제).

### 2. 파일 업로드 검증 (`storage/FileValidator`)
- Local/Firebase 저장소가 모두 업로드 전에 호출: 폴더명(영문/숫자/-/_), 크기(`app.storage.max-file-size`, 기본 5MB → 413),
  확장자(jpg/jpeg/png/gif/webp), **매직 바이트 스니핑**(확장자와 실제 형식 일치).
- Firebase blob 이름에서 원본 파일명을 제거(UUID + 검증된 확장자), Content-Type 도 확장자로 결정.
- multipart 한도 초과(`MaxUploadSizeExceededException`)는 500 대신 413.
- 삭제 권한(폴더 화이트리스트 + DB 소유권 확인)은 기존 `FileController` 로직 유지.

### 3. 캐시 무효화 정합 — 표는 `docs/ARCHITECTURE.md` "캐시 키 & 무효화" 참고
- Redis `KEYS` 제거: 숙소 캐시 무효화는 `AccommodationCacheKeys.allFor(id)` 명시 목록 DEL, 인기 숙소 키 수집은 `SCAN`.
- 기존 패턴(`accommodation:*:{id}`)이 놓치던 `reviewCount/reviewList/cancellationPolicy` 키도 이제 삭제된다.
- `accommodation:tags:{id}` 를 상세 조회와 `AccommodationTagMapService` 가 서로 다른 DTO 로 공유하던 충돌 → 태그맵은 `accommodation:tagmap:{id}`.
- 검색 결과 캐시는 키에 세대 번호(`search:recommend:version`)를 넣고, 예약 생성·리뷰 변경·태그 매핑 변경 시 **커밋 후** INCR.
- 검색의 예약 수 집계가 취소 예약까지 세던 문제 → `ReservationStatus.OCCUPYING` 만 집계.

### 4. 예외 매핑
- `PaymentController`, `EmailController` 의 미인증 `AccessDeniedException`(403) → `ApiException(401)`.
  `ReviewController` 도 principal 이 없으면 401. `UserController` 는 `/api/user/**` 가 인증 필수라 컨트롤러 전에 401 (아래 SOLID 리팩터링 5 참고).

### 5. 이메일 남용 방지 (`service/email/EmailRateLimiter`)
- 가입 인증/재전송, 비밀번호 재설정, 이메일 변경 메일: 용도·이메일별 10분 3회(`app.email.rate-limit.*`) 초과 시 429.
  Redis INCR+EXPIRE, Redis 불가 시 인스턴스 메모리 카운터로 대체. 비밀번호 재설정은 사용자 조회 전에 검사(계정 노출 방지).
- 토큰 1회용: `consumeByTokenValue`(원자적 DELETE, 영향 행 0 이면 거부) — 동시 요청 중 한쪽만 성공.
- 새 토큰 발급 시 같은 용도의 이전 토큰 무효화, 가입 인증에 EMAIL_CHANGE 토큰 사용 금지. 만료는 기존대로(인증 24h, 재설정 30분).

### 6. Toss 승인 타임아웃 보상 (`TossService.reconcileTimedOutConfirm`)
- 승인 호출이 타임아웃(504 경로)이면 `GET /v1/payments/orders/{orderId}` 로 상태 조회:
  `DONE` → 즉시 전액 취소 후 `PaymentConfirmTimeoutException(reversed=true, 504)`,
  404/ABORTED 등 → `reversed=false` 실패 안내. 조회·취소 실패는 `[결제 상태 확인 실패]`/`[결제 보상 실패]` ERROR 로그로 수동 확인.
- 토스 5xx 응답(502 경로)은 여기서 재조회하지 않는다 — 필요 시 같은 보상을 확장.

## 🧱 SOLID 리팩터링

동작은 그대로 두고 책임·의존 방향을 정리했다. 의도된 동작 변화는 **[변경]** 으로 표시.

### 1. 메일 발송 포트 분리 (LSP) — `service/email/EmailService`
- 문제: `NoOpEmailService`·`E2eNoOpEmailService` 가 SMTP 구현 클래스를 상속하고 `super(null)` 로 `JavaMailSender` 를 비워 뒀다.
  메서드 하나만 재정의를 빠뜨려도 null 로 발송을 시도하는 구조라 하위 타입이 상위 타입을 안전하게 대체하지 못했다.
- 변경: `EmailService` 를 4개 발송 메서드 인터페이스(포트)로 바꾸고 구현 3개가 각각 구현한다.
  `SmtpEmailService`(`!e2e` + `MailEnabledCondition`), `NoOpEmailService`(`!e2e` + `MailEnabledCondition.Disabled`), `E2eNoOpEmailService`(`e2e`).
  프로필/조건마다 빈은 정확히 하나. 호출자(결제·비밀번호 재설정·이메일 인증)는 인터페이스에만 의존한다.
- 쓰이지 않던 중복 `util/EmailSender`(같은 MIME 발송 코드) 삭제.
- 참고: SMTP 구현은 `@Async` 때문에 인터페이스 기반 프록시로 감싸진다. 구현 클래스 타입으로 주입받지 말 것.
- 테스트: `EmailServiceWiringTest`(조건/프로필별 단일 빈, no-op 은 JavaMailSender 미사용), `E2eProfileIsolationTest`.

### 2. 포인트 잔액 + 이력 원자화 (SRP) — `service/PointService`
- 문제: 잔액 변경(`UserService.usePoint/addPoint`)과 이력 기록(`PointHistoryService.recordUsage/addHistory`)을 호출자가 따로 불러
  한쪽 누락·순서 차이로 잔액과 `point_history.balance` 가 어긋날 수 있었다(예약 `ReserveService`, 리뷰 적립 `ReviewService`).
- 변경: `PointService.earn/use/refund(user, amount, reason, relatedId)` 가 같은 트랜잭션에서 잔액 변경 + 이력 저장(변동 후 잔액)을 함께 한다.
  변동량은 0 초과, 결과 잔액은 0 이상만 허용(부족하면 잔액·이력 모두 그대로). 적립 유효기간 1년은 서비스 상수.
  `UserService` 의 포인트 메서드, `PointHistoryService` 의 쓰기 메서드, `PointHistoryRequestDTO` 삭제 — `PointHistoryService` 는 조회 전용.
- 결제 사전 검증(`PaymentOrchestrationService.normalizeUsedPoint`)은 잔액만 읽으므로 그대로.
- 테스트: `PointServiceTest`, `ReserveServiceTest`, `ReviewServiceTest`.

### 3. 로그인 서비스의 웹 결합 제거 (SRP/DIP) — `UserService.login/socialLogin`
- 문제: 서비스가 `HttpServletResponse` 를 받아 refresh token 쿠키를 직접 썼고, 토큰 발급·응답 DTO 조립이 두 메서드에 중복됐다.
- 변경: 서비스는 `LoginResult(response, refreshToken)` 만 돌려주고, `AuthController` 가 `RefreshTokenCookieManager` 로 쿠키를 만든다
  (`changePassword` 의 쿠키 삭제와 같은 방식). 토큰 발급은 `issueTokens`, DTO 변환은 `toUserResponse` 로 합쳤다.
  `LoginResult.toString()` 은 토큰을 출력하지 않는다.

### 4. 예약 상태 캡슐화 — `honeyrest-domain` `Reservation`, `ReservationStatus`
- 문제: `status`·`cancelReason` 에 public setter 가 있어 어떤 상태로든 바꿀 수 있었고, 전이 규칙이 서비스마다 흩어져 있었다.
- 변경: setter 제거. 의도를 드러내는 도메인 메서드로만 바꾼다.
  - `requestCancel(reason)`: CONFIRMED → CANCEL_REQUEST (사용자 취소 요청, `ReserveService`)
  - `complete()`: CONFIRMED → COMPLETED (e2e 이용 완료 보조 API, 호스트 체크아웃 완료)
  - `cancel(reason)`: 취소 외 모든 상태 → CANCELLED (기존 동작 유지, 이제 전이표로 검증)
  - 호스트 이전용: `approveCancelRequest(reason)`(CANCEL_REQUEST→CANCELLED), `rejectCancelRequest(reason)`(CANCEL_REQUEST→CONFIRMED, 사유 `[REJECT] ` 접두어),
    `markNoShow()`(PENDING/CONFIRMED→NO_SHOW) — 호스트 JPQL 일괄 UPDATE 와 같은 출발 상태 조건
  - `changeStatus(to)`: 범용 전이(호스트 관리자 화면용). 정규화 후 전이표 검증, 같은 상태면 no-op.
- `ReservationStatus.canTransition(from, to)` / `allowedTransitions(from)` 전이표 — 두 앱의 현재 사용처에서 도출:
  PENDING→{CONFIRMED, NO_SHOW, CANCELLED}, CONFIRMED→{CANCEL_REQUEST, COMPLETED, NO_SHOW, CANCELLED},
  CANCEL_REQUEST→{CANCELLED, CONFIRMED}, COMPLETED→{CANCELLED}, NO_SHOW→{CANCELLED}, CANCELLED→{CONFIRMED}(관리자 복구).
- **[변경]** `cancel()` 은 전이표에 없는 상태(예: 오타 `CANCELED`, 모르는 값)에서 이제 IllegalStateException.
- 호스트는 엔티티 setter 를 쓰지 않아 그대로 컴파일된다. 다만 JPQL 일괄 UPDATE(취소 승인/거부, 완료, 노쇼)와
  `update(ReservationChanges)`·빌더 재저장 경로는 전이표를 거치지 않으므로 호스트에서 도메인 메서드로 옮기는 것을 권장한다.
- 테스트: 도메인 모듈 `ReservationStatusTest`(전이표), `ReservationTest`(도메인 메서드, setter 부재).

### 5. 그 밖의 정리
- 쿠폰 할인 계산을 `Coupon.calculateDiscount(amount)` 로 이동(`PaymentOrchestrationService` 는 사용 가능 여부만 검사).
  유형은 `CouponDiscountType` 이 별칭으로 해석: 정률 RATE/PERCENT/PERCENTAGE, 정액 FIXED/AMOUNT.
  **[변경]** 모르는 유형은 정액으로 조용히 처리하지 않고 IllegalStateException(결제 승인 전이라 과금 없음).
  **[변경]** 호스트 쿠폰 등록 화면이 저장하는 `PERCENTAGE` 가 이제 정률로 계산된다(과거에는 정액으로 계산되던 버그).
- `UserController` 의 `if (principal == null) return 401` 8곳 제거: `/api/user/**` 는 `SecurityConfig` 에서 authenticated 이고
  (permitAll 인 `/api/user/email/**` 는 `EmailController` 소관) 익명 요청은 컨트롤러 전에 401 이 된다. 근거 테스트 `UserApiAuthenticationTest`.
- `ReviewService`: 쓰지 않던 `RedisTemplate` 필드와 중복 주입된 `ReviewImageRepository`(두 필드) 정리, 포인트는 `PointService` 사용.
- **[변경]** "찾을 수 없음" → 404: `NotFoundException`(`ApiException` 하위) 도입. 사용자·예약·결제·리뷰·문의·이벤트·찜 대상·예약 폼 객실 조회에 적용
  (과거 IllegalArgumentException→400, RuntimeException→500). `deleteAccount` 의 RuntimeException(500)도 404.
  로그인 실패(이메일/비밀번호 불일치)와 결제 요청 본문의 객실·쿠폰 검증은 기존 400 유지.

---

## 🐞 버그 수정 (2026-10)

| # | 버그 | 원인 | 수정 |
|---|------|------|------|
| 1 | 쿠폰 적용 결제가 실패하거나 다른 쿠폰이 적용됨 | 예약 폼 쿠폰 목록은 쿠폰 마스터 id(`couponId`)를 내려주고, 결제는 그 값을 `user_coupon` id 로 조회 | 목록 DTO 에 `userCouponId` 추가(`couponId` 는 표시용 유지), 결제/예약 요청 필드를 `reservationInfo.userCouponId` 로 변경(구 `couponId` 는 받지 않음 → 금액 불일치 400, 과금 없음). e2e 시드 `user_coupon_id=7 ≠ coupon_id=1` |
| 2 | 리뷰 삭제 후 재작성 시 포인트 무한 적립 | 적립 판정이 "리뷰 존재 여부"뿐 (리뷰는 하드 삭제) | `PointService.earnOnce`: 같은 사유+예약 id 의 SAVE 이력이 있으면 적립 0. 삭제 시 회수는 하지 않음 |
| 3 | `total_rooms` 초과 예약 가능 (MySQL) | REPEATABLE READ 에서 객실 락 전에 일반 SELECT 가 실행돼 스냅샷 고정 → 락 후 COUNT 가 방금 커밋된 예약을 못 봄 | 저장 트랜잭션 첫 문장에서 객실 락, 겹치는 예약은 잠금 읽기(`FOR UPDATE`)로 집계, 격리 수준 READ COMMITTED 로 명시 |
| 4 | 같은 쿠폰/포인트 동시 이중 사용 | `UserCoupon.use()` 덮어쓰기, 포인트 read-modify-write | 조건부 UPDATE (`status='ISSUED'`, `point >= :amt`), 0행이면 409. 이력은 `PointService` 가 변동 후 DB 잔액으로 기록 |
| 5 | 남의 문의 수정·삭제 가능 (IDOR) | 컨트롤러가 `(userId, inquiryId)` 순서로 `(inquiryId, userId)` 서비스 호출 | 인자 순서 수정, 소유권 실패 403(과거 500)·없음 404 |
| 6 | 문의 수정 시 답변·카테고리 삭제 | 빌더로 새 엔티티를 만들어 save | `Inquiry.editQuestion` 으로 관리 엔티티 변경, 답변 달린 문의 수정은 409 (프론트 `!inquiry.reply` 규칙과 동일) |
| 7 | 숙소 상세 캐시가 갱신되지 않음, 이미지 중복 | 상세·이미지·태그·취소 정책 캐시 TTL 없음, 이미지 MISS 시 개별 RPUSH | TTL 10분(`app.cache.accommodation-detail-ttl`), 이미지는 MULTI/EXEC 로 통째 교체 (키·자료형 유지 → 호스트 `allFor` 무효화 그대로) |
| 8 | 정률 쿠폰 금액이 프론트와 1원 단위로 어긋남 | 서버 소수 둘째 자리 HALF_UP, 프론트 원 단위 버림 | `Coupon.calculateDiscount` 원 단위 버림(scale 0, DOWN). 33,333 × 10% = 3,333 |
| 9 | 결제 시 쿠폰 적용 대상·사용 이력 미검증 | 목록 쿼리에서만 확인 | `CouponService.calculateDiscountForPayment`: 소유·ISSUED·만료·활성·기간·최소 금액(NULL=없음)·대상·`coupon_usage` 확인 |
| 10 | 검색에 비활성 숙소/객실 노출, 예약 가능 여부 오표시 | 상태 필터 없음, 가용성 판단에 모든 객실 사용 | 숙소·객실 `ACTIVE`(대소문자 무시) 필터, 가용성은 운영 중 + `maxOccupancy >= guests` 객실만 |
| 11 | 지난 날짜 예약 가능 | 서버 날짜 검증 없음 | `StayDatePolicy`: 체크인 ≥ 오늘(Asia/Seoul), 체크아웃 > 체크인, 최대 30박 → 400 (결제 승인 전·예약 폼) |
| 12 | 찜 토글이 검색 결과에 반영되지 않음 | 검색 캐시 세대 미증가 | `searchCacheVersionService.bumpAfterCommit()` |
| 13 | 최소 주문 금액 NULL 쿠폰이 목록에서 빠짐 | `min_order_amount <= :price` 가 NULL 에서 거짓 | `isNull().or(loe)` (+ 사용자 쿠폰 만료 시각 조건 추가) |
| 14 | 마지막 리뷰 삭제 후 평점 유지 | 리뷰가 없으면 재계산 생략 | 평점 0.0 으로 초기화 (리뷰 수는 컬럼이 아니라 집계, 캐시는 함께 삭제) |
| 15 | 상세의 업체 주소가 숙소 주소 / 예약 폼 로그에 실명·전화번호 | 잘못된 필드 매핑 / DTO 통째 로깅 | `getCompanyAddress()` 사용 / id·숙박일·쿠폰 수만 로깅 |
| 16 | 날짜 없이 `/api/accommodations/{id}` 호출 시 500 | 재고 쿼리에 NULL 날짜 | 오늘~내일(Asia/Seoul) 기본값, 역순 400, 없는 숙소 404 |

- 테스트: `CouponFlowH2Test`, `ReviewPointAndRatingH2Test`, `InquiryOwnershipApiTest`, `AccommodationSearchFilterH2Test`,
  `AccommodationDetailCacheH2Test`, `PointAtomicUpdateH2Test`, `ReservationConcurrencyH2Test`, `StayDatePolicyTest`, `WishListServiceTest`,
  `CouponTest`(도메인), 기존 `PointServiceTest`·`ReserveServiceTest`·`PaymentOrchestrationServiceTest`·`ReviewServiceTest` 보강.
  MySQL 전용(InnoDB 잠금): `ReservationConcurrencyMySqlIntegrationTest` (`./gradlew integrationTest`, Docker 필요).
- 공통 테스트 지원: `support/H2IntegrationTestSupport`(H2 + 인메모리 Redis), `TestFixtures`, `ConcurrencyResult`.
- **[프론트 계약 변경]** 예약 폼 `availableCoupons[]` 에 `userCouponId` 추가, 결제 요청 `reservationInfo.couponId` → `reservationInfo.userCouponId`.

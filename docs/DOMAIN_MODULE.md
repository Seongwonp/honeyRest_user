# 공유 도메인 모듈 (`honeyrest-domain`)

사용자 API(이 저장소)와 호스트 관리자 앱(honeyRest_host)은 하나의 MySQL 스키마를 공유하면서도 JPA 엔티티를 각자 복사해 써 왔다.
사본은 시간이 지나며 조금씩 달라졌다(필드명, 정밀도, 도메인 메서드, 인덱스 선언). 이 문서는 두 사본을 하나의 모듈로 합치면서
**엔티티마다 무엇이 달랐고 어떻게 정했는지**를 기록한다.

- **기준(truth)**: Flyway `V1~V11`(`src/main/resources/db/migration`) + [DB_SCHEMA.md](../DB_SCHEMA.md). 컬럼은 바꾸지 않았다(새 마이그레이션 없음).
- **원칙**
  1. 스키마에 있는 컬럼이면 어느 한쪽에만 매핑돼 있어도 공유 엔티티에 넣는다.
  2. 한쪽에만 있는 도메인 메서드는 모두 공유 클래스에 남긴다(삭제하지 않음).
  3. 이름이 다르면 **컬럼명과 맞는 이름**을 고르고 두 앱의 사용처를 고친다.
  4. 한 앱만 쓰고 아무 엔티티도 참조하지 않는 엔티티는 그 앱에 남긴다(`ErrorLog` → 호스트).

## 1. 구성

```text
honeyRest_user/                               (루트 프로젝트 = 사용자 API. 기존 위치 그대로)
├── settings.gradle                           include 'honeyrest-domain'
├── build.gradle                              implementation project(':honeyrest-domain')
├── honeyrest-domain/
│   ├── build.gradle                          java-library, group com.honeyrest
│   └── src/main/java/com/honeyrest/domain/
│       ├── entity/   엔티티 30개 + BaseEntity + ReservationChanges(인터페이스)
│       └── type/     ReservationStatus, BannerPosition
└── src/main/resources/db/migration/          Flyway V1~V11 (API 모듈에 남음)

honeyRest_host/
├── .gitmodules                               libs/honeyrest-user → Seongwonp/honeyRest_user (branch main)
├── settings.gradle                           includeBuild('libs/honeyrest-user') + substitute com.honeyrest:honeyrest-domain
├── build.gradle                              implementation 'com.honeyrest:honeyrest-domain'
└── src/main/java/com/honeyrest/honeyrest_host/entity/ErrorLog.java   (호스트 전용 엔티티만 남음)
```

- **레이아웃 선택**: 사용자 API를 `honeyrest-api/` 로 옮기지 않고 루트에 둔 채 `honeyrest-domain/` 서브프로젝트만 추가했다.
  소스·리소스·CI·README 링크·IDE 설정 경로가 모두 그대로라 변경이 가장 작고, 호스트는 composite build 의 `project(':honeyrest-domain')` 치환으로
  이 모듈만 깔끔하게 가져갈 수 있다(루트 앱 프로젝트는 구성만 되고 빌드되지 않는다).
- **의존성**: `jakarta.persistence-api`(api), Lombok, `hibernate-core`(compileOnly — `@CreationTimestamp`/`@UpdateTimestamp`),
  `querydsl-apt`/`querydsl-core`(compileOnly — 사용자 API 의 QueryDSL 리포지토리가 쓰는 `QReservation` 등 Q 타입을 엔티티와 같은 패키지에 생성).
  Spring 의존성, 서비스/리포지토리는 없다.
- **스캔**: 두 앱 모두 `@EntityScan` 을 명시한다. 사용자 API `@EntityScan("com.honeyrest.domain")`,
  호스트 `@EntityScan({"com.honeyrest.domain", "com.honeyrest.honeyrest_host.entity"})`(ErrorLog 포함).
- **테스트**: `honeyrest-domain/src/test` 에 DB·Spring 없는 단위 테스트 6개(ReservationStatus, Reservation 도메인 메서드).

## 2. 엔티티별 통합 결과

"동일"은 두 사본의 매핑·메서드가 같아 패키지만 옮긴 경우다. 인덱스(`@Index`)는 사용자 사본에만 선언돼 있었고 공유 엔티티에 그대로 두었다
(운영은 Flyway 가 스키마를 만들고 `validate` 는 인덱스를 검사하지 않으므로, H2 `create-drop` 테스트 스키마에만 영향).

| # | 엔티티 (테이블) | 사용자 사본 | 호스트 사본 | 공유 엔티티 결정 |
|---|---|---|---|---|
| 1 | `Accommodation` (accommodation) | `updateRating()` 보유 | 메서드 없음 | 매핑 동일. `updateRating()` 유지. 미사용 `java.sql.Time` import 제거 |
| 2 | `AccommodationCategory` | — | — | 동일 |
| 3 | `AccommodationImage` | — | — | 동일 |
| 4 | `AccommodationRequestMap` | 필드 `RequestMapId`(대문자) | 동일 | **필드명 `requestMapId`** 로 정정. getter(`getRequestMapId`)는 그대로, 빌더 메서드만 `.requestMapId()` (사용처 없음) |
| 5 | `AccommodationTag` | `iconName` | `icon` (둘 다 컬럼 `icon_name`) | **`iconName`** 채택(컬럼명과 일치). 호스트 코드·템플릿에 `icon` 사용처 없음 |
| 6 | `AccommodationTagMap` | — | — | 동일 |
| 7 | `Banner` | `entity.BannerPosition` | `entity.enums.BannerPosition` | 매핑 동일. `BannerPosition` 은 Banner 가 참조하므로 `com.honeyrest.domain.type` 으로 이동 |
| 8 | `BaseEntity` (MappedSuperclass) | Spring `AuditingEntityListener` (`@CreatedDate`/`@LastModifiedDate`), 컬럼명 암묵 | 같은 리스너 + `@PrePersist/@PreUpdate` 폴백, 컬럼명 명시 | **Hibernate `@CreationTimestamp`/`@UpdateTimestamp`** + 컬럼명 명시(`created_at`, `updated_at`). 공유 모듈을 Spring 무의존으로 두기 위함. INSERT 시 둘 다, UPDATE 시 updated_at 갱신으로 결과 동일 |
| 9 | `CancellationPolicy` | `detail` = `TEXT` | `detail` = `JSON` | **`TEXT`** (V6 스키마 기준) |
| 10 | `Company` | — | — | 동일 |
| 11 | `Coupon` | `maxOrderAmount` | `maxDiscountAmount` (컬럼 `max_discount_amount`) | **`maxDiscountAmount`** 채택. 사용자 `PaymentOrchestrationService`, `CouponService` 호출부 수정. 프론트 계약인 `AvailableCouponDTO.maxOrderAmount` 필드명은 유지 |
| 12 | `CouponUsage` | — | `@ManyToOne` 에 잘못 붙은 `@Enumerated(STRING)` | 잘못된 `@Enumerated` 제거, 나머지 동일 |
| 13 | `EmailVerificationToken` | `pending_email`(V8) 매핑, 인덱스, `@PrePersist` 에서 만료 +24h | `pending_email` 없음, `@PrePersist` 에서 `isVerified=false` | `pendingEmail` 포함. `@PrePersist` 는 createdAt·만료(+24h) + `isVerified` 가 null 일 때만 false |
| 14 | `Event` | — | — | 동일 |
| 15 | `Inquiry` | 인덱스 3개 | `changeTitle/Content/Category/Reply/User/Accommodation()` | 인덱스 + 호스트 변경 메서드 모두 유지 (`changeReply` 는 `isReplied` 동기화) |
| 16 | `Notification` | `isRead = false` (빌더 기본값 경고) | `@Builder.Default` | `@Builder.Default` 채택 |
| 17 | `PasswordResetToken` | 정적 팩토리 `create()` | `@PrePersist`(createdAt, `isUsed=false`) | 둘 다 유지. `@PrePersist` 는 값이 비어 있을 때만 채움(`create()` 값 보존) |
| 18 | `Payment` | 인덱스, `@OneToOne(mappedBy) paymentDetail`(cascade ALL, orphanRemoval), `isCompleted/isCancelled/isTossPayment()` | 없음 | 모두 유지. 컬럼 추가 없음(역방향 연관). 호스트에는 Payment 삭제 경로가 없어 cascade 영향 없음 |
| 19 | `PaymentDetail` | `isCardPayment/isVirtualAccountPayment/isInstalledPayment()` | 없음 | 유지 |
| 20 | `PointHistory` | 인덱스 2개 | — | 인덱스 유지, 매핑 동일 |
| 21 | `PriceCalendar` | — | — | 동일 |
| 22 | `RefreshToken` | 인덱스 | — | 인덱스 유지, 매핑 동일 |
| 23 | `Region` | — | — | 동일 |
| 24 | `Reservation` | 인덱스 4개, `status`/`cancelReason` `@Setter`, `price` 정밀도 누락 | `public Long reservationId`, `price` `precision=10,scale=2`, `validateNew()/update(ReservationDTO,…)/cancel()` | PK 필드명은 양쪽 모두 `reservationId` 라 유지하되 **`private`** 로(호스트 직접 필드 접근 없음, JPQL `r.reservationId` 그대로). `price` 는 `DECIMAL(10,2)`. `@Setter` + 호스트 메서드 모두 유지. `update()` 는 호스트 DTO 의존을 끊기 위해 **`ReservationChanges` 인터페이스**를 받게 바꾸고 호스트 `ReservationDTO` 가 구현 |
| 25 | `Review` | `rating` `precision=3,scale=2`, `updateContent()`, `likeCount` `@Setter`, 인덱스 | `rating` `scale=1` | **`scale=2`** (V1 `DECIMAL(3,2)`). 사용자 메서드/세터 유지 |
| 26 | `ReviewImage` | 인덱스 | — | 인덱스 유지, 매핑 동일 |
| 27 | `Room` | — | — | 동일 |
| 28 | `RoomImage` | — | — | 동일 |
| 29 | `User` | `tokenValidAfter`(V9), `revokeExistingTokens()`, `verify/updateEmail/updateProfile…/deleteAccount()` | `isVerified` `@Builder.Default` | 모두 유지 + `@Builder.Default`(빌더 기본값이 null → false, `is_verified` NOT NULL) |
| 30 | `UserCoupon` | `use()`, 인덱스 | — | 유지 |
| 31 | `WishList` | 인덱스(`user_id, accommodation_id` UNIQUE 포함) | — | 인덱스 유지, 매핑 동일 |
| 32 | `ErrorLog` (error_log, V11) | 없음 | 호스트 전용 | **호스트에 남김** (`com.honeyrest.honeyrest_host.entity`). 다른 엔티티가 참조하지 않음 |

### 값 타입

| 타입 | 결정 |
|---|---|
| `ReservationStatus` | 상수·`OCCUPYING` 은 양쪽 동일. 호스트의 `ALL`, `isOccupying()`, `normalize()` 를 합쳐 `com.honeyrest.domain.type` 으로 이동 |
| `BannerPosition` | 값 동일(`MAIN_TOP`, `MAIN_MIDDLE`, `CATEGORY_TOP`). `com.honeyrest.domain.type` 으로 이동 |
| `ReservationChanges` (신규) | `Reservation.update()` 부분 변경 값 인터페이스. null = 변경 없음 |

## 3. 판단이 필요했던 점

- **감사(auditing) 방식 변경**: `AuditingEntityListener` → Hibernate 타임스탬프. `@CreationTimestamp` 는 INSERT 때 항상 현재 시각으로 덮어쓴다
  (호스트 폴백은 미리 채운 createdAt 을 보존했다). 두 앱 모두 createdAt 을 직접 채워 저장하는 코드가 없어 동작 차이는 없다. `@EnableJpaAuditing` 은 두 앱에 남아 있지만 무해하다.
- **`EmailVerificationToken` 만료**: 사용자 사본대로 `@PrePersist` 에서 항상 +24h 로 정한다. 호스트는 이 토큰을 만들지 않는다.
- **`AvailableCouponDTO.maxOrderAmount`**: 엔티티 필드명만 바꾸고 API 응답 필드명은 프론트엔드 계약이라 그대로 두었다.
- **QueryDSL Q 타입**: 엔티티가 모듈로 옮겨가 Q 타입도 `honeyrest-domain` 에서 생성된다(`com.honeyrest.domain.entity.QReservation`).
  저장소에 커밋돼 있던 옛 패키지의 Q 타입 스냅샷(`src/main/generated/…`, 빌드에 쓰이지 않음)은 삭제했다.
- **호스트 `entity/` 폴더의 레거시 SQL**(`accommodation.sql`, `insert.sql`)은 이번 범위 밖이라 `ErrorLog.java` 옆에 그대로 두었다.

## 4. 변경 절차 (앞으로)

1. 엔티티를 바꿀 때는 **이 저장소에서** `honeyrest-domain` 엔티티와 Flyway 마이그레이션을 같은 커밋으로 바꾼다.
2. `./gradlew test` (루트 + `:honeyrest-domain`) → 가능하면 `./gradlew integrationTest`(Docker).
3. 호스트 저장소에서 서브모듈을 그 커밋으로 재고정한다.
   ```bash
   cd honeyRest_host
   git submodule update --remote libs/honeyrest-user   # 또는: git -C libs/honeyrest-user checkout <커밋>
   ./gradlew test
   git add libs/honeyrest-user && git commit -m "honeyrest-user 서브모듈 재고정"
   ```
4. 커밋 전 변경을 호스트에서 바로 확인하려면 두 저장소를 나란히 두고 `./gradlew test -PhoneyrestUserDir=../honeyRest_user`.

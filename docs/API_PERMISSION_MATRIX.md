# API 권한 매트릭스 (2단계 완료 시점 기준)

작성일: 2026-08-11
범위: `honeyRest_user`(사용자 백엔드), `honeyRest_host`(호스트 백엔드)
기준 커밋: 각 저장소 `main` 최신 (1단계 + 2단계 병합 후)

이 문서는 [CLAUDE_REVIEW_ACTION_PLAN.md](CLAUDE_REVIEW_ACTION_PLAN.md) 2단계 항목 1·완료 게이트 G2의 산출물이다.
"인가 근거"는 각 엔드포인트가 실제로 무엇을 기준으로 접근을 허용/거부하는지를 가리키며,
**요청 본문·쿼리의 id를 신뢰하지 않는다**는 원칙이 지켜지는지가 핵심 점검 대상이다.

## 1. 역할 정의

| 저장소 | 역할 | 의미 |
| --- | --- | --- |
| 사용자 백엔드 | (익명) | 로그인하지 않은 방문자 |
| 사용자 백엔드 | USER | 로그인한 일반 회원 (JWT의 `role` 클레임이 아니라 매 요청마다 DB에서 재조회한 `user.role`) |
| 사용자 백엔드 | ADMIN | 배너/이벤트/숙소 태그 쓰기 전용 역할. **이 백엔드에는 ADMIN을 실제로 부여하는 가입 경로가 없다** — 즉 현재는 사실상 전면 차단(fail-closed) 상태이며, 호스트 백엔드로의 이관 또는 별도 발급 절차가 3단계 이후 과제다 |
| 호스트 백엔드 | (익명) | 로그인하지 않은 방문자 |
| 호스트 백엔드 | COMPANY_ADMIN | 입점 업체 관리자. 자기 회사 소유 리소스만 접근 가능 |
| 호스트 백엔드 | SUPER_ADMIN | 총관리자. 전 업체 데이터 접근 및 승인 권한 |

## 2. 신원 해석 방식

| 저장소 | 신원 판별 소스 | 비고 |
| --- | --- | --- |
| 사용자 백엔드 | `@AuthenticationPrincipal CustomUserPrincipal` → `principal.getUserId()` | `JwtTokenProvider.getAuthentication()`가 매 요청마다 `userRepository.findById()`로 DB 재조회하며, `tokenValidAfter`(로그아웃/비번변경 이후 발급 여부)와 `status != DELETED`를 함께 검사한다 |
| 호스트 백엔드 | `Authentication.getName()` (JWT의 `email` 클레임) → `CompanyResourceAccessService.currentCompanyId(authentication)` | 컨트롤러마다 흩어져 있던 신원 해석을 1단계·2단계에서 이 서비스로 점진적으로 통합함. `@AuthenticationPrincipal User`는 이 앱의 principal 타입이 String이라 항상 null이 되는 버그가 있었고(P0-3), 발견된 지점은 모두 수정함 |

## 3. 사용자 백엔드 — 엔드포인트별 인가 근거

| 엔드포인트 | 공개 범위 | 인가 근거 | 상태 |
| --- | --- | --- | --- |
| `GET /api/accommodations/**`, `/api/event/**`, `/api/banner/**` | 익명 포함 전체 공개 | 없음(의도된 공개 조회) | 안전 |
| `POST/PUT/DELETE /api/accommodations/**`, `/api/event/**`, `/api/banner/**` | ADMIN만 | `hasRole("ADMIN")` (발급 경로 없어 사실상 전면 차단) | 안전 |
| `GET /api/reserve/form-info` | 익명 허용(비회원 예약 지원) | `userId`를 쿼리에서 받지 않고 `principal`에서 도출, 비로그인 시 개인정보 필드는 비움 | 안전 (P0-4 수정) |
| `POST /api/payment/toss/confirm` | USER | `reservationInfo.userId`를 요청 바디값으로 두지 않고 항상 `principal.getUserId()`로 덮어씀 | 안전 (P0-7 수정) |
| `POST /api/wishList/toggle` | USER | `dto.userId` 대신 `principal.getUserId()` | 안전 (P1-4 수정) |
| `POST /api/review/write` | USER | `reservation.user == principal` && `status == COMPLETED` 검증 | 안전 (P0-6 수정) |
| `DELETE/POST /api/user/reviews/**` | USER | 서비스 계층에서 `review.user.userId == principal.userId` | 안전 |
| `GET/POST /api/user/reservations/**` | USER | `findByReservationIdAndUser_UserId`로 조회 자체가 스코프됨 | 안전 |
| `GET /api/user/coupons`, `/point-history` | USER | `principal.getUserId()` 고정, 쿼리 파라미터로 타인 id 지정 불가 | 안전 |
| `POST /api/files/upload`, `DELETE /api/files` | USER | folder 화이트리스트(`reviews`,`profile`) + DB 조회로 실제 소유 리소스인지 재검증 | 안전 (P1-5 수정) |
| `POST /api/user/email/verify-change` | USER (2단계에서 authenticated로 전환) | 토큰의 `tokenType == EMAIL_CHANGE`, `pendingEmail == newEmail`, 호출자 == `token.user` 모두 검증 | 안전 (P0-8 수정) |
| `POST /api/auth/logout` | USER | 로그아웃 시 `user.tokenValidAfter`를 갱신해 기존 access token을 전부 무효화 | 안전 (P1-10 수정) |

## 4. 호스트 백엔드 — 엔드포인트별 인가 근거

| 엔드포인트 | 공개 범위 | 인가 근거 | 상태 |
| --- | --- | --- | --- |
| `/admin/**` | COMPANY_ADMIN | `hasRole("COMPANY_ADMIN")` | 안전 |
| `/owner/**` | SUPER_ADMIN | `hasRole("SUPER_ADMIN")` | 안전 |
| `/api/owner/auth/signup` | SUPER_ADMIN | `/api/owner/**`를 `hasRole("SUPER_ADMIN")`로 명시 잠금 + 서비스가 요청 `role`을 그대로 신뢰하지 않고 검증된 호출자 컨텍스트에서만 role 지정 허용 | 안전 (P0-2 수정, 가장 심각했던 권한 상승) |
| `/api/admin/auth/signup`, `/login` | 익명(신규 업체 자가 가입 목적) | 서비스가 `role`을 항상 `COMPANY_ADMIN`으로 고정, 이메일 중복 체크 활성화 | 안전 |
| `/admin/price/**`(가격 캘린더) | COMPANY_ADMIN | 클라이언트가 보낸 `companyId`를 신뢰하지 않고 `CompanyResourceAccessService.currentCompanyId(auth)`로 재계산, `roomId`/`accommodationId`도 소유권 검증 | 안전 (P0-4 수정) |
| `/admin/reviews/**`, `/admin/inquiries/**` 단건 조작 | COMPANY_ADMIN | `ownsReview`/`ownsInquiry`로 accommodation 소유권 역추적 검증 | 안전 (P0-5 수정) |
| `/admin/point/refunds`(결제/환불) | COMPANY_ADMIN | `@AuthenticationPrincipal User`(항상 null이던 버그) 제거, `Authentication.getName()` 사용. companyId 못 구하면 fail-closed 빈 목록 | 안전 (P0-3 수정) |
| `/admin/accommodations/{id}/request` | COMPANY_ADMIN | 회사 소유권 확인 + `changeStatus`가 `PENDING` 외 값을 거부(자가 승인 차단) | 안전 (P1-1 수정) |
| `/owner/accommodation/{id}/approve`, `/reject` | SUPER_ADMIN | `/owner/**` 잠금 + `updateStatusIfCurrent`로 `PENDING → ACTIVE/REJECTED` 원자적 전이만 허용 | 안전 (P1-1/P1-2 수정, 신설) |
| `POST /admin/rooms/{roomId}` | COMPANY_ADMIN | `roomId` 소유권 + 폼의 `accommodationId`도 같은 회사 소속인지 검증 | 안전 (P1-3 수정) |
| 숙소 이미지 삭제(`deleteSubImageIds`) | COMPANY_ADMIN | `accommodationId`와 `imageId`가 실제로 연결된 경우만 삭제(`deleteByAccommodation_AccommodationIdAndImageIdIn`) | 안전 (P1-4 수정) |
| JWT 인증 전반 | - | `typ` 클레임이 `access`인 토큰만 인증에 사용, `refresh` 토큰으로는 인증 불가 | 안전 (P1-7 수정) |
| 폼 기반 관리자 콘솔 전체 (`/admin/**`, `/owner/**`의 POST) | COMPANY_ADMIN/SUPER_ADMIN | CSRF 보호 **비활성 상태** | **미해결** — 별도 작업으로 분리(30개+ 템플릿에 토큰 필드 추가 필요) |

## 5. 알려진 잔여 위험 (3단계 이후로 이연)

- **예약 재고/동시성**: 날짜별 재고 테이블이 없어 동일 객실 중복 예약(오버부킹)이 가능. 3단계 항목.
- **결제 보상 트랜잭션**: Toss 승인 후 DB 실패 시 자동 취소·재처리가 없음. 4단계 항목.
- **CSRF 재활성화**: 위 표 참고, 별도 작업으로 분리됨(`task_1cdef24a`).
- **principal resolver 완전 통합**: 호스트 백엔드는 이번 단계에서 손댄 컨트롤러 위주로 `CompanyResourceAccessService`에 수렴시켰으나, 전체 컨트롤러를 대상으로 한 전수 감사는 아직 하지 않았다.

## 6. Gate G2 체크리스트

- [x] 이번 단계에서 변경한 API는 본문·쿼리의 사용자/회사 ID를 권한 근거로 쓰지 않는다.
- [x] 이번 단계에서 다룬 저장소 조회에 `null`을 전달해 전체 테넌트가 노출되는 경로를 닫았다(`PaymentRepository` 등).
- [x] 이번 단계에서 수정한 항목에 대해 역할별 허용·거부 회귀 테스트를 추가했다
      (`OwnerAuthSignupSecurityTest`, `PaymentServiceImplTest`, `OAccommodationServiceImplTest`,
      `JwtAuthFilterTest`, `AdminWriteApiSecurityTest`, `FileControllerTest`, `ReviewServiceTest`,
      `JwtTokenProviderTest` 등).
- [ ] **전체 API 표면에 대한 완전한 권한 매트릭스**는 아니다 — 이번에 다룬 항목 위주다. 나머지 컨트롤러
      전수 점검은 3단계 이후 별도 감사로 남겨둔다.

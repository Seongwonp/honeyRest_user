# Render 무료 데모 배포 (보여주기용)

포트폴리오 링크용 **보여주기 전용 데모**를 Render 무료 플랜에 올리는 방법입니다.
운영 배포(Oracle Cloud VM + Docker Compose + MySQL/Redis)는 [DEPLOY.md](DEPLOY.md) 를 보세요. 이 문서의 구성은 운영 구성과 별개이며 `deploy/` 를 쓰지 않습니다.

| 서비스 | 주소(예상) | 저장소 / 빌드 파일 | 내용 |
|--------|-----------|-------------------|------|
| `honeyrest` | https://honeyrest.onrender.com | honeyRest_user · [`Dockerfile.render`](../Dockerfile.render) | React SPA + 사용자 API (한 컨테이너, 같은 Origin) |
| `honeyrest-admin` | https://honeyrest-admin.onrender.com | honeyRest_host · `Dockerfile.render` | 업체 관리자 / 총관리자 화면 |

두 서비스 모두 [`render.yaml`](../render.yaml)(Blueprint) 하나로 만들어집니다.

## 데모 계정 (공개용 — 버리는 계정)

| 사이트 | 역할 | 이메일 | 비밀번호 |
|--------|------|--------|----------|
| 사용자 | 회원 | `demo@honeyrest.app` | `Demo1234!` |
| 관리자 | 업체 관리자 (허니레스트 스테이) | `contact@honeyrest.com` | `Demo1234!` |
| 관리자 | 최고 관리자 | `admin@honeyrest.com` | `Demo1234!` |

- 사용자 로그인 화면의 **"데모 계정으로 로그인"** 버튼, 관리자 로그인 화면의 **데모 계정 버튼**(클릭 시 입력)으로 바로 들어갈 수 있습니다.
- 결제는 **"테스트 결제(데모)"** 버튼으로 진행되며 실제로 청구되지 않습니다(서버의 토스 스텁이 승인).
- 관리자 비밀번호는 `render.yaml` 의 `DEMO_COMPANY_PASSWORD` / `DEMO_ADMIN_PASSWORD` 값입니다. 바꾸면 로그인 화면 안내도 같이 바뀝니다.

## 데모 구성 요약

| 항목 | 데모(Render) | 운영(Oracle VM) |
|------|--------------|-----------------|
| 프로필 | `demo` | `prod` |
| DB | H2 인메모리(MySQL 모드), 기동 시 시드 적재 → **재시작하면 초기화** | MySQL 8.0 (Flyway) |
| Redis | 없음 (사용자: 인메모리 대체 / 관리자: 끔) | Redis 7 |
| 메일 | 발송 안 함 → 신규 가입의 이메일 인증을 끝낼 수 없음 (데모 계정 사용) | Gmail SMTP |
| 결제 | 토스 스텁 (항상 승인, 청구 없음) | 토스페이먼츠 |
| 소셜 로그인 | 사용 안 함 (데모 로그인 화면에서 숨김) | 카카오 / 구글 |
| 두 앱의 데이터 | **서로 분리** — 관리자에서 바꾼 내용이 사용자 사이트에 보이지 않음 | 같은 DB 공유 |

- 사용자 시드: [`db/demo-seed.sql`](../src/main/resources/db/demo-seed.sql) — 숙소 10곳(서울·부산·제주·강원·경북), 객실 20개, 리뷰 31개, 배너·이벤트·쿠폰, 데모 회원의 예약 5건(이용 완료 2 / 예정 2 / 취소 1)·찜·문의·포인트 내역
- 관리자 시드: honeyRest_host 의 `db/screenshot-seed.sql` (screenshot 프로필과 같은 데이터, 날짜는 오늘 기준 상대값)
- `/e2e/**` 테스트 보조 API 는 e2e 프로필 전용이라 데모에는 없습니다. demo 는 e2e 의 스텁(토스·메일·인메모리 Redis)만 공유합니다.

---

## 1. 처음 배포하기 (Blueprint)

**사전 준비**: 세 저장소(honeyRest_user `main`, honeyRest_host `main`, honeyrest_user_react `master`)에 데모 관련 변경이 **모두 push 되어 있어야** 합니다.
Render 는 GitHub 의 원격 저장소를 빌드하며, 사용자 이미지는 빌드 중에 프론트엔드 저장소를 clone 합니다(VITE_DEMO 기능이 없는 커밋이면 빌드가 실패하도록 막아 두었습니다).

1. https://dashboard.render.com 에 GitHub 계정으로 로그인합니다.
2. 오른쪽 위 **New** → **Blueprint** 를 누릅니다.
3. **Connect a repository** 에서 `Seongwonp/honeyRest_user` 를 고릅니다.
   (처음이면 "Configure GitHub App" 으로 Render 앱에 저장소 접근 권한을 줍니다. 세 저장소 모두 public 이라 honeyRest_user 만 연결해도 되지만, 자동 배포를 받으려면 honeyRest_host 도 권한 목록에 넣어 두는 것이 좋습니다.)
4. **Blueprint Name** 은 아무 이름(예: `honeyrest-demo`), **Branch** 는 `main` 그대로 둡니다.
5. Render 가 `render.yaml` 을 읽어 `honeyrest`, `honeyrest-admin` 두 개의 Web Service(Free)를 보여 줍니다. 환경변수 값은 파일에 들어 있으므로 따로 입력할 것이 없습니다.
6. **Apply** (또는 Deploy Blueprint) 를 누릅니다. 두 서비스가 동시에 빌드됩니다.

### 첫 빌드 예상 시간

| 서비스 | 예상 | 주요 단계 |
|--------|------|----------|
| `honeyrest` | 약 8~15분 | 프론트 clone + `npm ci` + Vite 빌드 → Gradle 의존성 다운로드 + bootJar → CDS 아카이브 생성 |
| `honeyrest-admin` | 약 7~12분 | (서브모듈이 비어 있으면) honeyRest_user clone → Gradle bootJar → CDS 아카이브 생성 |

이후 빌드는 Docker 레이어 캐시 덕분에 더 짧습니다. 빌드 후 첫 기동(아래 "콜드 스타트")까지 1~3분이 더 걸립니다.
빌드 로그는 서비스 화면의 **Events / Logs** 탭에서 볼 수 있습니다.

### 주소 확인

- 대시보드 → 서비스 이름 클릭 → 화면 위쪽의 `https://<이름>.onrender.com` 링크가 공개 주소입니다.
- 서비스 상태가 **Live** 가 되면 접속할 수 있습니다. `https://<주소>/actuator/health` 가 `{"status":"UP"}` 이면 정상입니다.

### 주소가 예상과 다를 때 (이름 충돌)

`honeyrest` / `honeyrest-admin` 이름이 이미 다른 사람에게 쓰이고 있으면 Render 가 `honeyrest-xxxx.onrender.com` 처럼 접미사를 붙입니다. 이 경우:

1. `render.yaml` 에서 실제 주소로 바꿉니다.
   - `honeyrest` 서비스: `APP_BASE_URL` (사용자 주소), `VITE_ADMIN_URL` (관리자 주소)
2. 커밋·push 하면 자동 배포됩니다. (또는 대시보드 → 서비스 → **Environment** 에서 같은 값을 바꾸고 **Save, rebuild, and deploy**)
   - `VITE_ADMIN_URL` 은 SPA 번들에 상수로 들어가므로 **재빌드**가 필요합니다(재시작만으로는 안 바뀜).
3. 세 저장소 README 의 "라이브 데모" 주소도 실제 주소로 고칩니다.

---

## 2. 콜드 스타트 (무료 플랜의 잠듦)

- 무료 Web Service 는 **15분 동안 요청이 없으면 잠듭니다(spin down).** 다음 요청이 오면 컨테이너를 다시 띄우는데, 그동안 브라우저는 Render 의 대기 화면을 보여 줍니다.
- 이 앱들은 JVM + Spring Boot 라 **0.1 CPU 에서 기동에 1~3분** 걸립니다. 로컬에서 `--memory=512m --cpus=0.1` 로 잰 값:

| 서비스 | 기동(health UP)까지 | 기동 후 메모리 | 비고 |
|--------|--------------------|----------------|------|
| `honeyrest` | 약 105초 (CDS 없이 약 150초) | 약 240~270MB / 512MB | 힙 사용 ~70MB |
| `honeyrest-admin` | 약 150초 (CDS 없이 약 160초) | 약 350~380MB / 512MB | 힙 사용 ~90MB |

- 다시 깨어나면 **데이터가 시드 상태로 초기화**됩니다(인메모리 DB). 데모 중에 만든 예약·리뷰·관리자 변경은 사라집니다.
- 면접·발표 직전에는 두 주소를 미리 한 번씩 열어 깨워 두세요(각각 1~3분).
- 기동을 빠르게 하려고 이미지 빌드 때 **CDS(클래스 데이터 공유) 아카이브**를 만들어 둡니다(`Dockerfile.render`). `spring.main.lazy-initialization` 은 쓰지 않습니다:
  시간 이득이 작고(사용자 약 7초) 첫 요청이 그만큼 느려지며, 관리자 앱에서는 시드 SQL 초기화 빈까지 지연되어 데이터가 비는 문제가 확인됐습니다.

---

## 3. 업데이트하기

- `render.yaml` 에 `autoDeploy: true` 가 있어 **honeyRest_user `main` / honeyRest_host `main` 에 push 하면 해당 서비스가 자동으로 다시 빌드·배포**됩니다.
- **프론트엔드(honeyrest_user_react)만 바꿨을 때**: 사용자 이미지는 빌드 중에 프론트를 clone 하므로 honeyRest_user 에 변경이 없으면 자동 배포가 일어나지 않고, 수동 배포해도 Docker 캐시 때문에 예전 clone 이 재사용될 수 있습니다.
  → 대시보드 → `honeyrest` → **Manual Deploy → Clear build cache & deploy**
- **공유 도메인 모듈(honeyrest-domain)을 바꿨을 때**: 관리자 이미지는 서브모듈 또는 `USER_REF`(기본 `main`)의 honeyRest_user 를 씁니다. 서브모듈을 새 커밋으로 고정(re-pin)해 honeyRest_host 에 push 하거나, `honeyrest-admin` 에서 **Clear build cache & deploy** 를 합니다.
- 환경변수만 바꾸려면 대시보드 → 서비스 → **Environment** → 수정 → **Save and deploy** (빌드 인자인 `VITE_ADMIN_URL`·`FRONTEND_REF`·`USER_REF` 는 **Save, rebuild, and deploy**).
- Blueprint 와 대시보드 설정이 어긋나면 Blueprint 화면의 **Manual Sync** 로 `render.yaml` 을 다시 적용합니다.

---

## 4. 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| 빌드 실패: `프론트엔드에 VITE_DEMO 기능이 없습니다` | honeyrest_user_react `master` 에 데모 변경이 push 되지 않았거나 `FRONTEND_REF` 가 다른 브랜치. push 후 Clear build cache & deploy |
| 빌드 실패: Gradle `Could not resolve` / `429` | Maven Central 일시 제한. 잠시 뒤 **Manual Deploy** 로 재시도 |
| 배포 후 계속 `Deploying` → 실패 (`Exited with status 137` / `OutOfMemoryError`) | 512MB 초과. 서비스 → Environment → `JAVA_TOOL_OPTIONS` 에서 `-XX:MaxRAMPercentage=50` 을 `45` 로 낮추거나 `-XX:MaxMetaspaceSize=160m` 확인. `-XX:+ExitOnOutOfMemoryError` 로 OOM 시 바로 재시작된다 |
| 배포가 health check 실패로 끝남 | 기동이 오래 걸려 시간 초과된 경우. Logs 에 `Started ... in N seconds` 가 찍히는지 확인하고 **Manual Deploy** 로 재시도. 계속되면 Settings → Health Check Path 를 비워 두고(포트 열림만 확인) 재배포 |
| JVM 옵션을 바꾼 뒤 기동이 느려짐 | CDS 아카이브는 이미지 빌드 때의 JVM 옵션(GC 등)과 맞아야 쓰인다. GC 를 바꾸면 Dockerfile.render 의 `JAVA_TOOL_OPTIONS` 도 같이 바꿔 재빌드 |
| 첫 접속이 1~3분 걸림 | 정상(콜드 스타트). 위 2번 참고 |
| 로그인 직후 바로 로그아웃됨 (사용자) | `COOKIE_SECURE=true` 인데 http 로 접속한 경우. 항상 `https://` 주소로 접속 |
| 관리자 로그인 화면에 데모 계정이 안 보임 | `DEMO_COMPANY_PASSWORD` / `DEMO_ADMIN_PASSWORD` 가 비어 있음 (비어 있으면 계정도 만들지 않는다) |
| 데모 배너의 "관리자 데모 열기" 링크가 틀림 | `VITE_ADMIN_URL` 수정 후 **재빌드** (위 "주소가 예상과 다를 때") |
| 이번 달 중간에 서비스가 멈춤 | 무료 인스턴스 시간은 **워크스페이스 전체 합산 월 750시간**. 두 서비스가 24시간 깨어 있으면 1,440시간이라 초과할 수 있다(보통은 15분 무접속 시 잠들어 훨씬 적게 쓴다). 업타임 모니터로 계속 깨워 두지 말 것. 대시보드 → Billing 에서 사용량 확인 |
| 이미지가 안 보임 | 시드 이미지는 `/uploads/placeholder/*.svg` (이미지에 포함). 데모에서 업로드한 파일은 디스크가 영구적이지 않아 재시작 시 사라진다 |

## 5. 로컬에서 같은 이미지 확인

```bash
# 사용자 (honeyRest_user 루트)
docker build -f Dockerfile.render -t honeyrest-demo .
docker run --rm -p 8080:8080 --memory=512m --cpus=0.1 honeyrest-demo
# → http://localhost:8080  (데모 배너, 데모 로그인, 테스트 결제(데모))

# 관리자 (honeyRest_host 루트)
docker build -f Dockerfile.render -t honeyrest-admin-demo .
docker run --rm -p 8081:8081 --memory=512m --cpus=0.1 \
  -e DEMO_COMPANY_PASSWORD='Demo1234!' -e DEMO_ADMIN_PASSWORD='Demo1234!' honeyrest-admin-demo
# → http://localhost:8081/auth/login

# Docker 없이 (사용자): SPA 를 먼저 VITE_DEMO=true 로 빌드해 APP_STATIC_DIR 로 넘긴다
#   (프론트 저장소) VITE_DEMO=true VITE_BACKEND_URL= npm run build
#   APP_STATIC_DIR=../honeyrest_user_react/dist ./gradlew bootRun --args='--spring.profiles.active=demo'
```

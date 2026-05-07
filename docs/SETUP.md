# ⚙️ HoneyRest – 실행 전 필수 설정

> 프로젝트를 로컬에서 실행하기 위한 환경 설정 가이드입니다.

---

## 1️⃣ Redis 설치 및 실행

HoneyRest는 인기 숙소 캐싱, 배너 캐싱, 조회수 관리 등에 Redis를 사용합니다.

### macOS (Homebrew)

```bash
brew install redis
brew services start redis   # 실행
brew services stop redis    # 중지
```

### Ubuntu / Linux

```bash
sudo apt update
sudo apt install redis-server
sudo systemctl enable redis-server.service
sudo systemctl start redis-server
```

### Windows

Redis for Windows (예: Memurai) 설치 후 실행. 기본 포트 6379 사용.

### 설치 확인

```bash
redis-cli ping
# PONG 이 출력되면 정상
```

---

## 2️⃣ MySQL 설치 및 실행

### macOS (Homebrew)

```bash
brew install mysql
brew services start mysql
```

### 데이터베이스 생성

```sql
CREATE DATABASE honey_rest_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Flyway가 애플리케이션 시작 시 스키마를 자동으로 마이그레이션합니다. (`V1__baseline.sql`)

---

## 3️⃣ 환경 변수 설정 (`application_security.properties`)

`src/main/resources/application_security.properties` 파일을 직접 생성하고 아래 내용을 채워주세요.

```properties
# Database password
spring.datasource.password=YOUR_DB_PASSWORD

# JWT secret key
jwt.secret-key-value=YOUR_SECURE_JWT_SECRET_KEY_32CHARS_OR_MORE

# Toss Payments
com.tjfgusdh.toss.widgetClientKey=YOUR_TOSS_CLIENT_KEY
com.tjfgusdh.toss.widgetSecretKey=YOUR_TOSS_SECRET_KEY

# OpenWeather API
weather.api.key=YOUR_OPENWEATHER_API_KEY

# Firebase service account JSON 파일명 (src/main/resources/ 에 위치)
fire.base.secretKey=honeyrest-firebase-adminsdk.json

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

### 🚨 보안 주의사항

- `application_security.properties`는 반드시 `.gitignore`에 포함해야 합니다.
- GitHub 공개 저장소에 절대 올리지 마세요.
- 팀 협업 시 `application_security.properties.example` 형식만 공유하고, 실제 키 값은 각 개발자가 직접 채워넣어야 합니다.

---

## 4️⃣ Firebase 설정

1. Firebase Console에서 서비스 계정 JSON 키를 발급합니다.
2. 발급받은 JSON 파일을 `src/main/resources/` 폴더에 넣습니다.
3. `application_security.properties`의 `fire.base.secretKey` 값을 해당 파일명으로 설정합니다.

```
src/main/resources/
└── honeyrest-firebase-adminsdk.json  ← 이 파일
```

---

## 5️⃣ 실행 순서

```bash
# 1. Redis 실행 확인
redis-cli ping   # PONG

# 2. MySQL 실행 확인
brew services list | grep mysql   # started

# 3. Spring Boot 실행 (IntelliJ 또는 Gradle)
./gradlew bootRun
```

> 서버 기동 후 Swagger UI: http://localhost:8080/swagger-ui.html

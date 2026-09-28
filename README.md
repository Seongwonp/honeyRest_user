# 🐝 HoneyRest – 감성 숙소 예약 플랫폼 (User API)
👨‍💻 Created by 박성원 (Seongwon Park) – User 영역 총괄 & 팀장

## 🎥 HoneyRest 광고 영상

[![HoneyRest 광고 영상](https://github.com/Seongwonp/honeyRest_user/blob/main/%E1%84%92%E1%85%A5%E1%84%82%E1%85%B5%E1%84%85%E1%85%A6%E1%84%89%E1%85%B3%E1%84%90%E1%85%B3.gif?raw=true)](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2F%E1%84%92%E1%85%A5%E1%84%82%E1%85%B5%E1%84%85%E1%85%A6%E1%84%89%E1%85%B3%E1%84%90%E1%85%B3.mp4?alt=media&token=1d89a752-00e0-4c82-b6c0-94723c57cc70)

> 🎬 클릭하면 전체 광고 영상을 볼 수 있습니다.

---

## 📌 프로젝트 개요

**HoneyRest**는 감성 숙소 예약을 위한 **풀스택 웹 플랫폼**입니다.
사용자(User), 업체 관리자(Company Admin), 총 관리자(Super Admin)로 역할을 분리하여
숙소 검색부터 예약, 리뷰 작성, 결제, 관리자 통계까지 전체 사용자 흐름을 하나의 시스템으로 통합 구현했습니다.

- **프로젝트 기간**: 2025.08.04 ~ 2025.09.04 (총 4주)
- **팀원 구성**:
  - 👤 박성원 (팀장) – 사용자(User) 영역 개발 총괄 & 광고 영상 제작 / 전체 DB 설계 및 ERD 작성 / 전체 시스템 통합 및 코드 리뷰
  - 🏨 김민경 – 업체 관리자(Company Admin) 시스템 개발
  - 🛡️ 설현오 – 총 관리자(Super Admin) 시스템 개발

---

## 📦 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot 3.5.4, Java 17, Spring Security |
| **인증** | JWT (jjwt), OAuth2 (Google, Kakao) |
| **DB** | MySQL / MariaDB, Spring Data JPA, QueryDSL, Flyway |
| **캐시** | Redis |
| **파일 스토리지** | 로컬 디스크(기본, `/uploads/**`) 또는 Firebase Storage — `app.storage.type`으로 선택 |
| **결제** | Toss Payments |
| **메일** | Spring Mail (Gmail SMTP) |
| **API 문서** | SpringDoc OpenAPI / Swagger UI |
| **Frontend** | React (별도 레포) |

---

## ▶️ 실행 방법

### 1. 사전 준비

- JDK 17, MySQL 8 (또는 MariaDB), Redis
- `src/main/resources/application_security_ex.properties`를 복사해 `application_security.properties`를 만들고 값을 채웁니다.
  (DB 비밀번호, JWT 키, OAuth2, Gmail, Toss, OpenWeather 등 — 이 파일은 gitignore 대상이며 **필수**입니다.)

### 2. 파일 저장소 선택 (`app.storage.type`)

| 값 | 동작 | 필요 조건 |
|----|------|-----------|
| `local` (기본값) | `app.storage.local.dir`(기본 `./uploads`)에 저장, `/uploads/**`로 서빙 | 없음 |
| `firebase` | Firebase Storage에 업로드 | `fire.base.secretKey` 서비스 계정 키 파일(classpath) |

환경변수로도 지정할 수 있습니다.

| 환경변수 | 대응 프로퍼티 | 기본값 |
|----------|---------------|--------|
| `APP_STORAGE_TYPE` | `app.storage.type` | `local` |
| `APP_STORAGE_LOCAL_DIR` | `app.storage.local.dir` | `./uploads` |
| `FIREBASE_STORAGE_BUCKET` | `app.storage.firebase.bucket` | 기존 프로젝트 버킷 |
| `FIREBASE_PROJECT_ID` | `app.storage.firebase.project-id` | 기존 프로젝트 ID |

로컬 모드에서 기존 DB의 Firebase 이미지 URL을 플레이스홀더로 바꾸려면 `scripts/seed-local-images.sql`을 실행합니다.
(프론트엔드 개발 서버를 쓰는 경우 `/uploads` 경로도 백엔드로 프록시해야 이미지가 보입니다.)

### 3. 실행

```bash
# 기본 실행 (로컬 저장소)
./gradlew bootRun

# 로컬 개발 프로필 (SQL 로그 출력)
./gradlew bootRun --args='--spring.profiles.active=local'

# Firebase 저장소 사용
APP_STORAGE_TYPE=firebase ./gradlew bootRun

# 테스트 (@SpringBootTest 테스트는 로컬 MySQL/Redis 필요)
./gradlew test
```

자세한 환경 설정은 [SETUP.md](docs/SETUP.md)를 참고하세요.

---

## 🔗 문서 네비게이션

| 문서 | 설명 |
|------|------|
| [⚙️ SETUP.md](docs/SETUP.md) | 실행 전 환경 설정 (Redis, 환경변수, Firebase) |
| [🧑‍💻 FEATURES.md](docs/FEATURES.md) | 주요 기능 및 화면 소개 |
| [🗂️ ARCHITECTURE.md](docs/ARCHITECTURE.md) | 프로젝트 구조, ERD, DB 설계 |
| [⚡ IMPROVEMENTS.md](docs/IMPROVEMENTS.md) | 안정화 · 테스트 · 성능 최적화 · DB 안정화 |
| [🧰 STABILIZATION.md](docs/STABILIZATION.md) | 재가동 안정화 단계와 검증 결과 |
| [🗃️ DB_SCHEMA.md](DB_SCHEMA.md) | 전체 테이블 컬럼 상세 명세 |
| [💭 RETROSPECTIVE.md](docs/RETROSPECTIVE.md) | 프로젝트 회고 |

---

## 🚀 관련 레포지토리

- [User React Frontend](https://github.com/Seongwonp/honeyrest_user_react)
- [Admin / Host 시스템](https://github.com/Seongwonp/honeyRest_host)

---

## 🎬 시연 영상 & 발표 자료

- 📺 [숙소 검색 → 예약 → 결제 시연 영상](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2FHoneyRest_Pay.mp4?alt=media&token=b96a6897-b48b-4138-a5cf-fdd2e53caefb)
- 📄 [발표 자료 PDF](https://github.com/user-attachments/files/22292418/HoneyRest.pdf)

---

## 🙋‍♂️ 개발자 정보

**박성원 (Seongwon Park)** – 팀장 / 사용자(User) 영역 총괄

- User API 백엔드 및 프론트엔드 전체 설계 및 개발
- 전체 DB 설계 및 ERD 작성
- 관리자 시스템 기술 방향 결정 (Thymeleaf) 및 전체 시스템 통합
- 광고 영상 및 일러스트 제작 / 프로젝트 발표용 PPT 기획 및 디자인 총괄

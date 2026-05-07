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
| **Backend** | Spring Boot 3.5.4, Java 17, Spring Security, Spring Batch |
| **인증** | JWT (jjwt), OAuth2 (Google, Kakao) |
| **DB** | MySQL / MariaDB, Spring Data JPA, QueryDSL, Flyway |
| **캐시** | Redis |
| **파일 스토리지** | Firebase Storage, S3 (Filebase) |
| **결제** | Toss Payments |
| **메일** | Spring Mail (Gmail SMTP) |
| **API 문서** | SpringDoc OpenAPI / Swagger UI |
| **Frontend** | React (별도 레포) |

---

## 🔗 문서 네비게이션

| 문서 | 설명 |
|------|------|
| [⚙️ SETUP.md](docs/SETUP.md) | 실행 전 환경 설정 (Redis, 환경변수, Firebase) |
| [🧑‍💻 FEATURES.md](docs/FEATURES.md) | 주요 기능 및 화면 소개 |
| [🗂️ ARCHITECTURE.md](docs/ARCHITECTURE.md) | 프로젝트 구조, ERD, DB 설계 |
| [⚡ IMPROVEMENTS.md](docs/IMPROVEMENTS.md) | 안정화 · 테스트 · 성능 최적화 · DB 안정화 |
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

# 🐝 HoneyRest – 감성 숙소 예약 플랫폼 (User API)
👨‍💻 Created by 박성원 (Seongwon Park) – User 영역 총괄

![HoneyRest 광고 클립 GIF](./허니레스트.gif)

> 🎞️ 위 GIF는 광고 영상의 주요 장면을 클립으로 구성한 이미지입니다.

HoneyRest는 사용자, 호스트, 관리자 역할이 분리된 **풀스택 숙소 예약 플랫폼**입니다.  
숙소 검색, 예약, 리뷰, 이미지 업로드 등 다양한 기능을 제공하며,  
확장성과 성능을 고려한 백엔드 설계와 직관적인 프론트엔드 UI를 구현했습니다.

## 🎥 HoneyRest 광고 영상

> 감성 숙소 예약 플랫폼 HoneyRest의 분위기를 담은 광고 영상입니다.

[📺 광고 영상 보러가기](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2F%E1%84%92%E1%85%A5%E1%84%82%E1%85%B5%E1%84%85%E1%85%A6%E1%84%89%E1%85%B3%E1%84%90%E1%85%B3.mp4?alt=media&token=1d89a752-00e0-4c82-b6c0-94723c57cc70)

---

## 📝 프로젝트 발표 자료

> HoneyRest의 전체 기획, 기능 흐름, 기술 스택, 시연 화면 등을 담은 발표용 PPT입니다.  
> 자세한 내용은 아래 PDF를 참고해주세요.

📄 [HoneyRest 발표 자료 (PDF)](https://github.com/user-attachments/files/22292418/HoneyRest.pdf)

---

## 📦 기술 스택 (User API 기준)

| 구분               | 기술 / 라이브러리               | 역할 / 설명                          | 사용처 / 특징                                  |
|--------------------|----------------------------------|--------------------------------------|------------------------------------------------|
| **프레임워크**       | Spring Boot                     | 백엔드 애플리케이션 프레임워크       | REST API, DI, 설정 관리                        |
| **보안 / 인증**     | Spring Security                 | 로그인 / 권한 관리                   | JWT 인증, 소셜 로그인(Google, Kakao)           |
| **JWT**            | jjwt                            | Access/Refresh 토큰 관리             | 만료 관리, 재발급                              |
| **OAuth2**         | Spring Security OAuth2          | 소셜 로그인                           | 인증, 사용자 정보 조회, redirect 처리          |
| **DB**             | MariaDB                         | 관계형 데이터 저장소                 | JPA 기반 CRUD, QueryDSL 가능                   |
| **ORM**            | Spring Data JPA                 | 엔티티 → DB 매핑                     | 엔티티 기반 설계                               |
| **캐시**           | Redis                           | 인기 숙소 / 지역 캐시, 조회수 관리   | ZSet, TTL 활용                                 |
| **파일 업로드**     | Firebase Storage                | 이미지 / 파일 저장                   | 숙소 이미지, 프로필 이미지                     |
| **메일 발송**       | Spring Mail (Gmail SMTP)        | 인증 / 알림 이메일                   | 이메일 인증, 알림, 비밀번호 재설정             |
| **API 문서화**      | SpringDoc OpenAPI / Swagger UI  | REST API 문서 자동화                 | `/api-docs`, `/swagger-ui.html`                |
| **환경 / 보안 관리** | application_security.properties | 민감 정보 분리                        | DB 패스워드, Firebase 키, JWT 시크릿 등         |

---

## 🧑‍💻 주요 기능

### 👤 사용자 (User)
- 숙소 검색 및 예약
- 관심 숙소 등록
- 리뷰 작성 및 확인
- 마이페이지 관리
- Redis 기반 추천/검색 캐싱

### 🏨 업체 관리자 (Company Admin)
- 숙소 등록/수정/삭제
- 객실 재고 및 가격 캘린더 관리
- 예약 현황 및 리뷰 응답
- 매출 통계 대시보드

### 🛡️ 총 관리자 (Super Admin)
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
  ├── admin/           # 관리자 시스템 (Thymeleaf 기반)
  │
  └── README.md               # 전체 프로젝트 설명
```

- [User_React 바로가기](https://github.com/Seongwonp/honeyrest_user_react)
- [Admin 바로가기](https://github.com/Seongwonp/honeyRest_host)


## 🗃️ 데이터베이스 설계 (ERD)

HoneyRest의 데이터베이스는 사용자, 숙소, 예약, 리뷰, 관리자 기능까지 모두 포함하는  
**도메인 중심의 관계형 구조**로 설계되었습니다. 확장성과 무결성을 고려해 정규화된 테이블로 구성되어 있으며,  
JPA 기반의 ORM 매핑을 통해 엔티티와 DB가 유기적으로 연결됩니다.

### 📌 ERD 이미지

![HoneyRest ERD](https://github.com/user-attachments/assets/f9be0cb6-0e3b-43c4-800c-58c6c78a55f5)

> 📄 자세한 ERD 구조는 PDF 문서에서 확인 가능합니다.

---

### 📋 주요 테이블 요약

| 테이블명         | 설명 |
|------------------|------|
| `User`           | 사용자 정보, 권한, 알림 설정 등  
| `Accommodation`  | 숙소 정보, 위치, 태그, 이미지  
| `Room`           | 객실 정보, 재고, 가격, 캘린더  
| `Reservation`    | 예약 내역, 상태, 결제 정보  
| `Review`         | 리뷰 내용, 평점, 신고 여부  
| `Admin`          | 관리자 계정, 권한, 통계  
| `Coupon`         | 할인 쿠폰, 조건, 유저 연결  
| `Notification`   | 이메일/앱 알림 내역  
| `WishList`       | 관심 숙소 저장 기능  
| `Policy`         | 취소 정책, 운영 기준  

---

### 🧠 설계 포인트

- **정규화**: 중복 최소화, 관계 명확화  
- **확장성**: 관리자 기능, 쿠폰, 알림 등 확장 가능한 구조  
- **보안**: 토큰 관리, 민감 정보 분리, 권한 기반 접근 제어  
- **성능**: Redis 캐싱, 인기 숙소 조회수 ZSet 구조 활용

---

> “데이터베이스는 서비스의 뼈대입니다. HoneyRest는 그 뼈대를 세심하게 설계했습니다.”

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

> 추가 화면은 포트폴리오 또는 시연 영상에서 확인 가능합니다.


## 🎬 사용자 시연 영상

> HoneyRest의 사용자 흐름을 실제 화면 기반으로 시연한 영상입니다.  
> 숙소 검색 → 예약 → 결제 → 마이페이지 확인까지 전체 흐름을 담았습니다.

 - 📺 [User 시연 영상 보러가기](https://firebasestorage.googleapis.com/v0/b/honeyrest-7fb60.firebasestorage.app/o/video%2FHoneyRest_Pay.mp4?alt=media&token=b96a6897-b48b-4138-a5cf-fdd2e53caefb)
   
---

## 🙋‍♂️ 개발자 정보

**박성원 (Seongwon Park)** – 사용자(User) 영역 총괄

- **User API 백엔드 및 프론트엔드 전체 설계 및 개발**
- **DB 설계 및 ERD 작성**
- **API 명세서 작성 및 문서화**
- **광고 영상 및 일러스트 제작**
- **프로젝트 발표용 PPT 기획 및 디자인 총괄**


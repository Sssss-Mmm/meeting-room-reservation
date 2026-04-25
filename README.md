# Everspin Meeting Room Reservation System

회의실 예약 및 관리 웹 애플리케이션입니다.

## 기술 스택

- **Backend**: Java 21, Spring Boot 3.2.5, Spring Security 6, Spring Data JPA
- **Frontend**: Thymeleaf 3.1, Vanilla JS, Custom CSS
- **Database**: MySQL 8.0
- **Infrastructure**: Docker, Docker Compose

---

## 실행 방법

### 사전 요구사항

- Docker, Docker Compose 설치 필요
- 8080 포트 사용 가능 여부 확인

### 실행

```bash
docker compose up --build
```

애플리케이션이 시작되면 http://localhost:8080 에서 접근 가능합니다.

> DB 헬스체크 통과 후 앱이 기동되므로, 최초 실행 시 30초~1분 정도 소요될 수 있습니다.

### 환경 변수 (선택)

기본값으로 바로 실행되며, 필요 시 `.env` 파일로 재정의할 수 있습니다.

```env
MYSQL_DATABASE=everspin
MYSQL_USER=everspin
MYSQL_PASSWORD=everspin
MYSQL_ROOT_PASSWORD=everspin
```

### 초기 계정

애플리케이션 최초 실행 시 관리자 계정과 샘플 회의실이 자동으로 생성됩니다.

| 구분 | 아이디 | 비밀번호 |
|------|--------|----------|
| 관리자 | `admin` | `admin1234` |

일반 사용자는 `/auth/signup` 에서 직접 가입합니다.

### 종료 및 데이터 초기화

```bash
# 종료 (데이터 유지)
docker compose down

# 종료 + 데이터 삭제
docker compose down -v
```

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 회원가입 / 로그인 | Spring Security 폼 로그인 |
| 회의실 조회 | 전체 회의실 목록 및 상세 정보 확인 |
| 예약 신청 | 회의실 선택 후 날짜·시간·참석 인원 입력 |
| 예약 캘린더 | 월별 내 예약 현황 캘린더 뷰 |
| 예약 승인/반려 | 관리자가 대기 중인 예약을 처리 |
| 알림 | 예약 상태 변경 시 알림 표시 |
| 회의실 관리 | 관리자가 회의실 추가·활성화/비활성화 |

---

## 아키텍처

### 인프라 구조

```
┌─────────────────────────────────────────┐
│              Docker Compose             │
│                                         │
│  ┌──────────────┐    ┌───────────────┐  │
│  │  app:8080    │───▶│  db:3306      │  │
│  │  Spring Boot │    │  MySQL 8.0    │  │
│  └──────────────┘    └───────────────┘  │
│         ▲                  │            │
│         │            db-data (volume)   │
└─────────┼───────────────────────────────┘
          │
       Browser
```

앱 컨테이너는 DB 헬스체크 통과 후 기동하며, 멀티 스테이지 빌드로 JDK 없이 JRE만 포함한 경량 이미지를 생성합니다.

### 애플리케이션 레이어

```
Browser
  │  HTTP
  ▼
Controller  ──────────────────────────────────────────────┐
  │  (ReservationController, AdminController,             │
  │   RoomController, AuthController,                     │
  │   NotificationController)                             │
  │                                                       │
  ▼                                                       │
Service                                                   │
  │  (ReservationService, RoomService,                    │
  │   NotificationService, UserService)                   │
  │                                                       ▼
  ▼                                               Spring Security
Repository                                         (SecurityConfig)
  │  (JPA / JPQL)                                        │
  ▼                                                      │
MySQL                                             CustomUserDetails
                                                  CustomUserDetailsService
```

- **Controller**: 요청 수신, 응답 반환. 비즈니스 로직 없이 Service 호출만 담당
- **Service**: 트랜잭션 경계, 비즈니스 규칙 (중복 검증, 상태 전환, 알림 발송)
- **Repository**: Spring Data JPA. 복잡한 조회는 JPQL `@Query`로 작성
- **Security**: 폼 로그인, 쿠키 기반 CSRF, URL 패턴별 인가

### 도메인 모델

```
┌──────────┐       ┌───────────────────┐       ┌──────────┐
│  users   │       │   reservations    │       │  rooms   │
├──────────┤       ├───────────────────┤       ├──────────┤
│ id       │◀──┐   │ id                │   ┌──▶│ id       │
│ username │   └───│ user_id (FK)      │   │   │ name     │
│ password │       │ room_id (FK)  ────┘   │   │ location │
│ name     │       │ title                 │   │ capacity │
│ email    │       │ description           │   │ active   │
│ dept     │       │ start_time            │   └──────────┘
│ role     │       │ end_time              │
└──────────┘       │ attendees             │   ┌───────────────┐
                   │ status                │   │ notifications │
                   │ reject_reason         │   ├───────────────┤
                   └───────────────────────┘   │ id            │
                                               │ user_id (FK)  │
                                               │ message       │
                                               │ is_read       │
                                               │ created_at    │
                                               └───────────────┘
```

**예약 상태 흐름**

```
신청
  │
  ▼
PENDING ──▶ CONFIRMED (관리자 승인)
  │
  ├──▶ REJECTED  (관리자 반려, 사유 포함)
  │
  └──▶ CANCELLED (사용자 취소, 시작 전만 가능)
```

### 인증 / 인가

```
미인증 사용자  ──▶  /auth/login, /auth/signup, /rooms/** (공개)
                        │
                    폼 로그인
                        │
인증된 사용자  ──▶  /reservations/**, /api/** (ROLE_USER 이상)
                        │
관리자         ──▶  /admin/**                (ROLE_ADMIN 필요)
```

---

## 설계 내용 및 고민했던 부분

### 1. 예약 중복 방지 — 비관적 락(Pessimistic Lock)

동시에 같은 시간대에 예약 요청이 들어올 경우 둘 다 성공하는 문제를 방지해야 했습니다.

낙관적 락(Optimistic Lock)은 충돌이 발생한 뒤 예외를 던지고 재시도를 애플리케이션이 처리해야 하는 반면, 비관적 락은 `SELECT FOR UPDATE`로 DB 레벨에서 선점하여 후속 요청이 락 해제 전까지 대기합니다. 예약은 충돌 빈도가 낮고, 실패 시 재시도보다 즉각적인 오류 안내가 UX 측면에서 낫다고 판단하여 비관적 락을 선택했습니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId ...")
List<Reservation> findOverlappingReservationsWithLock(...);
```

### 2. CSRF 설정 — 쿠키 기반 토큰

Spring Security 기본 CSRF는 `HttpSessionCsrfTokenRepository`를 사용하는데, Thymeleaf가 응답 스트리밍을 시작한 후 세션을 생성하려 할 때 오류가 발생하는 문제가 있었습니다.

`CookieCsrfTokenRepository.withHttpOnlyFalse()`로 전환하여 토큰을 `XSRF-TOKEN` 쿠키에 저장함으로써 해결했습니다. REST API 엔드포인트(`/api/**`)는 인증된 사용자만 접근 가능하므로 CSRF 검증 대상에서 제외했습니다.

### 3. 예약 승인 워크플로 — PENDING 상태 도입

예약 즉시 확정하지 않고 관리자 승인 단계를 두었습니다. 상태는 `PENDING → CONFIRMED / REJECTED`, 또는 사용자가 직접 `CANCELLED`로 전환합니다. 이미 시작된 예약은 취소할 수 없도록 시간 검증을 추가했습니다.

### 4. 알림 시스템 — 폴링 방식

알림 구현 시 WebSocket(서버 푸시)과 폴링 중 어느 쪽을 선택할지 고민했습니다. WebSocket은 실시간성이 높지만 인프라 복잡도가 증가하고, 이 애플리케이션의 알림 특성상 수 초의 지연이 허용되므로 30초 간격 폴링을 선택했습니다. 드롭다운이 열려 있는 동안에는 폴링을 중단해 불필요한 요청을 줄였습니다.

### 5. N+1 문제 해결 — JOIN FETCH

`Reservation`은 `User`와 `Room`을 `LAZY`로 참조합니다. 목록 화면에서 `user.name`, `room.name`에 접근할 때마다 별도 쿼리가 발생하는 N+1 문제가 있었습니다.

조회 쿼리에 `JOIN FETCH`를 적용해 단일 쿼리로 해결했으며, 페이지네이션과 함께 사용할 때는 `countQuery`를 별도로 지정해 Hibernate 경고를 방지했습니다.

```java
@Query(value = "SELECT r FROM Reservation r JOIN FETCH r.user JOIN FETCH r.room WHERE r.status = :status ...",
       countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
Page<Reservation> findPendingWithDetails(...);
```

### 6. 상태별 카운트 — DB GROUP BY

캘린더 우측의 활동 현황에서 상태별 건수를 보여줄 때, 초기 구현은 사용자의 전체 예약을 Java로 불러와 `stream().collect(groupingBy(...))`로 집계했습니다. 예약이 많아질수록 낭비가 커지므로 DB `GROUP BY` 쿼리로 교체했습니다.

```sql
SELECT r.status, COUNT(r) FROM Reservation r WHERE r.user.id = :userId GROUP BY r.status
```

### 7. Thymeleaf 3.1 호환성

Thymeleaf 3.1부터 보안상의 이유로 `#request`, `#session` 등의 웹 컨텍스트 객체가 표현식에서 제거되었습니다. 활성 네비게이션 항목 표시에 `${#request.requestURI}`를 사용하고 있었는데, 이를 클라이언트 사이드 JS로 대체했습니다.

```javascript
const p = window.location.pathname;
document.querySelectorAll('.nav-item[href]').forEach(el => {
  if (p.startsWith(el.getAttribute('href'))) el.classList.add('active');
});
```

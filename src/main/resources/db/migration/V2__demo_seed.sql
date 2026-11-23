-- 시연용 초기 데이터.
--
-- 와장은 가입으로 만들어지지 않고 처음부터 DB 에 존재해야 하므로 여기서 넣는다.
-- 나머지는 프론트가 서버 없이 돌 때 쓰던 메모리 저장소와 같은 내용이라,
-- 서버를 붙여도 화면이 똑같이 보인다.
--
-- 모든 계정의 비밀번호는 'waggle1234' 다. BCrypt 해시로만 저장한다.
-- 일시는 UTC 로 적는다 (KST - 9시간).

SET @pw = '$2a$10$FQIyvcYDluX6SVPCzGY5oekCo6iLyM7RZJJK48kfxGBtq5hWj4qTW';
SET @t0 = '2026-09-01 01:00:00';

INSERT INTO seminars (id, title, description, capacity, apply_start_at, apply_end_at, total_grace_days, created_at, updated_at)
VALUES
  (1, 'Spring 세미나',
   '## 세미나 소개\n\nSpring Boot로 API 서버를 만들고 데이터베이스, 인증, 동시성, 배포까지 다룹니다. 매주 하나의 문제 상황에서 출발해, 그 문제를 해결하는 과정에서 필요한 개념을 익힙니다.\n\n1주차에는 HTTP와 REST API를 다루고, 2주차부터는 조별로 하나의 서비스를 만들며 데이터베이스 연동, 인증, 테스트, 배포를 차례로 붙여 나갑니다.\n\n## 참여 조건\n\nJava나 Kotlin 문법을 알고 있다면 누구나 참여할 수 있습니다. 백엔드 경험은 필요하지 않습니다.\n\n수업은 화요일 저녁 7시에 시작하며 한 회차는 두 시간입니다.',
   30, '2026-09-01 01:00:00', '2026-09-07 14:59:00', 3, @t0, @t0),
  (2, 'Frontend 세미나',
   '## 세미나 소개\n\nReact와 TypeScript로 웹 프론트엔드를 만듭니다. 컴포넌트 설계부터 상태 관리, 서버와의 통신, 배포까지 다룹니다.\n\n## 참여 조건\n\nHTML과 CSS를 다뤄 본 적이 있으면 충분합니다.',
   30, '2026-09-14 01:00:00', '2026-09-30 14:59:00', 3, @t0, @t0),
  (3, 'FastAPI 세미나',
   '## 세미나 소개\n\nPython으로 API 서버를 만들고 배포합니다. 비동기 처리와 데이터베이스 연동, 인증까지 직접 구현합니다.\n\n## 참여 조건\n\nPython 문법을 알고 있다면 누구나 참여할 수 있습니다.',
   25, '2026-09-14 01:00:00', '2026-09-30 14:59:00', 3, @t0, @t0),
  (4, 'iOS 세미나',
   '## 세미나 소개\n\nSwift와 SwiftUI로 앱을 만듭니다. 화면 구성과 데이터 흐름, 네트워크 통신을 배우고 앱 스토어 배포까지 해 봅니다.',
   2, '2026-09-14 01:00:00', '2026-09-30 14:59:00', 2, @t0, @t0),
  (5, 'Android 세미나',
   '## 세미나 소개\n\nKotlin으로 안드로이드 앱을 만듭니다. Jetpack Compose로 화면을 그리고 로컬 저장소와 API 연동을 익힙니다.',
   25, '2026-12-25 01:00:00', '2026-12-31 14:59:00', 3, @t0, @t0);

INSERT INTO users (id, email, password, name, github_username, role, status, seminar_id, created_at, updated_at)
VALUES
  (1, 'admin@wafflestudio.com', @pw, '와플장',   'waffle-admin',  'ADMIN',  'APPROVED', NULL, @t0, @t0),
  (2, 'staff@snu.ac.kr',        @pw, '김와플',   'waffle-kim',    'STAFF',  'APPROVED', 1,    @t0, @t0),
  (3, 'rookie@snu.ac.kr',       @pw, '오수현',   'suhyeon-oh',    'ROOKIE', 'APPROVED', NULL, @t0, @t0),
  (4, 'pending@snu.ac.kr',      @pw, '박대기',   'waiting-park',  'ROOKIE', 'PENDING',  NULL, @t0, @t0),
  (5, 'rejected@snu.ac.kr',     @pw, '이반려',   'rejected-lee',  'ROOKIE', 'REJECTED', NULL, @t0, @t0),
  (6, 'spring@snu.ac.kr',       @pw, '이스프링', 'spring-lee',    'ROOKIE', 'APPROVED', NULL, @t0, @t0),
  (7, 'jay@snu.ac.kr',          @pw, '박제이',   'jay-park',      'ROOKIE', 'APPROVED', NULL, @t0, @t0),
  (8, 'kotl@snu.ac.kr',         @pw, '최코틀',   'kotl-choi',     'ROOKIE', 'APPROVED', NULL, @t0, @t0),
  (9, 'newstaff@snu.ac.kr',     @pw, '정신입',   'new-jung',      'STAFF',  'PENDING',  2,    @t0, @t0);

INSERT INTO seminar_sessions (id, seminar_id, title, starts_at, location, assignment_title, lecture_content, assignment_content, created_at, updated_at)
VALUES
  (1, 1, 'Spring Boot와 REST API', '2026-09-08 10:00:00', '302동 208호', '모임 관리 API 완성하기',
   '## 문제 상황\n\n> 클라이언트가 사용할 수 있는 모임 관리 API 서버가 필요하다.\n\n## 세미나 핵심\n\n- 왜 웹 서버가 필요한가\n- HTTP 요청과 응답\n- Controller와 Service의 역할\n- DTO와 Validation\n\n## 실습\n\n```\nPOST /meetings\nGET  /meetings/{id}\n```',
   '## 과제 내용\n\n메모리 기반 모임 관리 API를 완성합니다.\n\n## 제출 조건\n\n- 목록 조회, 수정, 삭제를 직접 구현합니다\n- 공개 Acceptance Test를 모두 통과합니다\n\n## 제출 방법\n\n```\n./gradlew test\n```',
   @t0, @t0),
  (2, 1, 'Database, ERD, Swagger', '2026-09-15 10:00:00', '302동 208호', '세미나 서비스 ERD 설계',
   '## 문제 상황\n\n> 서버를 껐다 켜니 데이터가 모두 사라진다. 여러 명이 동시에 개발하려는데 서로 생각하는 데이터 구조와 API가 다르다.\n\n이번 시간에는 이 두 문제를 Database, ERD, Swagger로 해결합니다. ERD는 DB에 대한 팀의 계약, Swagger는 HTTP API에 대한 팀의 계약입니다.\n\n## 세미나 핵심\n\n- RDBMS가 필요한 이유와 SQL 기초\n- Spring Data JDBC와 Repository\n- DB 제약 조건과 Pagination\n- OpenAPI / Swagger\n\n## 개발 환경\n\nDB는 로컬에 직접 설치하지 않고 Docker로 실행합니다.\n\n```\ndocker compose up -d\n```',
   '## 과제 내용\n\n서비스 요구사항을 바탕으로 ERD를 설계하고 2주차 API를 구현합니다.\n\n## 제출 조건\n\n- ERD와 schema.sql은 6개 테이블 전체를 포함합니다\n- UNIQUE, FK, CHECK 제약을 빠짐없이 적습니다\n- 공개 Acceptance Test를 모두 통과합니다',
   @t0, @t0),
  (3, 1, '인증·인가와 예외 처리', '2026-09-22 10:00:00', '302동 309호', '로그인과 가입 심사',
   '## 문제 상황\n\n> 지금은 userId만 알면 다른 사람인 것처럼 요청할 수 있다.\n\n## 세미나 핵심\n\n- 인증과 인가\n- 로그인과 비밀번호 처리\n- 공통 예외 처리',
   NULL, @t0, @t0),
  (4, 1, '트랜잭션, 동시성, 테스트', '2026-09-29 10:00:00', '302동 309호', '선착순 신청 동시성 해결',
   NULL, NULL, @t0, @t0),
  (5, 2, 'React와 컴포넌트', '2026-09-10 10:00:00', '301동 101호', '첫 컴포넌트 만들기',
   '## 세미나 핵심\n\n- 컴포넌트와 props\n- 상태와 이벤트',
   '## 과제 내용\n\n버튼 컴포넌트를 만들고 상태를 붙여 봅니다.', @t0, @t0);

-- version 은 1부터 시작한다. 0 은 Spring Data JDBC 가 '아직 저장되지 않은 행'으로 읽는다.
INSERT INTO enrollments (id, seminar_id, rookie_id, grace_days_remaining, dropped, version, created_at, updated_at)
VALUES
  (1, 1, 3, 2, FALSE, 1, '2026-09-02 05:02:00', '2026-09-02 05:02:00'),
  (2, 1, 6, 3, FALSE, 1, '2026-09-02 05:05:00', '2026-09-02 05:05:00'),
  (3, 1, 7, 1, TRUE,  1, '2026-09-02 05:06:00', '2026-09-02 05:06:00'),
  (4, 1, 8, 3, FALSE, 1, '2026-09-03 00:00:00', '2026-09-03 00:00:00'),
  (5, 2, 3, 3, FALSE, 1, '2026-09-14 01:00:00', '2026-09-14 01:00:00'),
  (6, 4, 6, 2, FALSE, 1, '2026-09-14 01:00:00', '2026-09-14 01:00:00'),
  (7, 4, 7, 2, FALSE, 1, '2026-09-14 01:01:00', '2026-09-14 01:01:00');

INSERT INTO attendances (session_id, enrollment_id, status, created_at, updated_at)
VALUES
  (1, 1, 'PRESENT', @t0, @t0), (2, 1, 'ABSENT',  @t0, @t0), (3, 1, 'PRESENT', @t0, @t0),
  (1, 2, 'PRESENT', @t0, @t0), (2, 2, 'PRESENT', @t0, @t0), (3, 2, 'PRESENT', @t0, @t0),
  (1, 3, 'ABSENT',  @t0, @t0), (2, 3, 'ABSENT',  @t0, @t0),
  (1, 4, 'PRESENT', @t0, @t0), (2, 4, 'PRESENT', @t0, @t0), (3, 4, 'PRESENT', @t0, @t0);

INSERT INTO assignment_submissions (session_id, enrollment_id, status, created_at, updated_at)
VALUES
  (1, 1, 'PASSED', @t0, @t0), (2, 1, 'PASSED', @t0, @t0),
  (1, 2, 'PASSED', @t0, @t0), (2, 2, 'PASSED', @t0, @t0), (3, 2, 'NOT_SUBMITTED', @t0, @t0),
  (1, 3, 'NOT_SUBMITTED', @t0, @t0), (2, 3, 'NOT_SUBMITTED', @t0, @t0),
  (1, 4, 'PASSED', @t0, @t0), (2, 4, 'PASSED', @t0, @t0), (3, 4, 'PASSED', @t0, @t0);

-- Waggle 스키마.
--
-- seminars ─┬─< users              (운영진 1명은 세미나 1개를 담당, 세미나 1개에 운영진 여러 명)
--           ├─< seminar_sessions   (세미나 1개에 회차 여러 개)
--           └─< enrollments >─ users (루키와 세미나의 다대다. 수강에 딸린 값을 들고 있다)
--                  └─< attendances / assignment_submissions >─ seminar_sessions
--
-- 일시는 모두 UTC 기준 시점(Instant)으로 저장하고, 표시 시간대는 애플리케이션이 정한다.

CREATE TABLE seminars
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NULL COMMENT 'Markdown 소개',
    capacity         INT          NOT NULL COMMENT '신청할 수 있는 최대 인원',
    apply_start_at   DATETIME(6)  NOT NULL,
    apply_end_at     DATETIME(6)  NOT NULL,
    total_grace_days INT          NOT NULL COMMENT '수강생마다 이 값에서 시작한다',
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- 와장/운영진/루키를 한 테이블에 둔다. 한 사용자는 하나의 역할만 가지며 역할은 바뀌지 않는다.
CREATE TABLE users
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    github_username VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL COMMENT 'ADMIN | STAFF | ROOKIE',
    status          VARCHAR(20)  NOT NULL COMMENT 'PENDING | APPROVED | REJECTED',
    seminar_id      BIGINT       NULL COMMENT '운영진이 담당하는 세미나. 와장과 루키는 NULL',
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status_role (status, role),
    CONSTRAINT fk_users_seminar FOREIGN KEY (seminar_id) REFERENCES seminars (id)
) ENGINE = InnoDB;

-- 회차 번호는 저장하지 않는다. (seminar_id, starts_at, id) 정렬 순서가 곧 회차 번호다.
CREATE TABLE seminar_sessions
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    seminar_id         BIGINT       NOT NULL,
    title              VARCHAR(255) NOT NULL,
    starts_at          DATETIME(6)  NOT NULL,
    location           VARCHAR(255) NOT NULL,
    assignment_title   VARCHAR(255) NOT NULL COMMENT '회차마다 과제가 정확히 하나 있다',
    lecture_content    TEXT         NULL COMMENT 'Markdown 수업 설명',
    assignment_content TEXT         NULL COMMENT 'Markdown 과제 설명',
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_seminar_sessions_round (seminar_id, starts_at, id),
    CONSTRAINT fk_seminar_sessions_seminar FOREIGN KEY (seminar_id) REFERENCES seminars (id)
) ENGINE = InnoDB;

-- 잔여 Grace Day 와 탈락 여부는 루키가 아니라 "이 루키의 이 세미나 수강"에 대한 값이다.
-- 현재 신청 인원은 이 테이블을 세어서 구한다. 취소는 행을 지우므로 자리가 바로 빈다.
CREATE TABLE enrollments
(
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    seminar_id           BIGINT      NOT NULL,
    rookie_id            BIGINT      NOT NULL,
    grace_days_remaining INT         NOT NULL,
    dropped              BOOLEAN     NOT NULL DEFAULT FALSE COMMENT '표시용. 다른 기능에 영향을 주지 않는다',
    version              BIGINT      NOT NULL DEFAULT 0 COMMENT '낙관적 잠금. 운영진 여러 명이 같은 대시보드를 고친다',
    created_at           DATETIME(6) NOT NULL,
    updated_at           DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enrollments_seminar_rookie (seminar_id, rookie_id),
    KEY idx_enrollments_rookie (rookie_id),
    CONSTRAINT fk_enrollments_seminar FOREIGN KEY (seminar_id) REFERENCES seminars (id),
    CONSTRAINT fk_enrollments_rookie FOREIGN KEY (rookie_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- 출석과 과제 기록은 "어떤 회차의, 어떤 수강생"에 대한 기록이다. 한 칸에 하나씩만 존재한다.
-- 기록하지 않은 칸은 행이 없는 상태로 남는다. 결석이나 미제출로 간주하지 않는다.
CREATE TABLE attendances
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    session_id    BIGINT      NOT NULL,
    enrollment_id BIGINT      NOT NULL,
    status        VARCHAR(20) NOT NULL COMMENT 'PRESENT | ABSENT',
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendances_session_enrollment (session_id, enrollment_id),
    CONSTRAINT fk_attendances_session FOREIGN KEY (session_id) REFERENCES seminar_sessions (id),
    CONSTRAINT fk_attendances_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
) ENGINE = InnoDB;

CREATE TABLE assignment_submissions
(
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    session_id    BIGINT      NOT NULL,
    enrollment_id BIGINT      NOT NULL,
    status        VARCHAR(30) NOT NULL COMMENT 'PASSED | SUBMITTED_BUT_FAILED | NOT_SUBMITTED',
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_submissions_session_enrollment (session_id, enrollment_id),
    CONSTRAINT fk_submissions_session FOREIGN KEY (session_id) REFERENCES seminar_sessions (id),
    CONSTRAINT fk_submissions_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
) ENGINE = InnoDB;

-- V2 — cohort / session / announcement 테이블 (domain-entities.md §2~5, U2 FR-2/6)
-- status 는 VARCHAR + 애플리케이션 @Enumerated(EnumType.STRING) (V1 users 규약 상속).
-- 하위(회차·공지)는 코호트 하드 삭제 시 CASCADE, User 는 이력 보존 위해 RESTRICT
-- (cid:application-design:c2). 파일럿은 하드 삭제 대신 종료됨 상태 전이를 우선한다.

CREATE TABLE cohort (
    id            BIGSERIAL     PRIMARY KEY,
    mentor_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    title         VARCHAR(200)  NOT NULL,
    description   TEXT,
    capacity      INT           NOT NULL CHECK (capacity >= 1),
    start_date    DATE          NOT NULL,
    end_date      DATE          NOT NULL,
    session_count INT           NOT NULL CHECK (session_count >= 1),
    status        VARCHAR(20)   NOT NULL DEFAULT 'RECRUITING',
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);

-- performance-design.md §2 / scalability-design.md §2 — 목록·소유자·정렬 조회 인덱스
CREATE INDEX ix_cohort_status ON cohort (status);
CREATE INDEX ix_cohort_mentor_id ON cohort (mentor_id);
CREATE INDEX ix_cohort_created_at ON cohort (created_at);

CREATE TABLE session (
    id        BIGSERIAL   PRIMARY KEY,
    cohort_id BIGINT      NOT NULL REFERENCES cohort (id) ON DELETE CASCADE,
    seq       INT         NOT NULL CHECK (seq >= 1),
    status    VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    -- INV-U2-2 — 한 코호트의 회차 순번은 유일. (cohort_id, seq) UNIQUE 인덱스가
    -- performance-design.md §2 의 session(cohort_id, seq) 조회 인덱스도 겸한다.
    CONSTRAINT ux_session_cohort_seq UNIQUE (cohort_id, seq)
);

CREATE TABLE announcement (
    id            BIGSERIAL     PRIMARY KEY,
    cohort_id     BIGINT        NOT NULL REFERENCES cohort (id) ON DELETE CASCADE,
    body          TEXT          NOT NULL,
    external_link VARCHAR(2048),
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);

-- performance-design.md §3 — 공지 목록/최근 공지 조회(cohort_id + createdAt desc)
CREATE INDEX ix_announcement_cohort_created ON announcement (cohort_id, created_at);

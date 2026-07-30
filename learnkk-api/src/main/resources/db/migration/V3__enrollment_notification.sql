-- V3 — enrollment / notification 테이블 (domain-entities.md §2~4, U3 FR-3/4/10)
-- status/type 은 VARCHAR + 애플리케이션 @Enumerated(EnumType.STRING) (V1 users·V2 규약 상속).
-- 하위(참여·알림)는 코호트/사용자 하드 삭제 시 정책이 다르다:
--   enrollment.cohort_id  → cohort(id)  ON DELETE CASCADE  (코호트 삭제 시 참여 제거)
--   enrollment.mentee_id  → users(id)   ON DELETE RESTRICT (참여 이력 있는 사용자 삭제 방지)
--   notification.user_id  → users(id)   ON DELETE CASCADE  (사용자 삭제 시 알림 제거)
-- (cid:application-design:c2). 파일럿은 하드 삭제 대신 종료됨 상태 전이를 우선한다.

CREATE TABLE enrollment (
    id         BIGSERIAL   PRIMARY KEY,
    cohort_id  BIGINT      NOT NULL REFERENCES cohort (id) ON DELETE CASCADE,
    mentee_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    decided_at TIMESTAMP,
    -- @Version 낙관적 락 컬럼(domain-entities.md §2). 관리자 승인/거절의 1차 방어선은
    -- 상태 가드 조건부 UPDATE 이며, version 은 보조 방어선이다(nfr-design reliability §2).
    version    BIGINT      NOT NULL DEFAULT 0,
    -- INV-U3-2 / R-U3-08 — 한 사용자는 한 코호트에 하나의 Enrollment 만.
    -- 선착순 동시 이중 제출의 최종 방어선(비관적 락을 우회하는 어떤 경로든 차단).
    CONSTRAINT ux_enrollment_cohort_mentee UNIQUE (cohort_id, mentee_id)
);

-- performance-design.md §3 / scalability-design.md §2 — 확정 인원 집계·대기 목록 조회
CREATE INDEX ix_enrollment_cohort_status ON enrollment (cohort_id, status);
-- 내 신청 목록(mentee 스코프, 최신순)
CREATE INDEX ix_enrollment_mentee ON enrollment (mentee_id);

CREATE TABLE notification (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(40)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

-- performance-design.md §3 — 알림 목록(user 스코프 + 안읽음 + 최신순)
CREATE INDEX ix_notification_user_read_created ON notification (user_id, is_read, created_at);

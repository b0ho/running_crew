-- V5 — final_report / certificate / settlement_status 테이블 (domain-entities.md §2~4, U5 FR-7/8/9)
-- status/type 규약은 V1~V4 상속. 종료 오케스트레이션(U5)이 수료 판정+수료증 발급+정산 판정+상태 전이+알림을
-- 단일 트랜잭션으로 수행한다(INV-U5-3). 하드 삭제 정책은 V3/V4 와 동일:
--   final_report.cohort_id     → cohort(id) ON DELETE CASCADE  (코호트 삭제 시 보고서 제거)
--   final_report.author_id     → users(id)  ON DELETE RESTRICT (보고서 이력 있는 사용자 삭제 방지)
--   certificate.cohort_id      → cohort(id) ON DELETE CASCADE  (코호트 삭제 시 수료증 제거)
--   certificate.mentee_id      → users(id)  ON DELETE RESTRICT (수료증 보유 사용자 삭제 방지)
--   settlement_status.cohort_id→ cohort(id) ON DELETE CASCADE  (코호트 삭제 시 정산 상태 제거)
--   settlement_status.mentor_id→ users(id)  ON DELETE RESTRICT
-- (cid:application-design:c2). 파일럿은 하드 삭제 대신 종료됨 상태 전이를 우선한다.
-- 컬럼명은 엔티티 필드 snake_case 와 정합: FinalReport.submittedAt ↔ submitted_at,
-- Certificate.imagePath ↔ image_path, SettlementStatus.evaluatedAt ↔ evaluated_at.

-- 최종 보고서 (FinalReport, US-11 / FR-7)
CREATE TABLE final_report (
    id           BIGSERIAL     PRIMARY KEY,
    cohort_id    BIGINT        NOT NULL REFERENCES cohort (id) ON DELETE CASCADE,
    author_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    body         TEXT          NOT NULL,
    file_path    VARCHAR(512),
    submitted_at TIMESTAMP     NOT NULL DEFAULT now()
);

-- performance-design.md §3 — 보고서 이력 페이지네이션(cohort_id + submittedAt desc)
CREATE INDEX ix_final_report_cohort_submitted
    ON final_report (cohort_id, submitted_at);

-- 수료증 (Certificate, US-12 / FR-8)
CREATE TABLE certificate (
    id         BIGSERIAL     PRIMARY KEY,
    cohort_id  BIGINT        NOT NULL REFERENCES cohort (id) ON DELETE CASCADE,
    mentee_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    image_path VARCHAR(512)  NOT NULL,
    issued_at  TIMESTAMP     NOT NULL DEFAULT now(),
    -- INV-U5-1 — 코호트별 멘티당 수료증 1장(중복/재발급 방지). 재종료 시 사전조회 skip 의 최종 방어선.
    CONSTRAINT ux_certificate_cohort_mentee UNIQUE (cohort_id, mentee_id)
);

-- 정산 상태 (SettlementStatus, US-13 / FR-9)
CREATE TABLE settlement_status (
    id           BIGSERIAL   PRIMARY KEY,
    -- INV-U5-2 — 코호트당 정산 상태 1건(1:1). upsert 의 최종 방어선.
    cohort_id    BIGINT      NOT NULL UNIQUE REFERENCES cohort (id) ON DELETE CASCADE,
    mentor_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    satisfied    BOOLEAN     NOT NULL,
    evaluated_at TIMESTAMP   NOT NULL DEFAULT now()
);

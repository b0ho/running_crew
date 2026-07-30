-- V4 — attendance_evidence 테이블 (domain-entities.md §2, U4 FR-5, INV-U4-1)
-- status/enum 규약은 V1~V3 상속. 증빙은 회차(session) 단위로 멘토가 업로드하며,
-- 업로드 성공 시 동일 트랜잭션에서 회차가 인증(VERIFIED)된다(INV-U4-1).
--   attendance_evidence.session_id   → session(id) ON DELETE CASCADE  (회차 삭제 시 증빙 제거)
--   attendance_evidence.uploaded_by  → users(id)   ON DELETE RESTRICT (증빙 이력 있는 사용자 삭제 방지, R-U4-15)
-- (cid:application-design:c2). 파일럿은 하드 삭제 대신 종료됨 상태 전이를 우선한다.

CREATE TABLE attendance_evidence (
    id          BIGSERIAL     PRIMARY KEY,
    session_id  BIGINT        NOT NULL REFERENCES session (id) ON DELETE CASCADE,
    file_path   VARCHAR(512)  NOT NULL,
    mime_type   VARCHAR(100)  NOT NULL,
    size        BIGINT        NOT NULL CHECK (size >= 0),
    uploaded_by BIGINT        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at  TIMESTAMP     NOT NULL DEFAULT now()
);

-- performance-design.md §3 — 증빙 이력·존재 확인 조회(session_id + createdAt desc)
CREATE INDEX ix_attendance_evidence_session_created
    ON attendance_evidence (session_id, created_at);

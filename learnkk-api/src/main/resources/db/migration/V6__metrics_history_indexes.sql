-- V6 — U6 admin-metrics 보조 인덱스 (performance-design.md §2, code-generation-plan Step 1)
-- U6 는 읽기 전용 리포팅/조회 유닛으로 신규 테이블을 만들지 않는다(INV-U6-1).
-- 증빙/보고서 이력의 '전역 최신순' 조회(cohortId 필터 없이 전체)를 뒷받침하는 보조 인덱스만 추가한다.
-- 기존 인덱스는 (session_id, created_at) / (cohort_id, submitted_at) 로 코호트 스코프 조회를 커버하지만,
-- 전역 정렬(created_at desc / submitted_at desc)에는 선두 컬럼 인덱스가 필요하다.

-- 증빙 이력 전역 최신순 정렬 (HistoryService.evidenceHistory, cohortId=null 경로)
CREATE INDEX ix_attendance_evidence_created
    ON attendance_evidence (created_at);

-- 보고서 이력 전역 최신순 정렬 (HistoryService.reportHistory, cohortId=null 경로)
CREATE INDEX ix_final_report_submitted
    ON final_report (submitted_at);

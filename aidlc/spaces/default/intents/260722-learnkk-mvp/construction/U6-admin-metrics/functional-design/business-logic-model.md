# Business Logic Model — U6 admin-metrics (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U6 책임), `unit-of-work-story-map.md`(US-14/15), `requirements-analysis/requirements.md`(FR-10/11), `application-design/components.md`(소스 엔티티), `component-methods.md`(MetricsService.overview, HistoryService), `services.md`(MetricsService·HistoryService)
> 범위: 운영 지표 집계·증빙/보고서 이력 조회(관리자). 읽기 전용.

## 1. U6 워크플로 목록

| # | 워크플로 | 스토리 | 서비스 메서드 |
|---|---|---|---|
| W-U6-1 | 운영 지표 집계 조회 | US-14 | MetricsService.overview |
| W-U6-2 | 증빙 이력 조회(관리자) | US-15 | HistoryService.evidenceHistory |
| W-U6-3 | 보고서 이력 조회(관리자) | US-15 | HistoryService.reportHistory |

## 2. W-U6-1 운영 지표 집계 (MetricsService.overview)

`overview(): MetricsOverviewDto`. 관리자만(R-U6-01). 조회 시점 실제 데이터에서 계산(INV-U6-2).

절차:
1. 권한 확인(ROLE_ADMIN, 아니면 403).
2. 집계 범위: **종료됨(CLOSED) 코호트**(R-U6-04/05 범위 일관).
3. 산식(business-rules R-U6-04~07):
   - completedCohortCount = count(Cohort where status=종료됨) (R-U6-06)
   - attendanceRate = Σ(인증 회차) / Σ(전체 회차) over 종료됨 코호트, 분모 0 → 0 (R-U6-04)
   - completionRate = 발급 증서 수 / 종료됨 코호트 확정 멘티 총수, 분모 0 → 0 (R-U6-05)
   - certificateCount = count(Certificate) (R-U6-07)
4. MetricsOverviewDto(집계 범위 "종료된 코호트 N건 기준" 포함) 반환.

- **데이터 획득 방식(F2 명확화 — 리포팅 읽기 모델)**: U6은 파일럿에서 **read-only 리포팅 읽기 모델**로 구현한다. 즉 U6의 `MetricsRepository`(읽기 전용 JPQL/native 집계 쿼리)가 공유 스키마의 소스 테이블을 **읽기만** 하여 집계한다(쓰기 절대 없음, INV-U6-1). 이는 다른 서비스의 도메인 로직을 우회하지 않는 순수 조회이며, 파일럿 규모(<100명)에서 실시간 계산으로 충분(캐시 없음, INV-U6-2). 구체 집계 쿼리:
  - `completedCohortCount`: `SELECT COUNT(*) FROM cohort WHERE status='CLOSED'`
  - `attendanceRate`: `SELECT COALESCE(SUM(verified),0), COALESCE(SUM(total),0) FROM (코호트별 인증/전체 회차 집계 over status='CLOSED')` → verified*100.0/total, total=0이면 0
  - `completionRate`: `certificateCount / (SELECT COUNT(*) FROM enrollment e JOIN cohort c ON e.cohort_id=c.id WHERE c.status='CLOSED' AND e.status='CONFIRMED')`, 분모 0이면 0
  - `certificateCount`: `SELECT COUNT(*) FROM certificate`
  - 대안(팀 선택 가능): 위 쿼리 대신 소스 유닛의 read API(U2 종료 코호트·회차, U3 confirmedCount, U5 증서 수)를 조합해도 결과 동일. 파일럿 기본은 리포팅 읽기 모델. 어느 경로든 **쓰기 없음**.

```
overview()
  ├─ 비관리자? ─> 403
  └─ 종료됨 코호트 집합 S
       ├─ completedCohortCount = |S|
       ├─ attendanceRate = sum(verified in S)/sum(total in S)  (분모0→0)
       ├─ completionRate = certCount(S)/confirmedMentees(S)     (분모0→0)
       └─ certificateCount = count(Certificate)
     -> MetricsOverviewDto
```
<!-- Text fallback: overview는 관리자만 접근하며, 종료된 코호트 집합을 기준으로 완주 코스 수, 출석률(인증/전체 회차 합), 수료율(증서 수/확정 멘티 수), 발급 증서 수를 계산해 반환한다. 분모가 0이면 0%로 안전 처리한다. -->

## 3. W-U6-2 증빙 이력 조회 (HistoryService.evidenceHistory)

**시그니처(F1 명확화)**: `evidenceHistory(cohortId: Long?, page: int, size: int): Page<EvidenceHistoryItemDto>`. `cohortId`는 선택 필터(null이면 전체), page/size 페이지네이션(기본 size=20). 관리자만(R-U6-01).
- HistoryService는 U6 소유 서비스다. component-methods.md는 services.md에 HistoryService 존재만 선언했으므로 본 유닛이 메서드 시그니처를 확정한다.
- 구현: U4 AttendanceEvidence를 Session·Cohort·User(업로더 성명) 조인한 read-only 쿼리. 최신순(createdAt desc), 20건 페이지(R-U6-09).
- 파일 다운로드는 U1 `FileStorageService.load` 링크(R-U6-11).

## 4. W-U6-3 보고서 이력 조회 (HistoryService.reportHistory)

**시그니처(F1 명확화)**: `reportHistory(cohortId: Long?, page: int, size: int): Page<ReportHistoryItemDto>`. cohortId 선택 필터, 페이지네이션(기본 size=20). 관리자만.
- 구현: U5 FinalReport를 Cohort·User(작성자 성명) 조인, 첨부 유무(filePath != null) 계산한 read-only 쿼리. 최신순(submittedAt desc), 20건(R-U6-10).
- 증빙 이력과 **별도 뷰**(R-U6-08, FR-10 분리 조회 수용기준).

## 5. 크로스유닛 통합 계약 (U6가 요구)

| 방향 | 계약 | 상태 |
|---|---|---|
| U6 → U2 (읽기) | 종료됨 코호트·회차 수 조회 | U2 제공 |
| U6 → U3 (읽기) | `confirmedCount`/`confirmedEnrollments` | U3 제공 |
| U6 → U4 (읽기) | 증빙 이력 목록(조인) | U4 제공 |
| U6 → U5 (읽기) | 증서 수·수료 데이터·보고서 이력 | U5 제공 |

- U6은 말단 소비자: 어떤 유닛도 U6을 호출하지 않으며 U6은 아무것도 쓰지 않는다(INV-U6-1).

## 6. 프론트엔드 연동

U6는 UI 포함 → 상세는 `frontend-components.md`. 요약: AdminPage의 지표 탭(MetricsOverview 카드), 증빙 이력 탭, 보고서 이력 탭(각각 별도). 모든 호출 U1 ApiClient 경유, 관리자 권한.

## 7. 데이터 흐름 요약

```
U6(MetricsService/HistoryService)  [읽기 전용]
  --읽음--> U2(코호트/회차), U3(확정 멘티), U4(증빙 이력), U5(증서/보고서)
  (U6을 호출하는 유닛 없음; U6은 아무것도 쓰지 않음)
```
<!-- Text fallback: U6은 읽기 전용으로 U2 코호트/회차, U3 확정 멘티, U4 증빙 이력, U5 증서/보고서 데이터를 읽어 지표와 이력을 산출한다. 다른 유닛이 U6을 호출하지 않으며 U6은 쓰기를 하지 않는다. -->

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->

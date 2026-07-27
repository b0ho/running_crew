# Business Logic Model — U5 completion (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U5-completion
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U5 책임·소유권), `unit-of-work-story-map.md`(US-4 종료/US-11/12/13), `requirements-analysis/requirements.md`(FR-7/8/9), `application-design/components.md`(FinalReport·Certificate·SettlementStatus), `component-methods.md`(CompletionService.evaluateOnEnd/certificateOf, ReportService.submit/historyOf), `services.md`(CompletionService·ReportService)
> 범위: 코호트 종료 오케스트레이션·수료 판정/수료증·정산 판정·최종 보고서·종료 요약.

## 1. U5 워크플로 목록

| # | 워크플로 | 스토리 | 서비스 메서드 |
|---|---|---|---|
| W-U5-1 | 코호트 종료 오케스트레이션 | US-4(종료) | CompletionService.evaluateOnEnd (endCohort) |
| W-U5-2 | 최종 보고서 제출 | US-11 | ReportService.submit |
| W-U5-3 | 보고서 이력 조회 | US-11/15 | ReportService.historyOf |
| W-U5-4 | 수료증 조회 | US-12 | CompletionService.certificateOf |
| W-U5-5 | 종료 요약 조회 | US-4(종료) | (endCohort 결과 CohortEndSummaryDto) |

## 2. W-U5-1 코호트 종료 오케스트레이션 (핵심 — endCohort/evaluateOnEnd)

`endCohort(mentorId, cohortId): CohortEndSummaryDto`. 사용자 대면 종료 엔드포인트는 **U5가 노출**(U2 §7 계약). 단일 `@Transactional`.

절차:
1. **사전 검증**: Cohort 조회(없으면 404). 소유 멘토 확인(아니면 403, R-U5-01). status==진행중 확인(아니면 409 INVALID_STATE_TRANSITION, R-U5-02).
2. **회차 집계**(U2 읽기): totalSessions = 전체 회차 수, verifiedSessions = status==인증 회차 수(U4가 markVerified로 전이한 결과). totalSessions==0이면 정합 오류 500(R-U5-10).
3. **확정 멘티 조회**(U3 읽기): 해당 코호트 CONFIRMED Enrollment 멘티 목록.
4. **수료 판정+수료증**(R-U5-05~08): 코호트 출석률 판정 `verifiedSessions*100 >= totalSessions*80`.
   - 충족: 확정 멘티 전원 수료(R-U5-07 회차 단위 균일). **확정 멘티 목록은 U3 `EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>`(§5 계약)로 조회**(count가 아닌 목록이 필요). 각 멘티에 대해 순서대로:
     - a. 수료증 이미지 생성(템플릿 렌더링 → PNG, 멘티 성명·코호트명·발급일 임베드). 생성 주체는 CompletionService의 인증서 렌더러(구현 라이브러리는 code-generation에서 선택).
     - b. U1 `FileStorageService.store(image)` 호출 → imagePath 획득. **생성한 imagePath를 롤백 보상용 리스트에 누적**.
     - c. Certificate insert(cohortId·menteeId·imagePath, UNIQUE(cohortId,menteeId)로 재발급 방지 — 이미 있으면 skip).
   - 미충족: 수료증 미발급.
5. **정산 판정**(R-U5-11~14): 멘토 최종 보고서 존재 여부는 **U5 자체 `ReportService.mentorReportExists(cohortId, mentorId): boolean`**(`EXISTS(FinalReport WHERE cohortId=? AND authorId=cohort.mentorId)`)로 조회. 조건 `verifiedSessions == totalSessions AND mentorReportExists` → SettlementStatus.satisfied. 코호트당 1건 upsert(UNIQUE(cohortId): 존재 시 update, 없으면 insert).
6. **상태 전이**: U2 Cohort.status 세터로 종료됨 전이(R-U5-03, INV-U5-4).
7. **알림**: 각 확정 멘티에 U3 `NotificationService.notify`(COMPLETION_RESULT, 수료/미수료 메시지, R-U5-20).
8. 커밋 → CohortEndSummaryDto(수료자 수·미수료 수·정산 충족 여부·발급 증서 수) 반환.

- **원자성(INV-U5-3)**: 1~7이 하나의 트랜잭션. 부분 실패 시 전체 롤백(수료증만 발급되고 상태 전이 실패 같은 불일치 방지).
- **수료증 이미지 고아 파일 보상(R-U5-08a, 루프 레벨)**: 수료증 이미지 store(비트랜잭션 I/O)는 각 멘티마다 발생하며 생성된 imagePath를 리스트에 누적한다. 트랜잭션이 롤백되면 **누적한 모든 imagePath에 대해 U1 `FileStorageService.delete`를 호출**해 고아 이미지를 정리한다(일부 delete 실패 시 해당 경로 ERROR 로그). 5명 중 5번째에서 실패해도 앞선 4개 이미지가 정리된다. U4 R-U4-13과 동일 패턴의 다건 확장.

결정 트리:
```
endCohort(mentorId, cohortId)
  ├─ 코호트 없음? ─> 404
  ├─ 소유 멘토 아님? ─> 403
  ├─ status != 진행중? ─> 409 INVALID_STATE_TRANSITION
  └─ [TX]
       ├─ totalSessions==0? ─> 500 정합 오류(롤백)
       ├─ verified*100 >= total*80 ? ─ yes ─> 확정 멘티 전원 Certificate 발급
       │                              └ no  ─> 수료증 미발급
       ├─ verified==total && 멘토보고서제출? ─> Settlement.satisfied=true / else false
       ├─ U2.setStatus(종료됨)
       ├─ 각 확정 멘티 U3.notify(COMPLETION_RESULT)
       └─ commit -> CohortEndSummaryDto
```
<!-- Text fallback: endCohort는 코호트 없음 404, 비소유 403, 진행중 아님 409를 거른 뒤 하나의 트랜잭션에서 회차 수를 집계하고, 출석률 80% 이상이면 확정 멘티 전원에게 수료증을 발급, 정산 조건(전 회차 인증+멘토 보고서)을 판정, U2 세터로 종료됨 전이, 확정 멘티에게 결과 알림을 생성한 뒤 종료 요약을 반환한다. 부분 실패는 전체 롤백된다. -->

## 3. W-U5-2 최종 보고서 제출 (ReportService.submit)

`submit(userId, cohortId, body, file?): ReportDto`.
- body 필수 검증(R-U5-15). 참여자(멘토/멘티) 확인.
- 첨부 있으면: U1 `FileStorageService.store` → [TX: FinalReport 저장] → 롤백 시 `delete` 보상(R-U5-16/17, U4 R-U4-13과 동일 패턴). 첨부 없으면 순수 DB 트랜잭션.
- 상태 "제출됨". ReportDto 반환.

## 4. W-U5-3/4 조회

- historyOf(cohortId): 보고서 이력. 참여자·관리자만(R-U5-19). U6 관리자 이력 조회(US-15)가 이 데이터를 사용.
- certificateOf(menteeId, cohortId): 수료증 조회/다운로드(U1 load). 본인·관리자.

## 5. 크로스유닛 통합 계약 (U5 요구/제공)

| 방향 | 계약 | 상태 |
|---|---|---|
| U5 → U2 (읽기) | Cohort·Session(전체/인증 회차 수) 조회 | U2 제공 |
| U5 → U2 (호출) | Cohort.status 세터(종료됨) | U2 제공(U2 §8) |
| U5 → U3 (읽기) | **`EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>`** — 확정 멘티 **목록**(count 아님, 수료증 발급 대상 순회용) | **U3가 노출해야 할 계약**(component-methods.md는 join/myApplications만 선언 → U3 functional-design에 추가 요구) |
| U5 → U3 (호출) | `NotificationService.notify` (수료 결과) | U3 제공 |
| U5 → U1 (호출) | `FileStorageService.store/load/delete` (보고서 첨부·수료증 이미지) | U1 제공 |
| U6 → U5 (읽기) | 수료율·증서 수·보고서 이력 | U5 제공 |

- U5는 종료·판정의 단일 소유자이며 U2/U3/U4 데이터를 읽는다. U2/U3/U4는 U5를 호출하지 않는다(단방향, 순환 없음 — `cid:units-generation:c2`).

## 6. 프론트엔드 연동

U5는 UI 포함 → 상세는 `frontend-components.md`. 요약: EndCohortDialog(멘토 종료 액션), ReportForm(보고서 제출, 보고서 탭), CompletionResult(수료/미수료·수료증 다운로드), CohortEndSummary(종료 요약). 모든 호출 U1 ApiClient 경유.

## 7. 데이터 흐름 요약

```
U5(FinalReport/Certificate/SettlementStatus)
  --읽음--> U2(Cohort/Session), U3(확정 멘티), U4(인증 회차 상태)
  --호출--> U2(status 세터), U3(notify), U1(FileStorage)
  --제공(read)--> U6(수료율·증서·보고서 이력)
U2/U3/U4 --호출 안 함--> U5
```
<!-- Text fallback: U5는 U2 코호트/회차·U3 확정 멘티·U4 인증 회차를 읽어 종료 판정을 수행하고, U2 상태 세터·U3 알림·U1 파일 저장을 호출한다. 수료율·증서·보고서 이력을 U6에 read-only로 제공한다. U2/U3/U4는 U5를 호출하지 않는다. -->

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent
**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect를 가정하고 반증 시도)을 통과함. 상위 스테이지 산출물과의 정합성, 크로스유닛 계약, 규칙/불변식 참조, 예외→HTTP 매핑, 그리고 해당 유닛의 핵심 설계(동시성 락·트랜잭션 원자성·파일 보상·수료 산식·집계 정확성 등)가 검증됨. 파일럿 보류 항목은 명시적으로 스코프아웃되어 있고, 개발자가 본 산출물만으로 구현 가능함이 확인됨. 1차 지적사항이 있었던 경우 반복 리뷰에서 모두 해소 후 READY.

<!-- 주: 리뷰어가 최초 작성한 상세 리뷰는 영어였으며, team.md Mandated(모든 산출물 한글) 규칙 준수를 위해 한글 요약으로 대체함. 판정(READY)과 근거 요지는 보존. 상세 findings 이력은 audit trail(SUBAGENT_COMPLETED) 및 산출물 개정 이력에 남아 있음. -->

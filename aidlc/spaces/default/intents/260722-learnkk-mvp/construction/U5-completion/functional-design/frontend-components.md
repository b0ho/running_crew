# Frontend Components — U5 completion (LearnKK 파일럿)

> Construction · functional-design 단계 산출물(조건부 UI) · 유닛 U5-completion
> 리드 architect (서포트 developer)
> 상위 입력: `application-design/components.md`(S4b 보고서·S5 종료·S7/S8 수료·종료요약), `requirements-analysis/requirements.md`(FR-7/8/9, NFR-3), `unit-of-work-story-map.md`(US-4 종료/US-11/12/13), `component-methods.md`(CompletionService·ReportService)
> 규약: U1 공통 셸 + U2 CohortDetailPage 탭 재사용. team.md React 규약, Tailwind 경량 커스텀.

## 1. 컴포넌트 계층 (U5 추가분)

```
CohortDetailPage (U2)
 ├─ Tabs > "보고서" 탭 (U5 소유)
 │   ├─ ReportForm (body + 선택 첨부)
 │   └─ ReportList (제출 이력, 참여자·관리자)
 ├─ (멘토 전용) EndCohortButton -> EndCohortDialog (종료 확인)
 └─ CohortEndSummary (종료 후: 수료자 수·정산 충족·발급 증서 수)
MyPage / CohortDetail
 └─ CompletionResult (수료/미수료 배너 + 수료증 다운로드)
```
<!-- Text fallback: U2 코호트 상세에 U5의 보고서 탭(보고서 폼+이력), 멘토 전용 종료 버튼→종료 확인 다이얼로그, 종료 후 종료 요약을 둔다. 멘티는 수료 결과 배너와 수료증 다운로드(CompletionResult)를 본다. -->

## 2. 컴포넌트별 설계

### 2.1 EndCohortButton / EndCohortDialog (US-4 종료, 멘토 전용)
- 진행중 코호트의 소유 멘토에게만 노출(서버 R-U5-01/02가 최종 방어).
- 다이얼로그: "코호트를 종료하면 수료·정산 판정이 확정되고 되돌릴 수 없습니다. 진행할까요?" 확인 시 `POST /api/cohorts/:id/end`.
- 성공 시 CohortEndSummary 표시(수료자 N/전체 M, 정산 조건 충족 여부, 발급 증서 수). 실패: 403(비소유), 409("진행중 코호트만 종료 가능").

### 2.2 ReportForm / ReportList (US-11 / FR-7)
- ReportForm: body(필수 자유서식), 선택 파일 첨부(FileDropzone, U1 제약). `POST /api/cohorts/:id/reports`(multipart).
- 첨부 실패: 400 FILE_CONSTRAINT_VIOLATION. 제출 성공 시 상태 "제출됨", ReportList 갱신.
- ReportList: 참여자·관리자 조회(R-U5-19). 첨부는 다운로드 링크(U1 load).

### 2.3 CompletionResult (US-12, 멘티)
- 종료 후 멘티에게 수료/미수료 표시. 수료: "수료를 축하합니다" + 수료증 다운로드 버튼(`GET /api/cohorts/:id/certificate`). 미수료: "미수료 — 출석률 부족" 안내(R-U5-09).
- 알림(NotificationBell, U3)으로도 결과 통지되며 이 화면에서 상세 확인.

### 2.4 CohortEndSummary (US-4 종료 결과)
- 종료 직후 멘토에게 요약: 수료자 수/전체 확정 멘티 수, 정산 조건 충족 여부("정산 조건 충족" 메시지 또는 미충족), 발급 증서 수.

## 3. API 통합 지점 (U5)

| 액션 | 호출 | 메서드(BE) |
|---|---|---|
| 코호트 종료 | `POST /api/cohorts/:id/end` | CompletionService.evaluateOnEnd (endCohort) |
| 보고서 제출 | `POST /api/cohorts/:id/reports` (multipart) | ReportService.submit |
| 보고서 이력 | `GET /api/cohorts/:id/reports` | ReportService.historyOf |
| 수료증 조회 | `GET /api/cohorts/:id/certificate` | CompletionService.certificateOf |

- 종료 엔드포인트는 U2가 아니라 U5가 노출(오케스트레이션 소유). 모든 호출 U1 ApiClient 경유.

## 4. 접근성·상태 처리

- EndCohortDialog는 되돌릴 수 없음을 명확히 고지, 확인 버튼 포커스 트랩·키보드 접근.
- 수료/미수료 배너는 색+텍스트 병기. 수료증 다운로드는 대체 텍스트 제공.
- 보고서 제출 중 버튼 비활성화·중복 제출 방지.

# Frontend Components — U4 attendance (LearnKK 파일럿)

> Construction · functional-design 단계 산출물(조건부 UI) · 유닛 U4-attendance
> 리드 architect (서포트 developer)
> 상위 입력: `application-design/components.md`(S4 코호트 상세 진도·출석 탭·FileUpload), `requirements-analysis/requirements.md`(FR-5, NFR-3), `unit-of-work-story-map.md`(US-9/10), `component-methods.md`(AttendanceService·FileStorageService)
> 규약: U1 공통 셸 + U2 CohortDetailPage 탭 구조 재사용. team.md React 규약, Tailwind 경량 커스텀.

## 1. 컴포넌트 계층 (U4 추가분 — U2 코호트 상세의 "진도·출석" 탭)

```
CohortDetailPage (U2) > Tabs > "진도·출석" 탭 (U4 소유)
 ├─ ProgressSummary (인증 회차 수 / 전체 회차 수, 진도율 바)
 ├─ SessionAttendanceList
 │   └─ SessionRow (seq, status 배지 예정/인증, 증빙 미리보기 링크)
 └─ (멘토 전용) SessionEvidenceUpload
     ├─ FileDropzone (jpg/png/pdf, ≤10MB 클라이언트 사전 체크)
     └─ UploadResultToast
```
<!-- Text fallback: U2 코호트 상세의 진도·출석 탭에 진도 요약(인증/전체 회차, 진도율), 회차별 출석 목록(순번·상태 배지·증빙 미리보기), 그리고 멘토 전용 회차 증빙 업로드(파일 드롭존+결과 토스트)를 둔다. -->

## 2. 컴포넌트별 설계

### 2.1 ProgressSummary (US-10)
- 데이터: `GET /api/cohorts/:id/attendance`(sessionsOf) 응답의 인증 수·전체 수·진도율.
- 진도율 바는 색+수치 병기(접근성).

### 2.2 SessionAttendanceList / SessionRow (US-10)
- 회차별 seq·status 배지(예정/인증). 인증 회차는 증빙 미리보기 링크 노출.
- 증빙 미리보기/다운로드 → `GET /api/sessions/:id/evidence/:evidenceId`(FileStorageService.load 경유). 참여자·관리자만(R-U4-11).

### 2.3 SessionEvidenceUpload (멘토 전용, US-9 / FR-5)
- 멘토에게만 노출(currentUser가 코호트 멘토일 때). 서버 권한(R-U4-01)이 최종 방어.
- FileDropzone: 클라이언트 사전 검증(확장자 jpg/png/pdf, 크기 ≤10MB) — UX 보조. 서버 검증(R-U4-02/03, U1 FileStorage)이 권위.
- 업로드 → `POST /api/sessions/:id/evidence`(multipart). 성공 시:
  - 회차 상태 배지가 즉시 "인증"으로 갱신(서버가 markVerified 수행).
  - UploadResultToast "회차 출석이 인증되었습니다".
- 실패 처리: 400 FILE_CONSTRAINT_VIOLATION("허용되지 않는 파일 형식/크기입니다"), 403(비소유 멘토), 409 COHORT_CLOSED("종료된 코호트입니다").

## 3. API 통합 지점 (U4)

| 액션 | 호출 | 메서드(BE) |
|---|---|---|
| 진도·출석 조회 | `GET /api/cohorts/:id/attendance` | AttendanceService.sessionsOf |
| 증빙 업로드 | `POST /api/sessions/:id/evidence` (multipart) | AttendanceService.uploadEvidence |
| 증빙 다운로드 | `GET /api/sessions/:id/evidence/:evidenceId` | FileStorageService.load(경유) |

- 업로드는 multipart/form-data, ApiClient가 세션 쿠키·에러 정규화 처리. 대용량 업로드 진행률 표시(선택).

## 4. 접근성·상태 처리

- 파일 업로드는 키보드 접근 가능한 대체 input 제공(드롭존 단독 금지).
- status 배지 색+텍스트 병기. 업로드 결과 Toast는 aria-live.
- 업로드 중 버튼/드롭존 비활성화, 진행 상태 표시.

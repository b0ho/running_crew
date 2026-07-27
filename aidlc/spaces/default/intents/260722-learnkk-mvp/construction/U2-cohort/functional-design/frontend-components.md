# Frontend Components — U2 cohort (LearnKK 파일럿)

> Construction · functional-design 단계 산출물(조건부 UI) · 유닛 U2-cohort
> 리드 architect (서포트 developer)
> 상위 입력: `application-design/components.md`(S2 대시보드·S4 상세·S5 개설/종료 화면), `requirements-analysis/requirements.md`(FR-2/6, NFR-3), `unit-of-work-story-map.md`(US-3/4/5), `component-methods.md`(CohortService·AnnouncementService)
> 규약: U1이 확립한 AuthProvider/ApiClient/RequireAuth/ResponsiveTabBar 공통 셸 재사용. team.md React 규약, Tailwind 경량 커스텀(`cid:refined-mockups:c1`).

## 1. 컴포넌트 계층 (U2 추가분)

```
(공통 셸: App/AuthProvider/ApiClient/RequireAuth/ResponsiveTabBar — U1 소유)
 ├─ DashboardPage (라우트 /, 내 코호트 대시보드 — 로그인 후 첫 화면)
 │   ├─ CohortCard (내가 개설/참여한 코호트 요약)
 │   └─ EmptyState
 ├─ CohortFormPage (개설/수정 — 라우트 /cohorts/new, /cohorts/:id/edit)
 │   ├─ CohortForm (title·description·capacity·기간·sessionCount)
 │   └─ CapacityWarningBanner (정원 축소 경고)
 ├─ CohortDetailPage (라우트 /cohorts/:id)
 │   ├─ Tabs(공지 | 진도·출석 | 멤버 | 보고서)  ← 공지 탭·회차 목록은 U2, 나머지 탭은 상위 유닛
 │   ├─ SessionList (회차 목록, status 배지)
 │   └─ AnnouncementList / AnnouncementForm
 └─ (멤버 탭 간단 목록 — cid:refined-mockups:c1)
```
<!-- Text fallback: U1 공통 셸 위에 DashboardPage(코호트 카드), CohortFormPage(개설/수정 폼 + 정원 축소 경고), CohortDetailPage(공지/진도출석/멤버/보고서 탭, 회차 목록, 공지 폼)를 추가한다. 공지 탭과 회차 목록은 U2 소유이며 진도·출석/보고서 탭은 상위 유닛이 채운다. -->

## 2. 컴포넌트별 설계

### 2.1 DashboardPage (US-3, NFR-3 첫 화면)
- 데이터: 내가 멘토인 코호트 + 내가 확정 멘티인 코호트(확정 참여는 U3 데이터, 조회만).
- CohortCard: 제목·상태 배지(모집중/진행중/종료됨)·기간·정원 요약. 클릭 시 상세로.
- EmptyState: 참여/개설 코호트가 없을 때 "코호트를 탐색해보세요"(탐색 화면은 U3).

### 2.2 CohortForm (US-3 개설 / US-4 수정)
- 필드: title(필수·≤200), description(선택), capacity(정수≥1), startDate/endDate(endDate≥startDate), sessionCount(정수≥1).
- 개설: `POST /api/cohorts` → 성공 시 상세로 이동.
- 수정: `PUT /api/cohorts/:id`. capacity 축소 응답이 경고를 포함하면 CapacityWarningBanner 노출. 확정 인원 미만 축소(409 CAPACITY_BELOW_CONFIRMED)·종료됨(409 COHORT_CLOSED)·인증 회차 축소(409 SESSION_VERIFIED_LOCK)는 인라인 에러로 표시.
- 클라이언트 검증은 UX 보조, 서버 검증(business-rules R-U2-01~10)이 권위.

### 2.3 CohortDetailPage (US-3/4/5)
- Tabs: 공지(U2), 진도·출석(U4), 멤버(간단 목록), 보고서(U5). U2는 공지 탭·회차 목록·기본정보를 렌더.
- SessionList: 회차 seq·status(예정/인증) 배지. 인증 전이는 U4가 수행하나 표시는 여기서.
- 멘토에게만 공지 작성(AnnouncementForm)·수정 진입점 노출(권한은 서버 R-U2-15가 최종 판정).

### 2.4 AnnouncementForm / AnnouncementList (US-5)
- Form: body(필수), externalLink(선택·URL). `POST /api/cohorts/:id/announcements`.
- List: 최신순 공지. externalLink는 새 탭 링크(rel="noopener").

## 3. API 통합 지점 (U2)

| 액션 | 호출 | 메서드(BE) |
|---|---|---|
| 코호트 개설 | `POST /api/cohorts` | CohortService.create |
| 코호트 수정 | `PUT /api/cohorts/:id` | CohortService.update |
| 목록·탐색 | `GET /api/cohorts?filter` | CohortService.list/search |
| 상세 | `GET /api/cohorts/:id` | CohortService.get |
| 공지 작성 | `POST /api/cohorts/:id/announcements` | AnnouncementService.create |
| 공지 목록 | `GET /api/cohorts/:id/announcements` | AnnouncementService.list |

- 모든 호출 ApiClient 경유(세션 쿠키·에러 정규화). 권한 없는 작업 버튼은 UI에서 숨기되 서버 인가(R-U2-07/15)가 최종 방어.

## 4. 접근성·상태 처리

- 폼 라벨 연결·`aria-describedby` 에러, 제출 중 버튼 비활성화·중복 제출 방지.
- 상태 배지는 색+텍스트 병기(색만으로 의미 전달 금지 — 접근성).
- 반응형: 상세 탭은 모바일에서 스와이프/드롭다운 대체, 데스크톱 상단 탭(NFR-3).

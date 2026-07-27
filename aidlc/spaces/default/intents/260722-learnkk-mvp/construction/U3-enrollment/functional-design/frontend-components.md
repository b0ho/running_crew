# Frontend Components — U3 enrollment (LearnKK 파일럿)

> Construction · functional-design 단계 산출물(조건부 UI) · 유닛 U3-enrollment
> 리드 architect (서포트 developer)
> 상위 입력: `application-design/components.md`(S3 탐색/참여·S3b 신청상태·S6 관리자 대기승인·NotificationBell), `requirements-analysis/requirements.md`(FR-3/4/10, NFR-3), `unit-of-work-story-map.md`(US-6a/6b/7/8), `component-methods.md`(EnrollmentService·AdminApprovalService·NotificationService)
> 규약: U1 공통 셸(AuthProvider/ApiClient/RequireAuth/ResponsiveTabBar) 재사용. team.md React 규약, Tailwind 경량 커스텀.

## 1. 컴포넌트 계층 (U3 추가분)

```
(공통 셸 — U1)
 ├─ ExplorePage (라우트 /explore — 코호트 탐색·참여)
 │   ├─ CohortCard (U2 재사용) + JoinButton
 │   └─ Toast (확정/대기 결과 안내)
 ├─ MyApplicationsPage (라우트 /my/applications — 신청 상태)
 │   └─ ApplicationRow + StatusBadge(대기중/확정/거절)
 ├─ NotificationBell (공통 헤더 위젯 — 안읽은 수 배지, 드롭다운 목록)
 └─ AdminPage > 대기승인 탭 (라우트 /admin, 관리자 전용)
     └─ WaitingList + Approve/Reject 액션
```
<!-- Text fallback: U1 공통 셸 위에 ExplorePage(코호트 카드+참여 버튼, 결과 토스트), MyApplicationsPage(신청 상태 행+배지), NotificationBell(헤더 알림), AdminPage의 대기승인 탭(대기 목록+승인/거절)을 추가한다. -->

## 2. 컴포넌트별 설계

### 2.1 ExplorePage / JoinButton (US-6a/6b)
- 데이터: 모집중/진행중 코호트 목록(U2 조회 API). 각 카드에 정원·확정 인원 요약.
- JoinButton → `POST /api/cohorts/:id/enrollments`(join). 응답:
  - 201 확정: Toast "참여가 확정되었습니다".
  - 201 대기중: Toast "정원이 마감되어 대기 신청되었습니다".
  - 409 ALREADY_ENROLLED: "이미 신청한 코호트입니다".
  - 409 SELF_ENROLLMENT: "본인이 개설한 코호트에는 참여할 수 없습니다".
  - 409 COHORT_NOT_OPEN: "참여할 수 없는 코호트입니다".
- **이중 클릭 방지**: 제출 중 버튼 비활성화(서버 UNIQUE 제약이 최종 방어이나 UX 차원 방지).

### 2.2 MyApplicationsPage / StatusBadge (US-7 / FR-4)
- 데이터: `GET /api/me/enrollments`(myApplications). 각 행: 코호트 제목·상태 배지(대기중/확정/거절)·신청일.
- 상태 변경(승인/거절)은 NotificationBell 알림으로 통지되며 이 페이지 재조회 시 반영.

### 2.3 NotificationBell (US-7)
- 데이터: `GET /api/me/notifications`(listFor). 안읽은 수 배지.
- 드롭다운에서 항목 클릭 시 `POST /api/me/notifications/:id/read`(markRead).
- 폴링(파일럿: 단순 주기 폴링 또는 페이지 진입 시 조회. 실시간 푸시는 범위 외).

### 2.4 AdminPage 대기승인 탭 (US-8 / FR-10)
- 관리자 전용(currentUser.isAdmin 확인 렌더; 서버 @PreAuthorize가 최종 방어 — R-U3-11).
- 데이터: `GET /api/admin/enrollments/waiting`(listWaiting). 
- 각 행 Approve/Reject 버튼 → `POST /api/admin/enrollments/:id/approve` | `/reject`. 성공 시 목록에서 제거, 멘티에게 알림 자동 생성.
- 정원 초과 승인 가능(R-U3-13) — UI는 초과 승인 시 확인 다이얼로그 표시.

## 3. API 통합 지점 (U3)

| 액션 | 호출 | 메서드(BE) |
|---|---|---|
| 참여 신청 | `POST /api/cohorts/:id/enrollments` | EnrollmentService.join |
| 내 신청 목록 | `GET /api/me/enrollments` | EnrollmentService.myApplications |
| 대기 목록(관리자) | `GET /api/admin/enrollments/waiting` | AdminApprovalService.listWaiting |
| 승인/거절 | `POST /api/admin/enrollments/:id/approve|reject` | AdminApprovalService.approve/reject |
| 알림 목록 | `GET /api/me/notifications` | NotificationService.listFor |
| 알림 읽음 | `POST /api/me/notifications/:id/read` | NotificationService.markRead |

- 모든 호출 ApiClient 경유(세션·에러 정규화). 관리자 액션 버튼은 UI에서 숨기되 서버 인가가 최종 방어.

## 4. 접근성·상태 처리

- StatusBadge는 색+텍스트 병기(색만으로 상태 전달 금지).
- JoinButton 제출 중 비활성화·중복 제출 방지, 결과 Toast는 aria-live 영역으로 스크린리더 전달.
- 대기승인 목록의 Approve/Reject는 확인 절차 및 처리 중 비활성화.

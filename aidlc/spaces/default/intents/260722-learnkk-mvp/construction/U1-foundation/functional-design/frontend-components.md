# Frontend Components — U1 foundation (LearnKK 파일럿)

> Construction · functional-design 단계 산출물(조건부 UI) · 유닛 U1-foundation
> 리드 architect (서포트 developer)
> 상위 입력: `application-design/components.md`(S1 화면·FE 컴포넌트), `requirements-analysis/requirements.md`(FR-1, NFR-3), `unit-of-work-story-map.md`(US-1/2), `component-methods.md`(AuthService 계약), `services.md`(AuthService)
> 근거: U1은 kind=service이나 "배포 실행체 백엔드 + **해당 UI 포함**"(unit-of-work.md). 인증 UI는 워킹 스켈레톤 관통 경로의 필수 구성요소이므로 U1 범위에서 설계한다.
> 코드 규약: team.md React 규약(Named export, 중앙 API 클라이언트 에러 정규화, PascalCase 컴포넌트/useXxx 훅), UI는 Tailwind 경량 커스텀(`cid:refined-mockups:c1`).

## 1. 컴포넌트 계층 (U1 auth UI)

```
App
 ├─ ApiClient (중앙 래퍼: 에러 정규화, credentials:include)
 ├─ AuthProvider (인증 컨텍스트: currentUser, login, logout, signup)
 ├─ AuthPage (라우트: /auth)
 │   ├─ LoginForm
 │   └─ SignupForm
 ├─ RequireAuth (보호 라우트 가드)
 └─ ResponsiveTabBar (TopTabBar 데스크톱 / BottomTabBar 모바일 — 공통 셸)
```

<!-- Text fallback: App 아래 중앙 ApiClient와 AuthProvider가 있고, AuthPage가 LoginForm/SignupForm을 품는다. RequireAuth가 보호 라우트를 감싸고, ResponsiveTabBar가 데스크톱 상단탭/모바일 하단탭 공통 셸을 제공한다. -->

> 상위 유닛(U2~U6)의 화면(대시보드·탐색·상세 등)은 각 유닛에서 설계한다. U1은 인증 UI + 공통 셸(AuthProvider/ApiClient/RequireAuth/ResponsiveTabBar)만 확립한다.

## 2. 컴포넌트별 설계

### 2.1 ApiClient (공통)
- 책임: 모든 HTTP 호출의 단일 진입점. `credentials: 'include'`로 세션 쿠키 전송(FE/BE 분리 저장소 → CORS). 응답 에러를 공통 에러 DTO(`{code,message,timestamp,path}`)로 정규화해 throw.
- 노출 훅/함수: `useApi()` 또는 모듈 함수. 컴포넌트 산발 try-catch 지양(team.md).

### 2.2 AuthProvider (공통)
- state: `currentUser: UserDto | null`, `status: 'loading'|'authed'|'anon'`.
- 액션: `signup(req)`, `login(email,password)`, `logout()`.
- 부팅 시 세션 확인(예: `GET /api/auth/me`)으로 currentUser 복원.

### 2.3 LoginForm (US-2)
- props: 없음(AuthProvider 컨텍스트 사용).
- 필드/검증(클라이언트): email(형식), password(비어있지 않음). 서버 검증이 최종 권위.
- 제출: `login(email,password)` → 성공 시 **내 코호트 대시보드**로 이동(NFR-3, R-U1-11). 실패 시 401 → "이메일 또는 비밀번호가 올바르지 않습니다"(사용자 열거 방지, 미존재·불일치 동일 문구 — R-U1-09).

### 2.4 SignupForm (US-1)
- 필드: email·name·nickname·password(+ 확인). **isAdmin 필드 없음**(R-U1-06 — DTO 스키마에 부재).
- 클라이언트 검증: email 형식, name(≤100)·nickname(≤50) 비어있지 않음, password ≥8자.
- 제출: `signup(req)` → 성공(201) 시 로그인 유도 또는 자동 로그인 후 대시보드. 실패: 400 VALIDATION_ERROR(필드별 메시지), 409 DUPLICATE_EMAIL("이미 사용 중인 이메일입니다").

### 2.5 RequireAuth (공통 가드)
- 미인증(status='anon')으로 보호 라우트 접근 시 `/auth`로 리다이렉트(R-U1-13 대응 FE 처리).
- 관리자 전용 화면은 `currentUser.isAdmin` 확인 후 렌더(서버가 최종 권위 — 403 방어는 백엔드 R-U1-16a).

### 2.6 ResponsiveTabBar (공통 셸)
- 반응형: 데스크톱 상단 탭 바(TopTabBar), 모바일 하단 탭 바(BottomTabBar)로 전환(NFR-3, `cid:rough-mockups:c1`). 좌측 사이드바 미사용.
- U1에서는 셸 골격만; 탭 항목은 유닛 추가에 따라 확장.

## 3. API 통합 지점 (U1)

| 화면/액션 | 호출 | 메서드(BE) | 성공 후 |
|---|---|---|---|
| 회원가입 | `POST /api/auth/signup` | AuthService.signup | 로그인/대시보드 |
| 로그인 | `POST /api/auth/login` | AuthService.login(세션 수립) | 내 코호트 대시보드 |
| 세션 복원 | `GET /api/auth/me` | 현재 사용자 조회 | currentUser 세팅 |
| 로그아웃 | `POST /api/auth/logout` | 세션 무효화 | /auth |

- 모든 호출은 ApiClient 경유(`credentials:include`). 에러는 공통 에러 DTO로 정규화되어 폼이 필드/토스트로 표시.

## 4. 폼 검증 규칙 요약 (business-rules 대응)

| 필드 | 클라이언트 검증 | 서버 최종 규칙 |
|---|---|---|
| email | 형식·필수 | R-U1-01 형식, R-U1-02 정규화·유일 |
| name | 필수·≤100 | R-U1-03 |
| nickname | 필수·≤50 | R-U1-03 |
| password | 필수·≥8 | R-U1-04, R-U1-05(BCrypt 저장) |

> 클라이언트 검증은 UX 보조일 뿐이며 서버 검증(business-rules)이 권위다. 보안 판정(BCrypt·인가)은 전적으로 백엔드 책임.

## 5. 접근성·상태 처리

- 폼 필드는 라벨 연결(`label htmlFor`)과 에러 메시지 `aria-describedby` 연결(접근성).
- 로딩/제출중 상태에서 버튼 비활성화 및 중복 제출 방지.
- 에러는 필드 하단 인라인 + 전역 실패는 토스트로 표시(에러 정규화 경유).

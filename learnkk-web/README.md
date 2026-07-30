# learnkk-web

LearnKK 프론트 — Vite + React + TypeScript + Tailwind. 인증 UI(로그인/가입) + 공통 셸(AuthProvider·ApiClient·RequireAuth·ResponsiveTabBar).

## 요구 사항

- Node 20+

## 스크립트

```bash
npm install
npm run dev        # Vite 개발 서버(:5173, /api 는 localhost:8080 프록시)
npm run build      # tsc 타입체크 + 프로덕션 빌드
npm run lint       # ESLint (경고 0 게이트)
npm test           # Jest + React Testing Library
```

## 구조

```
src/
  api/         ApiClient(에러 정규화, credentials:include) · authApi · types
  auth/        AuthProvider · authContext(useAuth) · LoginForm · SignupForm · AuthPage · RequireAuth
  shell/       ResponsiveTabBar (데스크톱 상단 / 모바일 하단)
  pages/       DashboardPage (로그인 후 목적지 플레이스홀더)
```

## 규약 (team.md React)

- Named export 기본. 중앙 API 클라이언트에서 에러 정규화(컴포넌트 산발 try-catch 지양).
- 컴포넌트 `PascalCase`, 훅 `useXxx`. 폼 요소에 `data-testid` 부여, 라벨·`aria-describedby` 접근성 연결.
- 로그인 실패는 미존재/불일치 동일 문구(사용자 열거 방지, R-U1-09).

## API 연동

- 동일 오리진 프록시(`/api`)를 기본으로 하며(nginx/Vite), 세션 쿠키는 `credentials:'include'` 로 전송.
- 런타임 오버라이드가 필요하면 `window.__LEARNKK_API_BASE__` 를 설정한다.

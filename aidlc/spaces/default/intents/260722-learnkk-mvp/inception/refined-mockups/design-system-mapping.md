# Design System Mapping — LearnKK (파일럿)

> Inception · refined-mockups 단계 산출물 · 리드 design
> 근거: refined-mockups-questions.md (Q1=Tailwind 경량 커스텀, Q3=미니멀·중립)
> 상위 입력: `mockups.md`, `practices-discovery/team-practices.md`(React+ESLint+named exports)

## 기반

- **Tailwind CSS + 소형 커스텀 컴포넌트**(기성 UI 라이브러리 미도입, Q1=A). 파일럿에 가볍고 반응형 유틸리티 우수.

## 디자인 토큰 (예시)

| 토큰 | 값(예시) | 용도 |
|---|---|---|
| color.bg | 밝은 화이트/그레이-50 | 배경 |
| color.text | 그레이-900/600 | 본문/보조 |
| color.accent | 인디고-600 | 강조·주요 버튼 |
| color.success | 그린-600 | 확정·인증·수료 |
| color.warning | 앰버-600 | 마감·대기 |
| color.danger | 레드-600 | 거절·오류·미수료·파괴적 액션 |
| color.neutral | 그레이-400 | **미인증(예정)** 회차·비활성 상태 |
| radius | 8px | 카드·버튼 |
| spacing | 4px 배수 | 간격 스케일 |

> 상태색은 항상 텍스트 라벨과 병기(색맹 접근성).

## 공용 컴포넌트

- **Button**(primary/secondary/danger, 로딩·disabled 상태)
- **Card**(코호트 카드: 제목·메타·CTA)
- **Tabs**(코호트 상세·관리자)
- **StatusBadge**(모집중/마감/대기중/확정/거절/인증/수료 — 색+텍스트)
- **FileUpload**(형식/크기 검증, 진행률, 오류 인라인)
- **Toast / InlineError / EmptyState / ConfirmDialog**
- **BottomTabBar(모바일) / TopTabBar(데스크톱)** 반응형 전환

## 화면 ↔ 컴포넌트 매핑 (요약)

| 화면 | 주요 컴포넌트 |
|---|---|
| S2 대시보드 | Card, EmptyState, TabBar |
| S3 탐색/참여 | Card, StatusBadge, Button, Toast |
| S4 코호트 상세 | Tabs, StatusBadge, FileUpload, 멤버 목록 |
| S5 개설/종료 | Form 필드(회차 수 포함), ConfirmDialog |
| S6 관리자 | Tabs, Button(승인/거절), 지표 Card, EmptyState |
| S7 수료 결과 | Card, StatusBadge, 이미지 미리보기, Button(다운로드) |

## 규약 (team-practices 정합)

- React 컴포넌트 named export, feature 폴더 co-locate.
- 접근성: 시맨틱 태그, 라벨 연결, 포커스 링 유지(accessibility-checklist 참조).

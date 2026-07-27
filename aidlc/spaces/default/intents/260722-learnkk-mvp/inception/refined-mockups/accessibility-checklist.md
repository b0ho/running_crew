# Accessibility Checklist — LearnKK (파일럿)

> Inception · refined-mockups 단계 산출물 · 리드 design
> 상위 입력: `mockups.md`, `interaction-spec.md`, `ideation/rough-mockups/wireframes.md`(접근성 원칙)
> 파일럿 수준의 실용 체크리스트. 완전한 WCAG 준수는 보조기술 수동 검증 + 전문가 검토가 필요함을 명시.

## 지각 가능(Perceivable)

- [ ] 상태(모집중/마감/대기/확정/거절/인증/수료)는 **색상만이 아니라 텍스트**로도 표기.
- [ ] 텍스트 대비 최소 WCAG AA(본문 4.5:1) 목표.
- [ ] 이미지(수료증·증빙 미리보기)에 대체 텍스트.

## 운용 가능(Operable)

- [ ] 모든 인터랙티브 요소 **키보드 접근·포커스 순서** 논리적.
- [ ] 포커스 표시(focus ring) 유지.
- [ ] 파괴적 액션(코호트 종료) 확인 다이얼로그, 키보드로 취소 가능.

## 이해 가능(Understandable)

- [ ] 폼 라벨-입력 연결(label for/aria-label).
- [ ] 오류 메시지는 원인+해결 안내(예: "jpg/png/pdf, 10MB 이하").
- [ ] 일관된 내비게이션(탭 위치·라벨).

## 견고성(Robust)

- [ ] 시맨틱 HTML(button/nav/main/헤딩 계층).
- [ ] 동적 상태 변화에 aria-live **적용**: 토스트/알림은 `aria-live="polite"`, 오류·업로드 실패는 `aria-live="assertive"`. 확인 다이얼로그(코호트 종료)는 role="dialog" + 포커스 트랩. (잔여 리스크: 스크린리더 실제 낭독은 수동 검증 필요)

## 반응형

- [ ] 데스크톱 상단 탭 ↔ 모바일 하단 탭 전환 시 포커스·순서 유지.
- [ ] 터치 타깃 최소 크기 확보.

## 검증 방법 & 한계

- 자동 도구(axe 등)로 1차 점검 + 키보드-only 수동 통과.
- **완전한 WCAG 준수 판정은 보조기술(스크린리더) 수동 테스트와 전문가 검토가 필요**하며, 파일럿에서는 위 핵심 항목 우선 적용.

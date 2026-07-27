# User Stories Assessment — LearnKK (파일럿)

> Inception · user-stories 단계 산출물 · mob(product 리드 + design·developer·quality) 결과 종합
> 상위 입력: `requirements-analysis/requirements.md`, `stories.md`, `personas.md`

## Mob 프로세스 요약

- Round 1: 리드(product) 초안 → design·developer·quality 병렬 블라인드 검토(각 기여 파일 작성) → 리드 통합.
- 트리아지: 제기된 OBJECTION·RECOMMENDATION은 상호 충돌이 아닌 완결성 보강이라 라운드 2·인간 판단 없이 리드가 전부 통합.

## INVEST 평가

| 기준 | 평가 |
|---|---|
| Independent | US-6를 6a/6b로 분할해 의존 축소; US-0(RBAC)을 선행으로 명시 |
| Negotiable | 파일럿 범위로 협상 여지 유지(멤버 탭 등 이월) |
| Valuable | 각 스토리가 역할별 가치에 직접 연결 |
| Estimable | 회차 1급화·판정 시점 명시로 추정 가능성 향상 |
| Small | 6a/6b 분할, walking-skeleton 슬라이스 정의로 크기 축소 |
| Testable | 산식·경계·negative 케이스 AC로 검증 가능성 확보 |

## 반영된 참여자 기여

**developer (구현 가능성)**
- [반영] US-0 역할 모델·관리자 시드 추가(OBJECTION 해소).
- [반영] US-6 → US-6a(선착순)/US-6b(대기·동시성) 분할, 6a를 스켈레톤에 편입.
- [반영] 회차 1급 엔티티(US-3 생성/US-10 조회), 판정 트리거 시점(US-12/13), 파일 저장 위치·멘토 자동 멤버십.

**design (UX)**
- [반영] 접근성 AC(상태 색상+텍스트, 키보드) 공통 AC로 추가(OBJECTION 해소).
- [반영] US-4/US-10 AC 추가(OBJECTION 해소).
- [반영] 빈 상태(US-2/US-5/US-14), 알림 조회(US-7), 대시보드 요약(US-2/US-10), 관측 가능 피드백(US-2/US-6a).
- [이월] 멤버 탭·화면5 회차 필드 정합 → refined-mockups.

**quality (테스트 가능성)**
- [반영] US-12 출석 산식·반올림·79/80 경계, US-9 형식/크기 거부 분리, US-13 부분충족 미표시, US-6 순차 경계·중복 재신청.
- [반영] US-14 지표 집계 정의 명시(OBJECTION 해소).
- [반영] 테스트 우선순위 메모(핵심 도메인 집중 vs CRUD 스모크).
- [반영] 대기 취소→승격/거절 후 재신청은 파일럿 범위 외 명시.

## 잔여/이월

- refined-mockups: 화면5 회차 필드 반영, 멤버 탭 노출 여부 확정.
- functional-design: 최종 보고서를 멘토/멘티 모두 제출하는지 재확인, 파일 저장 상세.

## 커버리지

Must 백로그 B-M1~B-M8 + RBAC 선행(US-0)까지 스토리로 전개 완료. requirements FR-1~FR-11 전부 매핑(stories.md 매핑표 참조).

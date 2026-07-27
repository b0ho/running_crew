<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T18:00:00Z — 멘토/멘티를 컨텍스트 역할(개설=멘토, 참여=멘티)로, 관리자는 명시적 시드 역할로 결정(US-0). mob developer OBJECTION 반영.
- 2026-07-22T18:00:00Z — 코호트 상태(모집중→진행중→종료됨) 도입, 멘토 "코호트 종료" 액션이 수료·정산 판정 및 완주 집계 트리거로 확정.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->
- 2026-07-22T18:00:00Z — US-6를 6a(선착순)/6b(대기·동시성)로 분할. INVEST Independent 완화(의도된 분할 의존)로 스켈레톤 슬라이스 확보.

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-07-22T18:00:00Z — 대기 취소·자동 승격·거절 후 재신청을 파일럿 범위 외로 명시 제외(범위 좁게 유지 원칙).

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T18:00:00Z — 최종 보고서 멘토/멘티 모두 제출 여부, 파일 저장 상세는 functional-design에서 확정.
- 2026-07-22T18:00:00Z — rough-mockups 화면5 회차 필드 불일치·멤버 탭 노출은 refined-mockups에서 정합.

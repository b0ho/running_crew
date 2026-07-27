<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T17:00:00Z — 수료 기준(멘티, 출석 80%)과 정산 기준(멘토, 전 회차 인증+보고서)이 서로 다르게 나옴(Q1=B vs Q2=A). 두 기준을 분리 요구사항(FR-8 vs FR-9)으로 정의.
- 2026-07-22T17:00:00Z — 코호트를 회차(세션) 단위 구조로 확정(Q6=A). 출석·증빙이 회차 기반.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T17:00:00Z — 최종 보고서가 수료 조건과 무관(Q1=B)해졌으나 정산 조건에는 필요(Q2=A). 멘토/멘티 모두 제출하는지 functional-design에서 재확인.

<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T20:00:00Z — 도메인을 10개 엔티티 + 12개 서비스로 분해. 요구(FR-1~11)/스토리(US-0~15) 전부 매핑. 아키텍처 리뷰어 NOT-READY(RBAC 시드 ADR·테스트 전략 ADR·엔티티 FK) 반영해 ADR-8/9 + 엔티티 관계표 추가.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-07-22T20:00:00Z — 코호트 삭제 정책: 하드 삭제 대신 "종료됨" 상태 전이 우선(하위 CASCADE, User는 RESTRICT).

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T20:00:00Z — RBAC 시드 이메일/초기 비번은 환경설정 주입으로 code-generation에서 구체화.

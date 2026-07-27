<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T16:00:00Z — greenfield이므로 hub-and-spoke를 org.md 기본값 + project.md 결정 기반으로 진행. 리드 초안 → 3 스포크(quality/developer/devsecops) 기여 → 인터뷰 16문항 → 리드 통합.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->
- 2026-07-22T16:00:00Z — 사용자가 FE/BE 분리 저장소(Q1=B)를 선택, developer 스포크의 monorepo 권고와 다름. 트레이드오프(API 계약 조율 오버헤드)를 evidence에 기록.

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-07-22T16:00:00Z — 보안 위생(스캔·업로드 검증·TLS)을 파일럿에서 보류(Q14~16=B). 출시 속도 우선; BCrypt 해싱만 하드 제약. 확장 시 재검토 필수.

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T16:00:00Z — 파일 업로드 검증 부재는 출석 증빙(신뢰 경계 밖 입력)에 잔여 보안 리스크. 확장 전 검증 계층 추가 필요.
- 2026-07-22T16:00:00Z — 분리 저장소에서 OpenAPI 계약 동기화 실제 준수 모니터링 필요.

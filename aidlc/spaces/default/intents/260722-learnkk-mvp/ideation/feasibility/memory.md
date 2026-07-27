<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T11:00:00Z — 사용자가 여러 답변에서 "mvp"를 언급. 워크플로 스코프는 enterprise이므로, 여기서의 "mvp"는 초기 최소 산출물(파일럿)을 의미하는 것으로 해석(A-1). 사내 연동/클라우드/데이터 통제는 파일럿에서 제외하고 확장 시 재평가.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->
- 2026-07-22T11:00:00Z — Q3=로컬 서버라 AWS 서비스 선택/비용 산정을 이 단계에서 보류. aws-platform 관점은 "클라우드 이관은 확장 후속 과제"로 축약.

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-07-22T11:00:00Z — 자체 계정 인증(SSO 미연동) 선택: 파일럿 출시 속도를 위해 연동 복잡도 제거. 대신 검증된 Spring Security 사용 권고로 보안 리스크 완화.

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T11:00:00Z — 로컬 서버의 구체 형태(개발자 PC vs 사내 상시 서버) 미확정. application-design/infra에서 확정 (I-2).
- 2026-07-22T11:00:00Z — 전사 확장 시 직원 데이터 통제 정책은 법무/보안팀 확인 필요 (D-3, R-2).

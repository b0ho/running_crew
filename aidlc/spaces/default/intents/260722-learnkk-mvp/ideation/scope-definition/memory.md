<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T12:00:00Z — 참여 방식을 "멘토 승인"에서 "선착순 자동 참여 + 정원 마감 시 시스템 관리자 수동 점검·승인"으로 변경(Q1). 코호트에 정원(capacity) 개념 도입.
- 2026-07-22T12:00:00Z — 출석을 "멘토 증빙자료 첨부 업로드 → 출석 인증제"로 정의(Q1). 증빙 이력 보관 필요.
- 2026-07-22T12:00:00Z — Q2=B에서 "최종 보고서 이력" 언급 → 최종 보고서 제출(M7)을 신규 Must 기능으로 도출. 운영 지표(M8)도 Should→Must 승격.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->
- 2026-07-22T12:00:00Z — 회원가입 수집 정보를 이메일·성명·닉네임만으로 축소(Q1, 사번 제외). PII 최소화.

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-07-22T12:00:00Z — 정산서를 실제 정산 처리 없이 "정산 조건 충족" 메시지 수준으로 단순화(Q1). 온라인 결제/정산은 Won't 유지.

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T12:00:00Z — "정산 조건"의 구체 정의(무엇을 충족하면 조건 충족인지) 미확정. requirements에서 확정.
- 2026-07-22T12:00:00Z — 최종 보고서 형식(자유 vs 템플릿), 증빙 파일 형식/용량 제한 미확정. requirements/functional-design에서 확정.
- 2026-07-22T12:00:00Z — 시스템 관리자와 운영 담당자를 동일 역할로 가정. 분리 필요 여부 requirements에서 확인.

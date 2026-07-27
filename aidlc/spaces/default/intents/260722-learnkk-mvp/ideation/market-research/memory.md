<!-- INVARIANT: examples are single-line HTML comments so a fresh template parses to total=0 (MEMORY_EMPTY). Do NOT un-comment or split across lines. t100 guards this. -->
> This file is maintained by the orchestrator during stage execution. Add observations at the gate ritual, not by editing here directly.

## Interpretations
<!-- example: 2026-05-29T10:14:32Z — chose REST over GraphQL; the consuming team only needs CRUD, revisit if subscriptions land -->
- 2026-07-22T10:00:00Z — Q1(오픈소스 참고)과 Q5(직접 Build)를 "오픈소스를 baseline 참고 기준으로만 쓰고 그대로 채택하지 않으며 필요한 기능만 자체 구현"으로 해석. build-vs-buy와 competitive-analysis에 반영.
- 2026-07-22T10:00:00Z — Q4=C를 근거로 차별화 지점을 intent-statement의 "통합 증서"에서 "멘토 강의 수행 기록(지급 기록증)"으로 조정. 두 증서 모두 유지하되 차별화 강조점은 지급 기록증.
- 2026-07-22T10:30:00Z — [게이트 피드백 반영] 사용자가 증서는 차별화가 아니라 단순 수료증 이미지 1장 수준이라고 확정. 차별화를 "통합 관리·사내 흐름 맞춤"으로 되돌리고 competitive-analysis/build-vs-buy/market-trends 수정. project.md의 market-research:c2 학습을 정정하고 Corrections에 기록.

## Deviations
<!-- example: 2026-05-29T10:14:32Z — skipped the optional caching layer the stage prose suggested; the dataset is small enough that it adds risk -->
- 2026-07-22T10:00:00Z — 시장 규모(TAM/SAM/SOM) 정량 산정은 생략. 외부 판매가 아닌 사내 도구라 무의미; Q6의 사내 도입 규모로 대체.

## Tradeoffs
<!-- example: 2026-05-29T10:14:32Z — picked TDD over BDD this run; the team is unit-first and the domain is well-understood -->
- 2026-07-22T10:00:00Z — Build vs Buy: 유지보수 부담(-1)에도 불구하고 차별화·맞춤·가격 근거로 Build(+6) 선택. 범위를 좁게 유지해 유지보수 리스크 완화 권고.

## Open questions
<!-- example: 2026-05-29T10:14:32Z — confirm the retention window with compliance before the next stage hardens the schema -->
- 2026-07-22T10:00:00Z — Q3=특별 규제 없음이나, 멘토링/출석/강의 수행 기록은 직원 데이터를 포함. 전사 확장(Q6) 시 개인정보/인사 정책 재확인 필요.
- 2026-07-22T10:00:00Z — 오픈소스 "참고" 범위(참고만 vs 일부 컴포넌트 채택)는 application-design에서 확정 필요.

# NFR Design — 관찰 일지 (memory)

> Construction · nfr-design 단계 진행 일지. 유닛별 반복.
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-27T04:00:00Z — nfr-design은 nfr-requirements의 "무엇(목표)"에 대한 "어떻게(구체 설계)"다. 파일럿은 고급 NFR 패턴(서킷브레이커·캐시 티어·오토스케일·멀티리전)을 보류하므로, 설계는 파일럿에서 실제 채택하는 구체 메커니즘(세션/BCrypt 설정, 트랜잭션 경계, 로컬 스토리지, recreate 배포, 인덱스)을 명시하고 보류 패턴을 명확히 스코프아웃한다.
- 2026-07-27T04:00:00Z — 리뷰어 서브에이전트에는 team.md Mandated(한글 산출물) 준수를 위해 `## Review`를 한글로 작성하도록 명시 지시한다(project.md ## Corrections `cid:nfr-design:lang-subagent` 적용).

## Deviations

- 2026-07-27T04:00:00Z — Construction 질문 라운드 생략(project.md ## Corrections c2): NFR 설계가 nfr-requirements+관행에 pin되어 genuine gap 없음.

## Tradeoffs

- 2026-07-27T04:00:00Z — 파일럿에서 캐시·서킷브레이커·오토스케일 미도입. 근거: <100명·로컬 단일 서버에서 불필요한 복잡도. 대신 확장 트리거와 전환 경로(FileStorageService 백엔드 교체, 세션 스토어 외부화/JWT, 지표 캐시)를 설계에 명시해 확장 시 매끄러운 전환 보장.

## Open questions

- 2026-07-27T04:00:00Z — 헬스체크·모니터링 상세(엔드포인트·지표 수집)는 Operation 단계 observability-setup에서 구체화. 현재는 설계 수준 명시.

# build-and-test 관찰 일지 (memory.md)

> Construction · build-and-test 스테이지 · LearnKK 파일럿
> 리드 관점: QUALITY(리드) + DEVSECOPS(보안 테스트 입력)
> 상위 입력: U1~U6 code-generation-plan/code-summary, nfr-requirements/nfr-design(performance·security), memory/{org,team,project}.md, phases/construction.md
> 손으로 편집하지 않음(오케스트레이터 유지). ISO 타임스탬프 사용.

## Interpretations

- 2026-07-30T14:10:00Z — 이 스테이지는 코드를 재작성하지 않고 U1~U6에 이미 존재하는 테스트 자산을 **검증·확장·문서화**한다. Test Strategy=Comprehensive, 핵심 도메인 80% 라인 커버리지 목표(team.md Testing Posture).
- 2026-07-30T14:10:30Z — team.md/project.md 규칙에 따라 통합 테스트(Testcontainers)는 Docker 가용 CI 전용으로 취급하고, 로컬(Docker 부재)에서는 단위 테스트 + 실 compose 라이브 스모크로 동등 검증한다(`cid:code-generation:c1`). `@Tag("integration")` + `-PexcludeIntegration`로 로컬 배제.
- 2026-07-30T14:11:00Z — 성능 목표는 파일럿(<100명) 전제로 목표 latency 대비 단건 스모크로 갈음, 통계적 p95/부하는 Operation 단계 performance-validation으로 이관(performance-requirements §4).
- 2026-07-30T14:11:30Z — 보안 테스트는 devsecops 관점으로 인증/인가·입력검증·파일 업로드 매직바이트·BCrypt를 하드 검증하고, SAST/DAST/SCA/TLS는 파일럿 보류(확장 시 도입) — `cid:practices-discovery:c3`, security-design §5/§7.
- 2026-07-30T14:12:00Z — 모든 산문은 한글(team.md Mandated). 코드 식별자·명령·경로는 원문 유지.

## Deviations

- 2026-07-30T14:13:00Z — `npm ci`는 실행하지 않음. `learnkk-web/node_modules`가 이미 존재하여 과제 규칙("if node_modules missing")에 따라 생략. `npm test`/`npm run build`는 그대로 실행.
- 2026-07-30T14:13:20Z — 통합 테스트는 로컬 Docker 부재(`docker info` 실패)로 실행하지 않고 "CI 위임"으로 기록(과제 지시 준수).

## Tradeoffs

- 2026-07-30T14:11:45Z — 로컬 통합 테스트 미실행으로 실 DB 트랜잭션·락 경합·동시성 검증의 로컬 즉시 재현은 포기. 대신 (a) 단위 테스트가 서비스 로직 경계를 커버, (b) 동시성 정확성은 U3 code-generation 단계에서 실제 Testcontainers로 이미 검증된 이력 존재, (c) CI에서 전량 재실행으로 보완. 파일럿 리스크 수용.

## Open questions

- 2026-07-30T14:12:30Z — (해소) 백엔드 로컬 단위 테스트가 실제로 통과하는가? → 실행 결과 107건 전부 통과(0 실패/0 에러/0 스킵), spotlessJavaCheck 통과. BUILD SUCCESSFUL.
- 2026-07-30T14:13:40Z — 프론트 Jest/빌드 실제 통과 여부는 실행 결과로 확정 예정(build-test-results.md에 기록).

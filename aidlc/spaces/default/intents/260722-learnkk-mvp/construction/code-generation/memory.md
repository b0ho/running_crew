# Code Generation — 관찰 일지 (memory)

> Construction · code-generation 단계 진행 일지. 유닛별 반복(Bolt).
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-28T02:00:00Z — U1은 워킹 스켈레톤 Bolt(team.md·skeleton stance on). 관통 경로(가입→로그인→인증 호출 200→관리자 스텁 200)를 실제 실행 가능하게 하는 최소 슬라이스로 계획. 관리자 전용 스텁 엔드포인트 1개를 관통 검증용으로 포함.
- 2026-07-28T02:00:00Z — 관리자 시드는 domain-entities §6이 "Flyway 시드"라 하나, env 주입 + BCrypt 해싱 접근 용이성을 위해 Flyway 이후 실행되는 Spring ApplicationRunner 시더로 계획(멱등·fail-fast·env·평문 커밋 금지 규칙은 동일 유지). Java 기반 Flyway 마이그레이션도 허용 대안으로 명시.

## Deviations

- 2026-07-28T02:00:00Z — Construction 질문 라운드는 생략하나, code-generation은 Step 3 Plan Approval이 모든 모드에서 필수 하드 스톱이므로 계획 승인 게이트는 반드시 제시(질문 생략 규칙과 별개).

## Tradeoffs

- 2026-07-28T02:00:00Z — FE/BE 분리 저장소는 워크스페이스 루트 하위 디렉터리(learnkk-api/·learnkk-web/)로 실체화(별도 git 저장소 대신 파일럿 단일 워크스페이스 내 분리). 근거: 파일럿 단일 워크스페이스에서 관리 단순화, 계약 동기화(OpenAPI)는 유지. 실제 분리 저장소 분할은 확장 시.

## Open questions

- 2026-07-28T02:00:00Z — 세션 쿠키 정책에서 CSRF 처리(SameSite=Lax 우선 + 상태변경 보호)의 구체 방식은 개발자 에이전트 구현에서 확정. Testcontainers 실행에 Docker 필요 — CI/로컬 환경 전제.

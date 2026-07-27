# RAID Log — LearnKK (사내 멘토링 통합 플랫폼)

> Ideation · feasibility 단계 산출물 · Risks / Assumptions / Issues / Dependencies
> 상위 입력: `intent-capture/intent-statement.md`, `market-research/build-vs-buy.md`, `market-research/market-trends.md`
> 근거: feasibility-questions.md (Q1~Q6)

## Risks (리스크)

| ID | 리스크 | 가능성 | 영향 | 대응 |
|---|---|---|---|---|
| R-1 | 로컬 서버 호스팅이 파일럿 이후 확장에 병목 | 중 | 중 | 컨테이너 기반 구성으로 클라우드 이관 대비(application-design) |
| R-2 | 직원 데이터 통제 미적용 상태로 확장 시 규제/정책 노출 | 중 | 중 | 전사 확장 전 접근통제·보관정책 재확인 (compliance) |
| R-3 | 자체 계정 인증 자체 구현의 보안 결함 | 낮 | 중 | 검증된 프레임워크 기능(Spring Security) 사용, 범위 최소화 |
| R-4 | 빠른 파일럿 우선으로 필수 기능 외 누락 | 낮 | 낮 | 필수(table-stakes) 범위 명확화(scope-definition) |

## Assumptions (가정)

| ID | 가정 |
|---|---|
| A-1 | "mvp"는 워크플로 스코프(enterprise)와 별개로, 초기 최소 산출물(파일럿)을 의미한다 |
| A-2 | 파일럿 사용자는 사내 직원 대상, 총 100명 미만 |
| A-3 | 증서(수료증·지급 기록증)는 단순 이미지 1장 수준의 부가 산출물 |
| A-4 | 파일럿 단계 데이터 통제는 임시로 사내 일반 도구 수준 |

## Issues (이슈)

| ID | 이슈 | 상태 |
|---|---|---|
| I-1 | 오픈소스 "참고" 범위(참고만 vs 일부 컴포넌트 채택) 미확정 | application-design에서 확정 |
| I-2 | 로컬 서버의 구체 형태(개발자 PC vs 사내 상시 서버) 미확정 | application-design/infra에서 확정 |

## Dependencies (의존)

| ID | 의존 |
|---|---|
| D-1 | scope-definition에서 필수 기능 범위 확정 → 이후 설계 |
| D-2 | React+Spring 스택 확정(TC-1)에 따른 application-design 방향 |
| D-3 | 전사 확장 시 데이터 정책은 법무/보안팀 확인 필요(Q4=D 대안) |

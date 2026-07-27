# Risk & Sequencing Rationale — LearnKK (파일럿)

> Inception · delivery-planning 단계 산출물 · Bolt 순서의 경제적/리스크 근거
> 상위 입력: `bolt-plan.md`, `units-generation/unit-of-work-dependency.md`(DAG), `application-design/decisions.md`, `requirements-analysis/requirements.md`

## 시퀀싱 원칙

- 위상 제약(DAG U1→U2→(U3∥U4)→U5→U6)을 준수하되, 그 안에서 리스크 조기 노출을 우선.

## Bolt별 리스크 & 근거

| Bolt | 핵심 리스크 | 순서 근거 |
|---|---|---|
| Bolt 1 U1 | 아키텍처/CI/배포 미검증 | 스켈레톤으로 최우선 검증(ON). 이후 모든 Bolt의 기반 |
| Bolt 2 U2 | 회차 구조 정합 | U3/U4의 선행 의존이므로 조기 |
| Bolt 3 U3 | **동시성(정원 초과)** — 최대 정합성 리스크 | U2 직후 조기 착수해 락·유니크 제약 검증(Testcontainers) |
| Bolt 4 U4 | 파일 업로드/저장 | U2 이후, U3와 병렬 가능 |
| Bolt 5 U5 | 수료·정산 판정 산식 경계 | U3(확정 참여)·U4(출석) 이후 |
| Bolt 6 U6 | 집계 정확성 | 상위 데이터 모두 존재 후 |

## 주요 리스크 대응

- 동시성(R): Bolt 3에서 ExecutorService+CountDownLatch 동시성 테스트 필수.
- 파일 보안 보류(R): 파일럿 잔여 리스크 — 확장 전 검증 계층 추가(project.md).
- 판정 경계(R): 출석 79/80% 경계 테스트(US-12 AC).

## 크리티컬 패스

- U1→U2→U3→U5→U6 (U4는 U3와 병렬이나 U5가 U4도 요구). 크리티컬 패스 길이 5.

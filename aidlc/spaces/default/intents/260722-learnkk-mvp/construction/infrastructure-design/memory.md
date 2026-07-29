# Infrastructure Design — 관찰 일지 (memory)

> Construction · infrastructure-design 단계 진행 일지. 유닛별 반복.
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-28T00:00:00Z — 리드가 aws-platform이나 project.md/team.md는 퍼블릭 클라우드를 파일럿 Forbidden으로 명시. 따라서 AWS 서비스 매핑 대신 **로컬 Docker(compose) 인프라**로 설계하고, AWS/클라우드는 명시적 확장 트리거로 스코프아웃. 메모리 규칙이 에이전트 기본(AWS)에 우선.
- 2026-07-28T00:00:00Z — U1은 foundation 유닛이므로 shared-infrastructure(공유 DB·파일 볼륨·컨테이너·CI/CD·시크릿)와 공유 스키마 소유·접근 경계를 여기서 확립. U2~U6은 이를 상속하며 자기 테이블·서비스만 추가.

## Deviations

- 2026-07-28T00:00:00Z — Construction 질문 라운드 생략(project.md ## Corrections c2): 인프라 결정(로컬 Docker·recreate·GitHub Actions·git SHA·일 1회 백업)이 team.md Deployment·practices에 pin되어 genuine gap 없음.

## Tradeoffs

- 2026-07-28T00:00:00Z — 단일 호스트 docker compose 채택(쿠버네티스/스웜 미도입). 근거: <100명·단일 서버 파일럿에 오케스트레이터는 과잉. 확장(다중 인스턴스·HA) 시 클라우드 오케스트레이션 이관 트리거 명시.

## Open questions

- 2026-07-28T00:00:00Z — 상세 관측(APM·분산 트레이싱·Prometheus/Grafana)·SLI/SLO·정량 RPO/RTO·알림 룰은 Operation observability-setup/incident-response에서 구체화. 본 단계는 헬스체크·기본 지표 노출·백업 존재까지 설계.

## Deviations

- 2026-07-28T01:00:00Z — U6 리뷰에서 크로스유닛 인덱스 필드명 불일치(BF-1) 발견·수정: U5 `final_report` 인덱스가 `created_at`으로 명시됐으나 엔티티 필드는 `submittedAt`. U5 nfr-design/performance-design §3와 infrastructure-services §1을 `submitted_at`으로 정정. 이미 게이트 승인된 U5 nfr-design의 사실 오류를 소급 정정(인덱스 컬럼명이 엔티티 필드와 일치해야 code-generation에서 실패하지 않음).

## Tradeoffs

- 2026-07-28T01:00:00Z — U2~U6 인프라 산출물은 U1-foundation의 공유 인프라를 상속하고 유닛 고유 사항(스키마·인덱스·테스트·파일 볼륨 사용)만 명시하는 얇은 문서로 작성. 근거: 파일럿이 단일 배포단위(모듈러 모놀리스)라 인프라가 실질적으로 공유되며, 유닛별 중복 서술은 드리프트 위험만 키움.

## Open questions

- 2026-07-28T01:00:00Z — 인덱스 컬럼명이 엔티티 필드명과 일치하는지 검증하는 센서(예: 마이그레이션/설계의 인덱스 참조를 엔티티 필드 목록과 대조)를 도입할지 검토(BF-1 유형 재발 방지). code-generation에서 실제 실패로 드러나므로 파일럿 필수는 아니나 조기 검출 가치 있음.

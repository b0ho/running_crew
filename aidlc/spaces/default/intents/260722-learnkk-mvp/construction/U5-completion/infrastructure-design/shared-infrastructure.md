# Shared Infrastructure — U5 completion (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U5-completion
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/logical-components.md`·`scalability-design.md`·`reliability-design.md`·`security-design.md`·`performance-design.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: 공유 인프라는 U1-foundation 확립. 본 문서는 U5가 공유 리소스와 맺는 관계·경계만 명시.

## 1. U5가 사용하는 공유 리소스(U1 확립)

| 공유 리소스 | U5 사용 |
|---|---|
| PostgreSQL(`learnkk-db`) | FinalReport·Certificate·SettlementStatus 테이블(U5 소유) 추가 |
| 파일 볼륨(`uploads`) | 보고서 첨부·수료증 이미지(U4와 공유, FileStorageService 경유) |
| `learnkk-api` / `learnkk-web` | 수료/정산/보고서 도메인 모듈 / UI |
| GitHub Actions 파이프라인 | U5 종료 오케스트레이션 테스트 포함 |

## 2. 스키마·스토리지 소유·접근 경계

- **U5 소유 테이블**: FinalReport, Certificate, SettlementStatus. Flyway 순서 (U3∥U4) 다음.
- **접근 경계(캡슐화)**: U5는 종료·판정의 단일 소유자. U2(Cohort/Session 읽기 + status 세터 `transitionToEnded` 호출), U3(`confirmedEnrollments` 읽기 + `notify` 호출), U4(회차 인증 상태 읽기), U1(FileStorage 호출)와 서비스 계약으로 상호작용(component-methods.md 레지스트리 등록). **U2/U3/U4는 U5를 호출하지 않음**(단방향, 순환 없음, DAG 유지).
- **파일 볼륨 공유**: U4와 uploads 공유하되 서버 UUID 파일명으로 충돌 없음, FileStorageService 경유만.

## 3. 확장 — 증서 발급 비동기화 & 스토리지 분리

- 대규모 코호트 종료의 긴 트랜잭션 회피는 증서 발급 비동기 배치(워커/큐 인프라 도입, `scalability-design.md` §2) — U5 고유 확장.
- 파일 스토리지 분리(오브젝트 스토리지)는 U1·U4 공통 교체점(FileStorageService 백엔드). 나머지 확장 경로는 U1 shared-infrastructure §4 공통.

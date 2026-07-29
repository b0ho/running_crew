# Shared Infrastructure — U4 attendance (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U4-attendance
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/logical-components.md`·`scalability-design.md`·`reliability-design.md`·`security-design.md`·`performance-design.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: 공유 인프라는 U1-foundation 확립. 본 문서는 U4가 공유 리소스와 맺는 관계·경계만 명시.

## 1. U4가 사용하는 공유 리소스(U1 확립)

| 공유 리소스 | U4 사용 |
|---|---|
| PostgreSQL(`learnkk-db`) | AttendanceEvidence 테이블(U4 소유) 추가 |
| **파일 볼륨(`uploads`)** | **U4 주 사용처**(증빙 파일). U1 FileStorageService(store/load/delete) 경유만 |
| `learnkk-api` / `learnkk-web` | 출석 도메인 모듈 / UI |
| GitHub Actions 파이프라인 | U4 파일+DB 테스트 포함 |

## 2. 스키마·스토리지 소유·접근 경계

- **U4 소유 테이블**: AttendanceEvidence. Flyway 순서 (U3∥U4).
- **파일 볼륨 접근 경계**: uploads는 U4·U5 공유하되 **모든 접근은 U1 FileStorageService 경유**(직접 파일시스템 접근 금지). 서버 UUID 파일명으로 유닛 간 충돌 없음.
- **크로스유닛 계약**: U4→U1 `FileStorageService.store/load/delete`(호출), U4→U2 `SessionService.markVerified`(호출)·Session/Cohort 읽기. U5→U4·U6→U4 read-only(회차 인증 상태·증빙 이력). U4는 U5 호출 안 함(순환 없음, DAG 유지).

## 3. 확장 — 스토리지 분리

uploads 볼륨은 다중 인스턴스의 핵심 제약. 확장 시 공유/오브젝트 스토리지로 FileStorageService 백엔드 교체(U1·U5와 공통 교체점, `scalability-design.md` §2). 나머지 확장 경로는 U1 shared-infrastructure §4 공통.

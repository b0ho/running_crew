# Logical Components — U5 completion (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U5-completion
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/tech-stack-decisions.md`(종료 오케스트레이션·이미지 생성), `functional-design/business-logic-model.md`(W-U5-1~5·§5 크로스유닛 계약), 나머지 NFR requirements(적용 지점)
> 목적: U5 NFR 설계가 적용되는 논리 컴포넌트 인벤토리 — 오케스트레이션 경계·실패 도메인·크로스유닛 계약(단일 소유·순환 없음).

## 1. 논리 컴포넌트 인벤토리

U5는 U1 배포단위(`learnkk-api`/`learnkk-web`) 안의 수료/정산/보고서 도메인 모듈이며, **코호트 종료·판정의 단일 소유자**다.

| 컴포넌트 | 종류 | 책임 | 적용 NFR 설계 |
|---|---|---|---|
| `CompletionController` | 컴포넌트 | endCohort·certificateOf HTTP 경계 | 보안(소유·본인 스코프) |
| `CompletionService` | 컴포넌트 | 종료 오케스트레이션·수료/정산 판정·수료증 발급 | 신뢰성(원자성·보상·판정)·보안(무결성) |
| `CertificateRenderer` | 컴포넌트 | 수료증 이미지 생성(템플릿→PNG) | 성능(메모리 효율)·보안(PII 최소) |
| `ReportService`/`ReportController` | 컴포넌트 | 최종 보고서 제출·이력 | 신뢰성(파일 보상)·보안(참여자 인가) |
| `Certificate/FinalReport/SettlementStatus Repository` | 컴포넌트 | 영속·UNIQUE 제약·인덱스 | 신뢰성(멱등·1건)·성능 |
| `FileStorageService`(U1) | 라이브러리(공유) | store/load/delete(보고서·수료증) | 보안·신뢰성(다건 보상)·확장 |
| Certificate/FinalReport/SettlementStatus 테이블 + 파일 볼륨 | 데이터/스토리지(공유) | 판정 결과·파일 | 내구성(볼륨 백업)·확장 |

## 2. 서비스 경계 & 종료 오케스트레이션

```
+-------------------------------------------------------------------+
|  learnkk-api (Spring)                                             |
|   CompletionController --> CompletionService (endCohort, 단일 TX) |
|     ├─ U2 read: Cohort/Session 집계                               |
|     ├─ U3 read: confirmedEnrollments(확정 멘티 목록)              |
|     ├─ CertificateRenderer -> U1 FileStorageService.store(누적)   |
|     ├─ Certificate/SettlementStatus upsert                        |
|     ├─ U2 call: Cohort.status 세터(종료됨)                        |
|     └─ U3 call: NotificationService.notify(결과)                  |
|     (롤백 시 누적 imagePath 전부 U1.delete 보상)                  |
+-------------------------------------------------------------------+
```
<!-- Text fallback: CompletionService.endCohort는 단일 트랜잭션에서 U2 회차/코호트를 집계하고 U3 확정 멘티 목록을 읽어 수료 판정 후 수료증 이미지를 생성·저장(누적)하고, 정산 upsert, U2 상태 세터로 종료됨 전이, U3 알림을 수행한다. 롤백 시 누적한 모든 imagePath를 U1 delete로 보상한다. -->

`business-logic-model.md` §5 크로스유닛 계약을 컴포넌트 관점으로 확정:

| 방향 | 계약 | 제공/요구 |
|---|---|---|
| U5 → U2 (읽기) | Cohort·Session(전체/인증 회차 수) 조회 | U2 제공 |
| U5 → U2 (호출) | Cohort.status 세터(종료됨) | U2 제공(U2 §8) |
| U5 → U3 (읽기) | `EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>` | U3 제공(레지스트리 반영) |
| U5 → U3 (호출) | `NotificationService.notify(...)` | U3 제공 |
| U5 → U1 (호출) | `FileStorageService.store/load/delete` | U1 제공 |
| U6 → U5 (읽기) | 수료율·증서 수·보고서 이력 | U5 제공 |

- **단일 소유·순환 부재**: U5는 종료·판정의 단일 소유자이며 U2/U3/U4 데이터를 읽고 U2 세터·U3 알림을 호출한다. **U2/U3/U4는 U5를 호출하지 않는다**(단방향, `cid:units-generation:c2`). DAG U1→U2→(U3∥U4)→U5→U6 유지.

## 3. 실패 도메인 & Blast Radius

| 실패 지점 | Blast Radius | 완화 |
|---|---|---|
| 종료 트랜잭션 롤백 | 해당 코호트 종료만 | 전체 롤백 + 누적 이미지 보상 delete(정합성 유지) |
| 개별 증서 이미지 생성 실패 | 종료 트랜잭션 롤백 | 앞선 이미지 누적 delete로 정리 |
| 보상 delete 일부 실패 | 고아 이미지 소수(정합성 무관) | ERROR 로그 → 수동 정리 |
| 보고서 첨부 저장 실패 | 해당 제출만 | 보상 delete(U4 패턴) |
| DB 다운 | 전체(공유 리소스) | U1과 동일 |

## 4. 공유 리소스 & 상속

- **상속(U1)**: SecurityConfig, GlobalExceptionHandler, DTO/OpenAPI, Flyway, FileStorageService(store/load/delete).
- **의존(U2/U3/U4 read)**: 회차/코호트(U2), 확정 멘티(U3), 인증 회차 상태(U4).
- **U5가 제공하는 계약**: 수료율·증서 수·보고서 이력 read-only(U6 집계·이력 조회).
- **공유 리소스**: PostgreSQL(Certificate/FinalReport/SettlementStatus 스키마), 파일 볼륨(보고서 첨부·수료증 이미지 — 백업 대상, `reliability-design.md` §5).

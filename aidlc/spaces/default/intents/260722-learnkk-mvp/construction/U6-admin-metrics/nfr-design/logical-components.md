# Logical Components — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/tech-stack-decisions.md`(집계 쿼리·Pageable), `functional-design/business-logic-model.md`(W-U6-1~3·§5 크로스유닛 계약), 나머지 NFR requirements(적용 지점)
> 목적: U6 NFR 설계가 적용되는 논리 컴포넌트 인벤토리 — 읽기 전용 소비 경계·실패 도메인·크로스유닛 계약(말단 소비자, 쓰기 없음).

## 1. 논리 컴포넌트 인벤토리

U6은 U1 배포단위(`learnkk-api`/`learnkk-web`) 안의 리포팅/이력 도메인 모듈이며 **읽기 전용 말단 소비자**다.

| 컴포넌트 | 종류 | 책임 | 적용 NFR 설계 |
|---|---|---|---|
| `MetricsController` | 컴포넌트 | overview HTTP 경계(관리자) | 보안(ADMIN 인가) |
| `HistoryController` | 컴포넌트 | 증빙/보고서 이력 HTTP 경계(관리자) | 보안(ADMIN·PII 보호) |
| `MetricsService` | 컴포넌트 | 운영 지표 집계(실시간) | 신뢰성(정확성·0 나눗셈)·성능(인덱스) |
| `HistoryService` | 컴포넌트 | 증빙/보고서 이력 조회(분리 뷰) | 성능(조인·페이지네이션)·보안 |
| `MetricsRepository` | 컴포넌트 | 읽기 전용 집계 JPQL/native 쿼리 | 성능(기준 인덱스)·신뢰성(범위 일관) |
| (소스 테이블: Cohort/Session/Enrollment/Certificate/AttendanceEvidence/FinalReport) | 데이터(공유, 읽기) | U2~U5 소유 스키마 | U6은 읽기만 |

- U6은 신규 저장소/엔티티가 없다(읽기 전용). 공유 스키마의 소스 테이블을 조인·집계만 한다.

## 2. 서비스 경계 & 읽기 전용 소비

```
+------------------------------------------------------------+
|  learnkk-api (Spring)                                       |
|   MetricsController --@PreAuthorize(ADMIN)--> MetricsService |
|   HistoryController --@PreAuthorize(ADMIN)--> HistoryService |
|        (readOnly=true 트랜잭션, 쓰기 없음)                  |
|        └─ read-only JPQL/native 집계·조인 쿼리              |
+------------------------------------------------------------+
        | 읽기만 (쓰기 없음, INV-U6-1)
        v
   U2(코호트/회차) · U3(확정 멘티) · U4(증빙 이력) · U5(증서/보고서)
```
<!-- Text fallback: MetricsController/HistoryController는 관리자 인가 후 MetricsService/HistoryService를 호출하고, 읽기 전용 트랜잭션에서 U2~U5 소스 테이블을 조인·집계만 한다. U6은 어떤 데이터도 쓰지 않는다. -->

`business-logic-model.md` §5 크로스유닛 계약(모두 읽기):

| 방향 | 계약 | 제공 |
|---|---|---|
| U6 → U2 (읽기) | 종료됨 코호트·회차 수 | U2 제공 |
| U6 → U3 (읽기) | confirmedCount/confirmedEnrollments | U3 제공(레지스트리 반영) |
| U6 → U4 (읽기) | 증빙 이력(조인) | U4 제공 |
| U6 → U5 (읽기) | 증서 수·수료 데이터·보고서 이력 | U5 제공 |

- **말단 소비자·쓰기 없음**: 어떤 유닛도 U6을 호출하지 않으며, U6은 아무것도 쓰지 않는다(INV-U6-1). DAG의 종착점(U1→U2→(U3∥U4)→U5→U6). 순환 불가능(U6은 호출 대상이 아님).
- **읽기 경로 결정**: 파일럿 기본은 U6 `MetricsRepository`가 공유 스키마를 직접 읽는 **리포팅 읽기 모델**. 소스 유닛의 read API 조합도 결과 동일(팀 선택 가능) — 어느 경로든 쓰기 없음.

## 3. 실패 도메인 & Blast Radius

| 실패 지점 | Blast Radius | 완화 |
|---|---|---|
| 집계 쿼리 실패 | 해당 조회 요청만 | 500 정규화, 부분 결과 금지(일관 지표만) |
| DB 다운 | 전체(공유 리소스) | U1과 동일. U6은 자체 데이터 없음 |
| 소스 데이터 일시 부정합 | 없음(READ_COMMITTED 커밋 상태만 읽음) | 트랜잭션 격리 |

- U6은 쓰기가 없어 **다른 유닛에 장애를 전파하지 않는다**(읽기 전용 소비자의 격리 강점).

## 4. 공유 리소스 & 상속

- **상속(U1)**: SecurityConfig(`@PreAuthorize` ADMIN·세션), GlobalExceptionHandler(에러 DTO), DTO/OpenAPI, FileStorageService.load(이력 파일 다운로드).
- **의존(U2~U5 read)**: 소스 스키마 조인·집계.
- **U6이 제공하는 것**: 없음(말단 소비자). 지표·이력 응답은 관리자 UI 소비.
- **공유 리소스**: PostgreSQL(소스 스키마 읽기 전용 접근). U6 고유 스키마·파일 없음.

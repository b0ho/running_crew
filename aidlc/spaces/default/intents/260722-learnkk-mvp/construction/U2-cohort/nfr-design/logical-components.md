# Logical Components — U2 cohort (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/tech-stack-decisions.md`(상속 스택·U2 고유 선택), `functional-design/business-logic-model.md`(W-U2-1~6·§8 크로스유닛 계약), 나머지 NFR requirements(적용 지점)
> 목적: U2 NFR 설계가 적용되는 논리 컴포넌트 인벤토리 — 경계·실패 도메인·크로스유닛 계약.

## 1. 논리 컴포넌트 인벤토리

U2는 U1의 배포단위(`learnkk-api`/`learnkk-web`) 안에서 동작하는 코호트 도메인 모듈이다.

| 컴포넌트 | 종류 | 책임 | 적용 NFR 설계 |
|---|---|---|---|
| `CohortController` | 컴포넌트 | 코호트 CRUD·start·상세 HTTP 경계 | 보안(인증)·성능(페이지네이션) |
| `CohortService` | 컴포넌트 | 개설(원자)·수정·조회·전이 도메인 로직 | 신뢰성(원자성·전이)·보안(소유권) |
| `AnnouncementService` | 컴포넌트 | 공지 작성·조회 | 보안(소유권·외부링크) |
| `SessionService` | 컴포넌트 | 회차 조회 + `markVerified` 전이(U4 호출) | 신뢰성(전이 캡슐화) |
| `CohortRepository`/`SessionRepository`/`AnnouncementRepository` | 컴포넌트 | 영속·인덱스 조회 | 성능(인덱스)·확장 |
| Cohort/Session/Announcement 테이블 | 데이터(공유) | U3~U6이 읽는 공유 도메인 | 확장(인덱스)·내구성(백업) |

## 2. 서비스 경계 & 크로스유닛 계약

```
+----------------------------------------------------------+
|  learnkk-api (Spring)                                     |
|   CohortController -> CohortService -> CohortRepository    |
|                    -> AnnouncementService                  |
|                    -> SessionService(markVerified)         |
|   (소유권 검증: 서비스 레이어 / @PreAuthorize: U1 골격)     |
+----------------------------------------------------------+
     ^ 읽기(confirmedCount)        ^ 쓰기 경로(status 세터)
     |                            |
   U3(EnrollmentService)        U5(CompletionService 종료 오케스트레이션)
     ^ 쓰기 경로(markVerified)
     |
   U4(AttendanceService 증빙 인증)
```
<!-- Text fallback: CohortController가 CohortService/AnnouncementService/SessionService를 호출한다. U2는 U3에서 confirmedCount를 읽고, U5는 U2의 status 세터로 종료 전이를, U4는 SessionService.markVerified로 회차 인증 전이를 수행한다. U2는 다른 유닛을 호출하지 않아 순환이 없다. -->

`business-logic-model.md` §8의 크로스유닛 계약을 컴포넌트 관점으로 확정:

| 방향 | 계약 | 제공/요구 |
|---|---|---|
| U2 → U3 (읽기) | `EnrollmentService.confirmedCount(cohortId): int` | U3가 노출해야 할 신규 read-only 계약 |
| U4 → U2 (쓰기) | `SessionService.markVerified(sessionId)` | U2 제공(회차 예정→인증 전이 캡슐화) |
| U5 → U2 (쓰기) | Cohort.status 세터(종료됨 전이) | U2 제공 |
| U3/U4/U5/U6 → U2 (읽기) | Cohort/Session 조회(get/list) | U2 제공 |

- **순환 부재**: U2는 U5를 호출하지 않는다(종료 오케스트레이션은 U5가 U2 데이터를 읽어 수행, `cid:units-generation:c2`). DAG U1→U2→(U3∥U4)→U5→U6 유지.

## 3. 실패 도메인 & Blast Radius

| 실패 지점 | Blast Radius | 완화 |
|---|---|---|
| CohortService 예외 | 해당 요청만(공통 에러 DTO 정규화, U1 골격) | 트랜잭션 롤백으로 정합성 유지 |
| U3 confirmedCount 조회 실패 | 정원 축소 요청만 | 안전 실패(거부), 나머지 코호트 기능 지속 |
| DB 다운 | 전체(공유 리소스) | U1과 동일(재기동·백업) |

## 4. 공유 리소스 & 상속

- **상속(U1)**: SecurityConfig(인증·RBAC·세션), GlobalExceptionHandler(에러 DTO), DTO/OpenAPI 규약, Flyway.
- **U2가 확립하는 공유 도메인**: Cohort/Session/Announcement 스키마와 조회/전이 서비스 계약 — U3~U6이 이 계약으로 상호작용.
- **공유 리소스**: PostgreSQL(공유 스키마에 코호트 도메인 추가). 파일 볼륨은 U2 미사용(U4/U5 사용).

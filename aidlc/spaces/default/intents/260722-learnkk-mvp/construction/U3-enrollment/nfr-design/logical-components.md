# Logical Components — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/tech-stack-decisions.md`(락·격리), `functional-design/business-logic-model.md`(W-U3-1~7·§7 크로스유닛 계약), 나머지 NFR requirements(적용 지점)
> 목적: U3 NFR 설계가 적용되는 논리 컴포넌트 인벤토리 — 경계·실패 도메인·크로스유닛 계약(순환 없음).

## 1. 논리 컴포넌트 인벤토리

U3는 U1 배포단위(`learnkk-api`/`learnkk-web`) 안의 참여/알림 도메인 모듈이다.

| 컴포넌트 | 종류 | 책임 | 적용 NFR 설계 |
|---|---|---|---|
| `EnrollmentController` | 컴포넌트 | join·myApplications HTTP 경계 | 보안(인증)·성능(페이지네이션) |
| `EnrollmentService` | 컴포넌트 | 선착순 참여(락)·확정/대기 결정·confirmedCount/confirmedEnrollments | 신뢰성(락·정합성)·보안(self-enrollment) |
| `AdminApprovalService` | 컴포넌트 | 대기 목록·승인·거절(관리자) | 신뢰성(조건부 UPDATE 경합 방지)·보안(ADMIN 인가) |
| `NotificationService` | 컴포넌트 | 알림 생성(제공 계약)·본인 조회·읽음 | 보안(본인 스코프) |
| `EnrollmentRepository`/`NotificationRepository` | 컴포넌트 | 영속·락 조회(FOR UPDATE)·인덱스 조회 | 성능·신뢰성·확장 |
| Enrollment/Notification 테이블 | 데이터(공유) | U2/U5/U6이 읽는 확정 데이터·알림 | 확장(DB 단일 진실 소스)·내구성(백업) |

## 2. 서비스 경계 & 크로스유닛 계약

```
+------------------------------------------------------------+
|  learnkk-api (Spring)                                       |
|   EnrollmentController -> EnrollmentService --FOR UPDATE--> [Cohort row 락]
|                                             -> EnrollmentRepository
|   AdminApprovalService(조건부 UPDATE)                        |
|   NotificationService(제공: notify)                          |
+------------------------------------------------------------+
     | 읽음(capacity/status)         ^ 제공(read: confirmedCount/confirmedEnrollments)
     v                               |
   U2(CohortService.get)           U2(정원 축소 검증)·U5(수료 발급)·U6(집계)
                                     ^ 호출(write: notify)
                                     |
                                   U5/U8(알림 생성 요청)
```
<!-- Text fallback: EnrollmentService가 Cohort 행에 FOR UPDATE 락을 걸어 선착순 참여를 직렬화한다. U3는 U2에서 capacity/status를 읽고, 확정 인원/확정 참여를 U2·U5·U6에 read-only로 제공하며, 알림 생성 API(notify)를 U5/U8이 호출한다. U3는 U5를 호출하지 않아 순환이 없다. -->

`business-logic-model.md` §7 크로스유닛 계약을 컴포넌트 관점으로 확정:

| 방향 | 계약 | 제공/요구 |
|---|---|---|
| U3 → U2 (읽기) | Cohort.capacity·status 조회 | U2 제공(get) |
| U2 → U3 (읽기) | `EnrollmentService.confirmedCount(cohortId): int` | **U3 제공(본 유닛 구현)** — U2 R-U2-09 요구 충족 |
| U5/U6 → U3 (읽기) | `EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>` | **U3 제공(본 유닛 구현)** — U5 수료증 발급·U6 집계 |
| U5/U8 → U3 (쓰기) | `NotificationService.notify(userId, type, message)` | U3 제공 |

- **순환 부재**: U3는 U5를 호출하지 않는다(U5가 U3 데이터를 읽어 수료 판정). DAG U1→U2→(U3∥U4)→U5→U6 유지. U3와 U4는 병렬 유닛이며 상호 직접 의존 없음(둘 다 U2를 읽음).

## 3. 실패 도메인 & Blast Radius

| 실패 지점 | Blast Radius | 완화 |
|---|---|---|
| 락 경합/타임아웃 | 해당 코호트 join 요청만(코호트 단위 국소화) | 롤백 + 409, 다른 코호트 무영향 |
| join 트랜잭션 실패 | 해당 요청만 | 롤백(부분 확정 없음) |
| 알림 생성 실패 | 해당 join(동일 트랜잭션이면 함께 롤백) | `reliability-design.md` §2 경계 결정 |
| DB 다운 | 전체(공유 리소스) | U1과 동일(재기동·백업). 정합성은 DB 복구로 보존 |

- **정합성 격리**: DB가 정원 제어의 단일 진실 소스이므로, 애플리케이션 인스턴스 장애가 정원 정합성을 훼손하지 않는다(`scalability-design.md` §2).

## 4. 공유 리소스 & 상속

- **상속(U1)**: SecurityConfig(인증·RBAC·세션·`@PreAuthorize`), GlobalExceptionHandler(에러 DTO), DTO/OpenAPI 규약, Flyway.
- **U3가 확립하는 공유 계약**: `confirmedCount`/`confirmedEnrollments`(읽기 제공), `NotificationService.notify`(쓰기 제공) — U2/U5/U6/U8이 이 계약으로 상호작용.
- **공유 리소스**: PostgreSQL(공유 스키마에 Enrollment/Notification 추가). 파일 볼륨 U3 미사용.

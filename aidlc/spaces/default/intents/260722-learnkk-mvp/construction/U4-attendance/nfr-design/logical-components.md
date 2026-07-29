# Logical Components — U4 attendance (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/tech-stack-decisions.md`(MultipartFile·FileStorage), `functional-design/business-logic-model.md`(W-U4-1~3·§6 크로스유닛 계약), 나머지 NFR requirements(적용 지점)
> 목적: U4 NFR 설계가 적용되는 논리 컴포넌트 인벤토리 — 파일+DB 경계·실패 도메인·크로스유닛 계약.

## 1. 논리 컴포넌트 인벤토리

U4는 U1 배포단위(`learnkk-api`/`learnkk-web`) 안의 출석/증빙 도메인 모듈이다.

| 컴포넌트 | 종류 | 책임 | 적용 NFR 설계 |
|---|---|---|---|
| `AttendanceController` | 컴포넌트 | 업로드(multipart)·진도 조회·다운로드 HTTP 경계 | 보안(권한·파일 검증)·성능(스트리밍) |
| `AttendanceService` | 컴포넌트 | 업로드+인증 트랜잭션·보상·진도 집계 | 신뢰성(원자성·보상)·보안(소유권) |
| `AttendanceEvidenceRepository` | 컴포넌트 | 증빙 이력 영속·인덱스 조회 | 성능·확장 |
| `FileStorageService`(U1 제공) | 라이브러리(공유) | store/load/**delete** — 파일 저장·경로 이탈 방지 | 보안(파일)·확장(백엔드 교체점)·신뢰성(보상) |
| `SessionService`(U2 제공) | 컴포넌트(외부) | `markVerified` 회차 인증 전이 | 신뢰성(트랜잭션 참여) |
| AttendanceEvidence 테이블 + 파일 볼륨 | 데이터/스토리지(공유) | 증빙 메타·파일 본체 | 내구성(볼륨 백업)·확장(스토리지 교체) |

## 2. 서비스 경계 & 파일+DB 흐름

```
+------------------------------------------------------------+
|  learnkk-api (Spring)                                       |
|   AttendanceController --multipart--> AttendanceService     |
|     (1) 사전검증(권한·파일제약)                              |
|     (2) FileStorageService.store  -----> [로컬 파일 볼륨]    |  (TX 밖)
|     (3) @Transactional {                                    |
|           EvidenceRepository.save                           |
|           U2 SessionService.markVerified  }                 |  (원자적)
|     (4) 롤백 시 FileStorageService.delete(보상)             |
+------------------------------------------------------------+
```
<!-- Text fallback: AttendanceService는 사전 검증 후 트랜잭션 밖에서 파일을 저장하고, 하나의 트랜잭션에서 증빙 이력 저장과 U2 markVerified를 원자적으로 수행한다. 롤백 시 저장 파일을 U1 delete로 보상 삭제한다. -->

`business-logic-model.md` §6 크로스유닛 계약을 컴포넌트 관점으로 확정:

| 방향 | 계약 | 제공/요구 |
|---|---|---|
| U4 → U1 (호출) | `FileStorageService.store/load/delete` | U1 제공(delete 포함 — U1 §7에서 확립) |
| U4 → U2 (호출) | `SessionService.markVerified(sessionId)` | U2 제공(U2 §8 계약) |
| U4 → U2 (읽기) | Session·Cohort 조회(권한·진도). **요구: Session 테이블 `(cohort_id, seq)` 인덱스**(진도 조회 ≤300ms 목표용) — U2가 `scalability-design.md` §2에서 이미 선언 | U2 제공 |
| U5 → U4 (읽기) | 회차 인증 상태(수료 판정) | U4 제공 |
| U6 → U4 (읽기) | 증빙 이력·출석 집계 | U4 제공 |

- **순환 부재**: U4는 U5를 호출하지 않는다(U5가 U4 데이터를 읽어 수료 판정). U4·U3는 병렬 유닛, 상호 직접 의존 없음(둘 다 U2 읽음). DAG U1→U2→(U3∥U4)→U5→U6 유지.

## 3. 실패 도메인 & Blast Radius

| 실패 지점 | Blast Radius | 완화 |
|---|---|---|
| 파일 store 실패 | 해당 업로드 요청만 | 400/500, TX 진입 안 함(부작용 없음) |
| 업로드 트랜잭션 롤백 | 해당 요청만 | 보상 delete로 고아 파일 제거 |
| 보상 delete 실패 | 고아 파일 1건(정합성 무관) | ERROR 로그 → 수동 정리(파일럿 수용) |
| 파일 볼륨 장애 | 업로드/다운로드만(Important 티어) | 예외 격리, 나머지 기능 지속. 볼륨 백업으로 복구 |
| DB 다운 | 전체(공유 리소스) | U1과 동일 |

## 4. 공유 리소스 & 상속

- **상속(U1)**: SecurityConfig, GlobalExceptionHandler, DTO/OpenAPI, Flyway, **FileStorageService(store/load/delete)**.
- **상속/의존(U2)**: `SessionService.markVerified` 회차 인증 전이 경로.
- **U4가 제공하는 계약**: 회차 인증 상태·증빙 이력 read-only(U5 수료 판정·U6 이력/집계).
- **공유 리소스**: PostgreSQL(AttendanceEvidence 스키마), 파일 볼륨(증빙 본체 — U4가 주 사용처). 파일 볼륨은 백업 대상(`reliability-design.md` §4).

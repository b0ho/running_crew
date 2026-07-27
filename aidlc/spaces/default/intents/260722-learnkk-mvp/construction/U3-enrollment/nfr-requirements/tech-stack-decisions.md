# Tech Stack Decisions — U3 enrollment (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U3-enrollment
> 리드 architect · 관점 devsecops·quality
> 상위 입력: `U3-enrollment/functional-design/business-logic-model.md`(join 동시성), `business-rules.md`(R-U3-07~10 락), `requirements-analysis/requirements.md`(NFR-1/6)

## 1. 상속 스택
U1-foundation 표준 스택 상속(Spring Boot·JPA·RDB·React·Docker·GitHub Actions·JUnit5/Testcontainers). 신규 프레임워크 없음.

## 2. U3 고유 기술 선택
| 항목 | 선택 | 근거 |
|---|---|---|
| 동시성 제어 | JPA `LockModeType.PESSIMISTIC_WRITE`(SELECT ... FOR UPDATE) | R-U3-07 정원 직렬화 |
| 중복 방지 | DB UNIQUE(cohortId, menteeId) | R-U3-08 최종 방어선 |
| 관리자 동시 승인 | `@Version`(낙관적 락) 또는 조건부 UPDATE | R-U3 승인 경합 방지 |
| 동시성 테스트 | ExecutorService + CountDownLatch + Testcontainers | R-U3-10 정원 경계 검증 |
| 트랜잭션 격리 | READ_COMMITTED(행 락이 정합성 보장) | R-U3-07b |

## 3. 보류/확장
- U1 보류 항목 상속. 대기열 자동 승격·재신청은 파일럿 범위 외(`cid:user-stories:c4`).

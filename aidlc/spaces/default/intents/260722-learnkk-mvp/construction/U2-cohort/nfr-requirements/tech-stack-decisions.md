# Tech Stack Decisions — U2 cohort (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 관점 devsecops·quality
> 상위 입력: `U2-cohort/functional-design/business-logic-model.md`(CohortService·상태전이), `business-rules.md`(R-U2-*), `requirements-analysis/requirements.md`(NFR-1/7)

## 1. 상속 스택 (U1 확립 기준)
U2는 U1-foundation의 tech-stack-decisions에서 확립한 프로젝트 표준 스택을 **그대로 상속**한다: Spring Boot 3.x + Spring Data JPA + RDB(PostgreSQL) + Flyway, React + Tailwind, springdoc-openapi, 공통 에러 DTO, Docker, GitHub Actions, JUnit5/MockMvc/Testcontainers/Jest. 별도 신규 기술 도입 없음.

## 2. U2 고유 기술 선택
| 항목 | 선택 | 근거 |
|---|---|---|
| 목록/검색 페이지네이션 | Spring Data `Pageable`(기본 20건) | R-U2-19 목록 조회 |
| 코호트+회차 원자 생성 | 단일 `@Transactional` 벌크 insert | W-U2-1(회차 N건 생성) |
| 상태 ENUM | JPA `@Enumerated(STRING)` (모집중/진행중/종료됨) | 가독성·마이그레이션 안전 |
| 외부 링크 검증 | URL 형식 검증(http/https) | R-U2-17 |

## 3. 보류/확장
- U1과 동일 보류 항목(클라우드·TLS·다중 인스턴스) 상속. U2 신규 보류 없음.

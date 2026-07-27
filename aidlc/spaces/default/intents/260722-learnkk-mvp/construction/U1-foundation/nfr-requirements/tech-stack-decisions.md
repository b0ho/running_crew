# Tech Stack Decisions — U1 foundation (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 관점 devsecops·quality
> 상위 입력: `U1-foundation/functional-design/business-logic-model.md`, `business-rules.md`, `requirements-analysis/requirements.md`(NFR-1/7)
> 근거 관행: team.md(FE/BE 분리·GitHub Actions·Docker recreate·Google Java Format·Prettier/ESLint), project.md ## Tech Stack(세션 인증), Mandated(DTO·BCrypt·CI 린트+테스트)

U1은 공통 인프라 유닛이므로 프로젝트 전반의 기술 스택을 확립한다. 이후 유닛은 이 스택을 상속한다.

## 1. 백엔드
| 항목 | 선택 | 근거 |
|---|---|---|
| 언어/런타임 | Java 17+ / Spring Boot 3.x | team.md Spring, 성숙한 생태계 |
| 보안 | Spring Security + BCrypt | Mandated BCrypt, 검증된 인증(feasibility 결정) |
| 인증 방식 | 서버 세션(HttpOnly 쿠키) | project.md ## Tech Stack(JWT는 확장 시) |
| 영속 | Spring Data JPA + RDB(PostgreSQL 권장) | 관계형 도메인, 트랜잭션·락(U3 동시성) |
| 마이그레이션 | Flyway | 스키마 버전·관리자 시드(R-U1-25) |
| API 문서 | springdoc-openapi | Mandated OpenAPI 자동생성 |
| 에러 처리 | @RestControllerAdvice + 공통 에러 DTO | team.md Code Style |
| 포매터 | Google Java Format | team.md |

## 2. 프론트엔드
| 항목 | 선택 | 근거 |
|---|---|---|
| 프레임워크 | React | team.md, NFR-1 |
| 스타일 | Tailwind 경량 커스텀 디자인 시스템 | `cid:refined-mockups:c1`(기성 UI 라이브러리 미도입) |
| HTTP | 중앙 API 클라이언트(axios/fetch) 래퍼 | 에러 정규화, credentials:include |
| 린트/포맷 | ESLint + Prettier | team.md |

## 3. 저장소·인프라
| 항목 | 선택 | 근거 |
|---|---|---|
| 저장소 토폴로지 | FE/BE 분리 저장소 | team.md, OpenAPI 계약 동기화 |
| 컨테이너 | Docker(FE 1 + BE 1) | 로컬 배포(NFR-2), recreate 전략 |
| CI | GitHub Actions | team.md, 머지 전 린트+테스트(Mandated) |
| 파일 저장 | 로컬 볼륨(웹루트 밖) | FileStorageService(U1) |

## 4. 테스트 스택
| 항목 | 선택 | 근거 |
|---|---|---|
| 백엔드 단위/통합 | JUnit 5 + MockMvc + Testcontainers | team.md, 실 DB 트랜잭션·락 검증 |
| 프론트 | Jest + React Testing Library | team.md |
| 동시성 테스트 | ExecutorService + CountDownLatch | team.md(U3 정원 경계) |

## 5. 보류/확장 항목
- 퍼블릭 클라우드, TLS 종단, 파일 스캔, SCA, 다중 인스턴스/세션 스토어(또는 JWT), 고급 배포(blue-green) — 확장 후속 과제(Forbidden/Scope Overrides 정합).

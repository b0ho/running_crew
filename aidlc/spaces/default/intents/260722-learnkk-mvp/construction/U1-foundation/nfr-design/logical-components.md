# Logical Components — U1 foundation (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 서포트 aws-platform(경계·인프라 매핑)
> 상위 입력: `nfr-requirements/tech-stack-decisions.md`(스택·토폴로지), `nfr-requirements/security-requirements.md`·`performance-requirements.md`·`scalability-requirements.md`·`reliability-requirements.md`(NFR 적용 지점), `functional-design/business-logic-model.md`(W1~W8 워크플로)
> 목적: NFR 설계 결정이 적용되는 **논리 컴포넌트 인벤토리** — 서비스 경계, 실패 도메인, blast radius, 공유 리소스. Infrastructure Design(3.4)으로 넘어가는 컴포넌트 수준 뷰.

## 1. 논리 컴포넌트 인벤토리

`tech-stack-decisions.md`(FE/BE 분리 저장소·Docker)와 `business-logic-model.md`(W1~W8)를 컴포넌트로 정리한다. U1은 kind=service이며 인증 UI를 포함한다.

| 컴포넌트 | 종류 | 책임 | 적용 NFR 설계 |
|---|---|---|---|
| `learnkk-web`(React) | UI/배포단위 | 인증 UI(AuthPage/Signup/Login), 중앙 API 클라이언트 | 성능(번들·CSR 라우팅), 보안(credentials 쿠키) |
| `learnkk-api`(Spring) | Service/배포단위 | 인증·인가·에러·파일 저장 골격 | 전 NFR(인증·세션·풀·에러) |
| `SecurityConfig` | 모듈 | 필터체인·`@EnableMethodSecurity`·세션 고정 방지·PasswordEncoder 빈 | 보안 §1·§2 |
| `AuthService` | 컴포넌트 | signup/login 도메인 로직 | 성능(BCrypt 예산)·보안(열거 방지) |
| `UserRepository` | 컴포넌트 | User 영속·email UNIQUE 조회 | 성능(인덱스)·확장(경쟁 안전) |
| `GlobalExceptionHandler` | 컴포넌트 | `@RestControllerAdvice` 공통 에러 정규화 | 신뢰성 §2 |
| `FileStorageService` | 라이브러리/컴포넌트 | 웹루트 밖 저장·검증·경로이탈 방지·멱등 delete | 보안 §6·신뢰성(보상)·확장(백엔드 교체점) |
| `Flyway 마이그레이션 + 관리자 시드` | 모듈 | 스키마 버전·멱등 시드·부팅 검증 | 신뢰성 §2·§3, 보안 §4 |
| PostgreSQL | 데이터스토어(공유 리소스) | 계정 영속 | 성능(풀)·신뢰성(백업) |
| 로컬 파일 볼륨 | 스토리지(공유 리소스) | 첨부 파일 | 보안(웹루트 밖)·확장(공유 스토리지 교체점) |

## 2. 서비스 경계 & 컴포넌트 격리

```
+----------------------------------------------------------+
|  learnkk-web (React, Docker 컨테이너 1)                   |
|   AuthPage / SignupForm / LoginForm / API Client(래퍼)    |
+----------------------------------------------------------+
        | HTTPS(확장) / HTTP(파일럿), credentials: include
        v
+----------------------------------------------------------+
|  learnkk-api (Spring Boot, Docker 컨테이너 1)             |
|   [SecurityConfig 필터체인]                               |
|     -> AuthController -> AuthService -> UserRepository     |
|     -> GlobalExceptionHandler(횡단)                        |
|     -> FileStorageService --------> [로컬 파일 볼륨]        |
|   [Flyway 마이그레이션 + 관리자 시드](부팅 시)             |
+----------------------------------------------------------+
        |                                    
        v                                    
+------------------+                         
|  PostgreSQL      |  (Docker 컨테이너 / 로컬 볼륨)          
+------------------+                         
```
<!-- Text fallback: React 웹 컨테이너가 세션 쿠키를 포함해 Spring API 컨테이너를 호출한다. API는 SecurityConfig 필터체인을 거쳐 AuthController→AuthService→UserRepository로 흐르고, GlobalExceptionHandler가 횡단 에러를 처리하며, FileStorageService는 로컬 파일 볼륨에, 데이터는 PostgreSQL에 영속한다. Flyway가 부팅 시 스키마·관리자 시드를 적용한다. -->

- **경계 원칙**: API 경계는 DTO로만 소통(JPA Entity 미노출, Mandated). FE/BE는 분리 저장소·독립 배포, OpenAPI 계약으로 동기화(team.md).
- **레이어 격리**: Controller(HTTP)–Service(도메인)–Repository(영속) 단방향 의존. 에러는 도메인 예외로 상향 전파 후 advice가 HTTP 매핑.

## 3. 실패 도메인 & Blast Radius

| 실패 지점 | Blast Radius | 완화 |
|---|---|---|
| `learnkk-api` 다운 | 전체 서비스(단일 인스턴스) | recreate 재기동, 헬스체크(신뢰성 §4). HA는 확장 |
| PostgreSQL 다운 | 인증·데이터 전면 불가(Critical) | 일 1회 스냅샷 백업, 재기동 복구 |
| 파일 볼륨 장애 | 파일 저장/조회만(Important) | 예외 격리, 나머지 기능 지속 |
| Flyway/시드 실패 | 기동 자체 중단(fail-fast) | 부팅 검증으로 조기 노출(보안 §4) |
| springdoc UI 장애 | 문서만(Nice to have) | 서비스 영향 없음 |

- 파일럿은 단일 인스턴스라 대부분의 blast radius가 "전체"로 수렴 → 이것이 HA를 확장 과제로 남기는 근거이자 확장 트리거(`scalability-design.md` §5)의 동기.

## 4. 공유 리소스 & 후속 유닛 계약

- **U1이 확립하는 공유 골격(후속 유닛 상속)**: SecurityConfig(인증·인가·세션·메서드 보안), GlobalExceptionHandler(에러 DTO 계약), FileStorageService(store/load/delete 계약), Flyway 시드 규약, DTO/OpenAPI 규약.
- **공유 리소스**: PostgreSQL(전 유닛 공유 스키마), 파일 볼륨(U4 증빙·U5 보고서/증서 사용).
- **확장 교체점(격리 지점)**: `FileStorageService`(공유 스토리지 교체), 세션 스토어(외부화/JWT) — `scalability-design.md` §5 트리거와 연동. 이 격리로 확장 시 상위 유닛 코드 변경 없이 백엔드만 교체 가능.

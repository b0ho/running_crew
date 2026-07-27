# Design Decisions (ADR) — LearnKK (파일럿)

> Inception · application-design 단계 산출물 · 주요 설계 결정 기록
> 상위 입력: `practices-discovery/team-practices.md`, `requirements-analysis/requirements.md`, project.md 학습

## ADR-1 아키텍처 스타일
- **결정**: React SPA(FE) + Spring Boot REST(BE), FE/BE 분리 저장소, 단일 모듈 레이어드(controller/service/repository/dto/domain).
- **근거**: team-practices; 파일럿 규모에 마이크로서비스는 과함. springdoc-openapi로 계약 문서화.

## ADR-2 선착순/정원 동시성
- **결정**: Enrollment (cohortId, menteeId) 유니크 제약 + 정원 확인 시 비관적 락(`SELECT ... FOR UPDATE`).
- **근거**: 정원 초과 확정 방지가 핵심 정합성 리스크(FR-3). 낙관적 락 대비 경쟁 상황에서 단순·확실. Testcontainers 동시성 테스트로 검증.

## ADR-3 DTO 경계
- **결정**: API 경계에서 요청/응답 DTO 사용, JPA Entity 직접 노출 금지.
- **근거**: team-practices 하드 제약. 계약 안정성·과다 노출 방지.

## ADR-4 파일 저장
- **결정**: 로컬 서버 파일시스템(웹루트 밖 볼륨)에 증빙·보고서·수료증 저장, 경로를 DB에 기록.
- **근거**: 로컬 Docker 호스팅(NFR-2). 파일 보안 검증(MIME/크기)은 기능 요구 수준 적용; 심층 스캔은 파일럿 보류(project.md).

## ADR-5 수료·정산 판정 트리거
- **결정**: 멘토의 "코호트 종료" 액션 시 CompletionService가 일괄 판정(온디맨드, 배치 아님).
- **근거**: 명확한 트리거 시점(US-4/12/13). 소규모라 실시간/배치 불필요.

## ADR-6 인증·보안
- **결정**: Spring Security + BCrypt 비밀번호 해싱. 파일럿은 평문 HTTP 허용(TLS·SSO·스캔은 확장 시).
- **근거**: feasibility/practices 결정. 최소 보안 하드 제약(BCrypt)만 적용.

## ADR-7 배포
- **결정**: Docker 컨테이너(recreate 재배포), GitHub Actions CI(린트+테스트 그린 게이트), git SHA 버저닝.
- **근거**: team-practices Deployment.

## ADR-8 RBAC 시드 메커니즘
- **결정**: 최초 관리자 계정은 **DB 마이그레이션(Flyway `V*__seed_admin.sql`)** 으로 부트스트랩한다(`isAdmin=true`, 시드 이메일은 환경설정으로 주입, 비밀번호는 BCrypt 해시로 시드). 회원가입 경로로는 관리자 권한 부여 불가.
- **근거**: US-0 RBAC 선행. 재현 가능·버전관리되는 시드가 수동 SQL보다 안전. 시드 계정 크리덴셜은 `.env` 외부화(비밀 소스에 커밋 금지).

## ADR-9 테스트 전략
- **결정**: 핵심 도메인(EnrollmentService.join, AttendanceService, CompletionService, MetricsService) 80% 커버리지 목표; **Testcontainers**로 트랜잭션·비관적 락 검증; **ExecutorService + CountDownLatch**로 정원 동시성(N/N+1) 테스트; 전역 머지 게이트는 린트+테스트 그린; CRUD성은 스모크.
- **근거**: team-practices Testing Posture. 설계 문서만 보는 개발자에게 테스트 요구를 명시.

## 잔여 리스크

- 파일 업로드 심층 검증 보류(확장 시 재검토).
- FE/BE 분리 저장소의 OpenAPI 계약 동기화 준수 모니터링 필요.

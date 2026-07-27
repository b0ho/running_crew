# 증거 기록

> 파이프라인 배포 리드(aidlc-pipeline-deploy-agent)가 검사·추론하고 인터뷰로 확정한 내용. Greenfield 프로젝트이므로 코드/git 증거는 아직 존재하지 않음.

## 검사한 소스

### 1. `aidlc/spaces/default/memory/org.md`
- **Way of Working**: trunk-based dev, squash-merge Bolts, 단기 feature 브랜치(1~2일)
- **Walking Skeleton**: `skeleton: on` 시에만 Bolt 1 실행, ladder prompt로 나머지 Bolt 게이트 또는 자율 선택
- **Testing Posture**: 스코프별 기본값(mvp/enterprise/feature/infra는 80% 커버리지, 테스트 CI 머지 전 실행)
- **Deployment**: staging에 머지 시 배포, production은 수동 승인 게이트
- **Code Style**: 프로젝트 루트 Prettier/ESLint 설정 따름, 언어 관용적 네이밍

### 2. `aidlc/spaces/default/memory/project.md`
- **Tech Stack**: React + Spring (풀스택 4~6명 팀)
- **Deployment 특화**: 초기 로컬 서버 호스팅, 퍼블릭 클라우드 보류(확장 후속 과제)
- **Authentication**: 자체 계정(이메일/사번), 사내 SSO 미연동(출시 속도 우선)
- **Team Composition**: 전원 풀스택·풀타임, 전담 디자인/PM 없음 → 파일럿 UI 단순 유지
- **Scope Principle**: 범위를 좁게 유지하여 유지보수 부담 완화
- **Privacy**: 최소 개인정보 수집(이메일·성명·닉네임; 사번 미수집)

### 3. `aidlc/spaces/default/memory/team.md`
- 현재 비어있음(practices-discovery가 이 스테이지에서 채움)
- 한글 소통 원칙 확정됨: "모든 질문·요약·소통 산출물은 한글로 작성·제시한다."

### 4. Greenfield 상태
- 이 프로젝트는 greenfield이므로:
  - git 히스토리 없음(코드 첫 커밋 전)
  - CI/CD 파이프라인 설정 없음(이 스테이지의 후속 construction 단계에서 구축 예정)
  - 브랜치 전략 증거 없음(org.md 기본값과 project.md 결정사항을 기준으로 추론)
  - 테스트 스위트 없음(코드 생성 단계에서 80% 커버리지 타겟으로 작성 예정)

## 인터뷰 확정 사항

### Q1: 저장소 토폴로지 (인간 결정: B = 분리 저장소)
프론트엔드(React)와 백엔드(Spring)를 **별도 저장소로 분리**한다. 각 저장소는 독립적인 CI/CD 파이프라인과 버저닝을 유지한다.

**트레이드오프 기록:** developer 스포크는 monorepo를 권고했으나(4~6명 풀스택 팀·트렁크 기반 개발·squash-merge Bolt와 정합성 높음), 인간은 분리 저장소를 선택했다. API 계약 변경 시 FE/BE 조율 오버헤드가 발생하므로 OpenAPI 스펙 또는 계약 문서를 통한 명시적 동기화가 필수다. 이 트레이드오프는 팀이 명시적으로 수용했다.

### Q2: Construction Autonomy Mode (인간 결정: A = 매 Bolt 게이트)
Bolt 1(skeleton) 출하 후 나머지 Bolt(2~N)는 **매 Bolt 게이트**로 실행한다. 각 Bolt 완료 후 오케스트레이터가 다음 Bolt 실행 여부를 사용자에게 묻고, 사용자 승인 후 다음 Bolt로 진행한다. 파일럿 단계에서 매 Bolt 출력을 검증하고 방향을 조정하기 위함.

### Q3: Walking Skeleton (인간 결정: A = ON)
LearnKK 파일럿은 **walking skeleton을 활성화**한다. Bolt 1은 코호트 개설 → 선착순 참여 1명 → 출석 인증 기본 플로우를 관통하는 첫 슬라이스를 배포하고, 사용자는 skeleton의 배포 결과를 검증한 후 나머지 Bolt 실행을 승인한다.

### Q4: 커버리지 정책 (인간 결정: A = 핵심 도메인 80% 목표, 전역 게이트는 린트+테스트 그린)
**핵심 도메인 로직(선착순 참여·정원 마감, 출석 인증, 집계, 인증/인가)**에 대해 80% 라인 커버리지 목표를 유지한다. DTO·getter/setter·설정·단순 UI 마크업은 커버리지 요구에서 제외한다.

**전역 머지 게이트는 "린트 + 테스트 그린"까지**. 80%는 획일적 하드 게이트가 아니라 핵심 모듈에 적용되는 팀 목표치로 운영한다. quality 스포크의 권고를 반영: 파일럿 속도와 품질의 균형을 위해 위험 기반 차등 커버리지 적용.

### Q5: 동시성 제어 전략 (인간 결정: A = DB 유니크 제약 + 비관적 락)
선착순 참여/정원 동시성은 **DB 유니크 제약 + 비관적 락(`SELECT ... FOR UPDATE`)**으로 구현한다. 동시성 테스트(`ExecutorService` + `CountDownLatch`)를 작성해 정원 N에 대해 동시 요청 M(>N)이 정확히 N명만 확정·초과분은 거부되는지 검증한다.

### Q6: 백엔드 통합 테스트 인프라 (인간 결정: A = Testcontainers)
백엔드 통합 테스트는 **Testcontainers**를 사용해 실제 DB 트랜잭션·락 경합을 검증한다. H2보다 프로덕션 환경 근접성이 높아 정합성 신뢰도를 확보한다.

### Q7: E2E/스모크 범위 (인간 결정: A = 핵심 플로우 스모크 1~2개만)
파일럿에서는 전면 E2E 보류. 핵심 사용자 플로우(로그인 → 코호트 참여 → 출석 인증) **스모크 테스트 1~2개**만 작성한다.

### Q8: CI 도구 (인간 결정: A = GitHub Actions)
**GitHub Actions**를 CI 도구로 사용한다. 로컬 서버 배포 환경과 호환되며, 무료 티어에서 충분히 운영 가능하다.

### Q9: 로컬 인스턴스 관리 (인간 결정: A = Docker 컨테이너 + recreate 재배포)
Staging/production 인스턴스를 **Docker 컨테이너** 방식으로 관리하고, **recreate 재배포** 전략(기존 컨테이너 정지 후 새 이미지로 재시작)을 사용한다. 파일럿에 적합한 단순 전략이다.

### Q10: Artifact 버저닝 (인간 결정: A = git SHA)
**git SHA**를 artifact 버전으로 사용한다. semver/빌드 번호는 확장 시 도입 검토. 트렁크 기반 개발과 정합적이며 추적이 명확하다.

### Q11: 에러 핸들링 (인간 결정: A = @RestControllerAdvice + 공통 에러 DTO)
백엔드(Spring) 에러 핸들링은 **`@RestControllerAdvice` + 공통 에러 DTO**(필드: `code`, `message`, `timestamp`, `path`)로 일관된 에러 응답을 제공한다. 서비스 레이어는 도메인 예외를 던지고, controller advice가 HTTP 상태로 매핑한다. `Result<T,E>` 커스텀 패턴은 도입하지 않음(파일럿 규모에 과설계).

### Q12: DTO/Entity 분리 (인간 결정: A = ALWAYS API 경계 DTO 사용)
**ALWAYS API 경계에서 요청/응답 DTO를 사용하고, JPA Entity를 직접 노출하지 않는다.** 순환 참조·과다 노출·계약 결합 방지. 이는 하드 제약으로 discovered-rules.md `Mandated`에 반영됨. developer 스포크 권고 수용.

### Q13: Code Style 상세 (인간 결정: A = springdoc-openapi + Google Java Format + ESLint 팀 커스터마이징 + named exports)
- **API 문서화:** springdoc-openapi를 사용해 OpenAPI 스펙을 코드에서 자동 생성
- **Java 포맷터:** Google Java Format 사용
- **React:** Prettier + ESLint(팀 커스터마이징) + Named exports 기본(프레임워크가 default를 요구하는 경우만 예외)

### Q14: CI 보안 스캔 (인간 결정: B = 넣지 않음)
파일럿 단계에서 CI 보안 스캔(secret scanning, SCA, lockfile 게이트)을 **넣지 않는다**. 확장 시 도입으로 보류. devsecops 스포크의 권고(gitleaks, npm audit, OWASP Dependency-Check)는 파일럿에서 채택하지 않음.

**잔여 보안 리스크 기록:** 이에 따라 자격증명 유출 감지·의존성 취약점 스캔이 부재하며, 확장 시 재검토 필요. 단, 기본 보안 위생(비밀번호 해싱, 파일 업로드 검증 — 아래 참조)은 유지한다.

### Q15: 보안 최소화 (인간 결정: B = 비밀번호 BCrypt 해싱만 하드 제약)
보안을 최소화한다. **비밀번호 BCrypt 해싱(평문 저장 금지)**만 하드 제약으로 확정(discovered-rules.md `Mandated` 및 `Forbidden`에 반영).

파일 업로드 MIME/크기 검증, 안티바이러스 스캔, PII 전송 구간 보호 등은 파일럿 하드 제약에서 제외(보류). devsecops 스포크의 파일 업로드 검증 권고("ALWAYS 업로드 파일은 유형·크기 검증, 원본 파일명으로 저장 경로 구성 금지, 웹 루트 밖 저장")는 채택하지 않음.

**잔여 보안 리스크 기록:** 파일 업로드 경로 조합·MIME 검증·크기 상한 부재로 path traversal, 과다 업로드, 악성 파일 실행 위험이 존재한다. 확장 시 재검토 필요.

### Q16: TLS (인간 결정: B = 평문 HTTP 허용)
파일럿 단계에서 **평문 HTTP를 허용**한다. TLS(자체 서명 인증서 포함)는 확장 시 도입. devsecops 스포크의 TLS 권고는 파일럿에서 채택하지 않음.

**잔여 보안 리스크 기록:** 로그인·PII 엔드포인트를 포함한 모든 전송 구간이 평문이므로 도청 위험이 존재한다. 확장 시 재검토 필요.

## 추론된 실무 관행 (인터뷰 후 최종 통합)

### Branching Strategy
- **트렁크 기반 개발** 확정(org.md 기본값, project.md에서 이의 없음)
- Bolt 브랜치는 `main`에서 분기, squash-merge로 `main` 복귀
- 장수명 브랜치 금지(1~2일 내 머지)
- **저장소 토폴로지:** FE/BE 분리 저장소(Q1 인간 결정)

### Testing Approach
- **도구:** Jest/RTL + JUnit 5 + MockMvc + Testcontainers
- **커버리지:** 핵심 도메인 80% 목표, 전역 게이트는 린트+테스트 그린(Q4)
- **동시성:** DB 유니크 제약 + 비관적 락, 동시성 테스트 작성(Q5)
- **통합 테스트:** Testcontainers로 실 DB 검증(Q6)
- **E2E:** 핵심 플로우 스모크 1~2개(Q7)
- 풀스택 팀이므로 개발자가 단위·통합 테스트 작성 책임

### Deployment Pipeline
- **CI 도구:** GitHub Actions(Q8)
- **로컬 인스턴스:** Docker 컨테이너 + recreate 재배포(Q9)
- **Artifact 버저닝:** git SHA(Q10)
- Staging: 머지 시 자동 재배포
- Production: 수동 승인 후 배포
- 파일럿이므로 blue-green/canary 같은 고급 전략 보류

### Code Style
- **React:** Prettier + ESLint(팀 커스터마이징) + Named exports 기본, 중앙 API 래퍼 에러 핸들링
- **Spring:** Google Java Format + @RestControllerAdvice + 공통 에러 DTO, DTO/Entity 분리 하드 제약(Q11, Q12)
- **API 문서화:** springdoc-openapi 자동 생성(Q13)

### Walking Skeleton
- **ON 확정(Q3).** Bolt 1은 코호트 개설 → 선착순 참여 1명 → 출석 인증 기본 플로우 슬라이스.
- Bolt 1 후 나머지는 매 Bolt 게이트(Q2).

## 플래그된 리스크 및 불확실성

### 1. 분리 저장소 vs monorepo 트레이드오프 (Q1 결정)
**리스크:** developer 스포크는 monorepo를 권고했으나(4~6명 풀스택 팀·트렁크 기반 개발·squash-merge Bolt와 정합성 높음), 인간은 FE/BE 분리 저장소를 선택했다. API 계약 변경 시 FE/BE 조율 오버헤드(별도 PR, 버전 동기화, 하나의 Bolt가 두 레포에 걸쳐 작업)가 발생한다. 이는 의도적 트레이드오프이며 팀이 수용했지만, construction 단계에서 FE/BE 병렬 개발 시 재작업 가능성이 있다. OpenAPI 스펙 또는 계약 문서 동기화가 실제로 지켜지는지 모니터링 필요.

### 2. 파일럿 보안 스캔·검증 보류에 따른 잔여 보안 리스크 (Q14, Q15, Q16 결정)
**리스크:** devsecops 스포크가 권고한 다음 통제를 파일럿에서 보류했다:
- **CI 보안 스캔 미도입(Q14):** secret scanning(gitleaks), SCA(npm audit/OWASP Dependency-Check), lockfile 게이트 부재 → 자격증명 유출 감지 없음, 의존성 취약점 노출 가능.
- **파일 업로드 검증 미도입(Q15):** MIME/크기/path traversal 방어 부재 → 악성 파일 실행·과다 업로드·경로 조합 취약점 노출 가능.
- **TLS 미도입(Q16):** 평문 HTTP로 로그인·PII 전송 → 도청 위험.

파일럿 단계에서는 출시 속도 우선으로 의도적 보류했으나, 확장(클라우드 이관, 전사 도입) 시 재검토 필수. 특히 출석 증빙 파일 첨부(project.md `cid:scope-definition:c2`, `cid:rough-mockups:c2`)는 신뢰 경계 밖 입력이므로 확장 전 검증 계층 추가 필요.

**완화된 하드 제약:** 비밀번호 BCrypt 해싱만 discovered-rules에 강제(Q15) — 최소 보안 위생 유지.

## 검사 시점
- 검사 시각: 2026-07-24T14:10:52Z
- 인터뷰 확정 시각: 2026-07-24T15:22:17Z
- 커밋: 8a76caef6888ba77a3bb6ade1fce6c6ad8b548f3
- 프로젝트 상태: Greenfield, IDEATION 완료, INCEPTION 진입

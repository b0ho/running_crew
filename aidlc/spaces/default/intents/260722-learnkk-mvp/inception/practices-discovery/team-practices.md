# 팀 실무 관행

> LearnKK 파일럿 팀의 확정된 실무 관행. `org.md` 기본값을 프로젝트 맥락에 맞게 특화함.

## Way of Working

우리는 **트렁크 기반 개발(trunk-based development)**을 사용한다. 모든 작업은 단기 feature 브랜치를 통해 `main`에 머지된다(통상 1~2일 이내 해소). 장수명 브랜치는 머지 부채를 누적시키므로 피한다.

**저장소 토폴로지:** 프론트엔드(React)와 백엔드(Spring)를 **별도 저장소로 분리**한다. 각 저장소는 독립적인 CI/CD 파이프라인과 버저닝을 유지한다. API 계약 변경 시 FE/BE 조율이 필요하므로, OpenAPI 스펙 또는 계약 문서를 통해 명시적으로 동기화한다.

Construction 워크트리의 base 브랜치는 `main`이고 머지 타겟도 `main`이다.

Bolt 브랜치는 **squash-merge**로 `main`에 머지한다. 각 Bolt는 `main` 위에서 Bolt 슬러그명의 단일 커밋이 되며, Bolt의 전체 커밋 히스토리는 워크트리 파기 전까지 소스 브랜치에 보존된다.

Squash는 `main`의 선형 이력을 깔끔하게 유지하며 delivery-planning의 Bolt 시퀀스와 1:1 대응되게 한다. 중간 커밋이 `main`에서 사라지는 트레이드오프가 있지만, 감사 로그가 전체 이벤트 시퀀스를 보존하므로 수용한다.

## Walking Skeleton

**LearnKK 파일럿은 walking skeleton을 활성화한다.** Bolt 1은 단독으로 실행되며 게이트된다: 코호트 개설 → 선착순 참여 1명 → 출석 인증 기본 플로우를 관통하는 첫 슬라이스를 배포하고, 사용자는 skeleton의 배포 결과를 검증한 후 나머지 Bolt 실행을 명시적으로 승인한다.

**Bolt 1 출하 후 construction autonomy mode:** 나머지 Bolt(Bolt 2~N)는 **매 Bolt 게이트**로 실행한다. 각 Bolt 완료 후 오케스트레이터가 다음 Bolt 실행 여부를 사용자에게 묻고, 사용자 승인 후 다음 Bolt로 진행한다. 이는 파일럿 단계에서 매 Bolt의 출력을 검증하고 방향을 조정할 수 있게 한다.

## Testing Posture

모든 Bolt에서 테스트를 일급 산출물로 취급한다. 구체적 방법론(TDD, BDD, ATDD, 고전 test-after)은 testing-strategy 스테이지가 제공될 때 캡처된다.

**LearnKK 파일럿 특화:**
- **도구:** 프론트엔드는 Jest/React Testing Library, 백엔드는 JUnit 5 + MockMvc + Testcontainers 사용
- **커버리지 목표:** 핵심 도메인 로직(선착순 참여·정원 마감, 출석 인증, 집계, 인증/인가)에 대해 **80% 라인 커버리지 목표**. DTO·getter/setter·설정·단순 UI 마크업은 커버리지 요구에서 제외.
- **품질 게이트:** 전역 머지 게이트는 **린트 + 테스트 그린**까지. 80%는 획일적 하드 게이트가 아니라 핵심 모듈 팀 목표치로 운영.
- **동시성 제어:** 선착순 참여/정원 동시성은 **DB 유니크 제약 + 비관적 락(`SELECT ... FOR UPDATE`)**으로 구현하고, 동시성 테스트(`ExecutorService` + `CountDownLatch`)를 작성해 정원 경계를 검증한다.
- **통합 테스트:** 백엔드 통합 테스트는 **Testcontainers**를 사용해 실제 DB 트랜잭션·락 경합을 검증한다.
- **E2E/스모크:** 파일럿에서는 전면 E2E 보류. 핵심 사용자 플로우(로그인 → 코호트 참여 → 출석 인증) **스모크 테스트 1~2개**만 작성.
- 풀스택 4~6명 구성이므로 전담 QA 없음 → 개발자가 단위·통합 테스트 작성 책임

## Deployment

**staging 환경으로는 머지 시 배포(deploy on merge)** 한다. Production 배포는 별도의 수동 승인 게이트를 둔다.

**LearnKK 파일럿 특화:**
- **배포 환경:** 초기는 **로컬 서버**에 배포(퍼블릭 클라우드 보류). 클라우드 이관은 확장 후속 과제.
- **CI 도구:** GitHub Actions를 사용한다.
- **로컬 인스턴스 관리:** Docker 컨테이너 방식으로 staging/production 인스턴스를 관리하고, **recreate 재배포** 전략(기존 컨테이너 정지 후 새 이미지로 재시작)을 사용한다.
- **Artifact 버저닝:** git SHA를 artifact 버전으로 사용한다. semver/빌드 번호는 확장 시 도입 검토.
- Production: 로컬 프로덕션 인스턴스로 수동 승인 후 배포
- 파일럿이므로 blue-green/canary 같은 고급 전략은 보류

## Code Style

프로젝트 루트 설정을 따른다:
- Formatter/Linter: 머지 전 CI에서 실행; 실패 시 PR 블럭.
- Naming conventions: 언어 관용적 유지.

프레임워크가 코드 스타일 제안을 할 때, 에이전트는 먼저 프로젝트 린터 설정을 읽고, 린터가 이미 커버하지 않는 경우에만 에이전트의 제안을 발동한다.

**LearnKK 파일럿 특화:**

### React (프론트엔드)
- **Formatter/Linter:** Prettier + ESLint (팀 커스터마이징)
- **Exports:** Named exports 기본. 프레임워크가 default를 요구하는 경우만 예외.
- **에러 핸들링:** 중앙 API 클라이언트 래퍼(axios/fetch)에서 try-catch로 정규화, 컴포넌트 산발 try-catch 지양.
- **네이밍:** 컴포넌트 파일·심볼 `PascalCase`, 훅 `useXxx`, 비컴포넌트 유틸 `camelCase`.

### Spring (백엔드)
- **Formatter:** Google Java Format 사용.
- **에러 핸들링:** `@RestControllerAdvice` + 공통 에러 DTO(필드: `code`, `message`, `timestamp`, `path`)로 일관된 에러 응답 제공. 서비스 레이어는 도메인 예외를 던지고, controller advice가 HTTP 상태로 매핑. `Result<T,E>` 커스텀 패턴은 도입하지 않음.
- **API 문서화:** springdoc-openapi를 사용해 OpenAPI 스펙을 코드에서 자동 생성.
- **네이밍:** 클래스 `PascalCase`, 메서드/필드 `camelCase`, 패키지 소문자.

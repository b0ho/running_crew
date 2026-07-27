**Collaborator:** aidlc-developer-agent

> 렌즈: 네이밍 컨벤션 / 레이어 경계(React 컴포넌트 구조, Spring controller-service-repository) / 에러 핸들링 / 파일 조직 / 코드 스타일. 소규모 파일럿(풀스택 4~6명, greenfield)에 비례하도록 최소·실용 관점으로 검토함.

## Contribution

리드 초안(team-practices.md `## Code Style`, discovered-rules.md)을 개발자 관점에서 검토한 결과, 방향은 타당하나 다음 지점에서 구체성이 부족해 구현 단계 진입 전 확정이 필요하다.

**1. 레포 토폴로지가 미정 — 다른 모든 컨벤션의 선행 조건.**
React FE와 Spring BE를 monorepo(단일 저장소, 예: `frontend/`·`backend/`)로 둘지 별도 레포로 둘지가 초안에 없다. 4~6명 풀스택 팀·트렁크 기반 개발·squash-merge Bolt라는 확정 사항과 가장 정합적인 것은 **monorepo**다. 이유: (a) 하나의 Bolt가 FE+BE를 함께 건드리는 풀스택 작업이 잦고, (b) API 계약 변경 시 FE/BE를 원자적 커밋으로 묶을 수 있으며, (c) 별도 레포는 소규모 팀에 CI·버저닝·PR 조율 오버헤드만 추가한다. 이 결정이 `main` 브랜치 전략, CI 설정, 파일 조직을 모두 좌우하므로 인터뷰 최우선 항목으로 올린다.

**2. Named exports 선호 규칙은 좋으나 예외를 명시해야 한다.**
초안의 "Named exports 선호(default export 최소화)"에 동의한다. 다만 일부 도구(예: Next.js `page`/route 컴포넌트, React.lazy 대상)는 default export를 요구할 수 있으므로 "프레임워크가 요구하는 경우를 제외하고 named export"로 예외를 붙이는 편이 마찰이 적다. Vite+React SPA면 예외가 거의 없어 순수 named로 강제 가능.

**3. 에러 핸들링은 FE/BE를 나눠서 정해야 한다 — 초안은 한 줄로 뭉뚱그림.**
초안의 "try-catch 또는 `Result<T,E>` 패턴 고려"는 두 스택을 섞고 있다. 파일럿 규모에서 권장:
- **Spring(BE):** `Result<T,E>` 커스텀 패턴 도입은 소규모 파일럿에 과설계다. 관용적 예외 + `@RestControllerAdvice` 전역 예외 핸들러로 일관된 에러 응답(공통 에러 DTO: `code`/`message`/`timestamp`)을 매핑하는 것을 권장. 서비스 레이어는 도메인 예외를 던지고, controller advice가 HTTP 상태로 변환.
- **React(FE):** API 호출은 중앙 API 클라이언트(axios/fetch 래퍼)에서 try-catch로 잡아 사용자 메시지로 정규화하고, TanStack Query 등을 쓰면 그 error 상태를 UI에서 소비. 컴포넌트마다 흩뿌린 try-catch는 지양.

**4. Spring 레이어 경계와 DTO/Entity 분리는 discovered-rules에 하드 제약으로 넣을 가치가 있다.**
JPA Entity를 controller 응답으로 직접 노출하면 순환 참조·과다 노출·계약 결합 문제가 생긴다. 파일럿이라도 "Entity를 API 경계 밖으로 직접 반환하지 않고 요청/응답 DTO를 둔다"는 규칙은 저비용·고효과다. 계약이 명확해야 하는 풀스택 팀 특성(이미 discovered-rules의 "REST API 계약 문서화" 규칙과 정합)과도 맞는다.

**5. API 계약 스타일이 "문서화한다"까지만 있고 규약이 없다.**
discovered-rules의 "OpenAPI 스펙 또는 명확한 계약" 규칙은 좋으나, 실제 REST 컨벤션(리소스 복수형 명사 URL, HTTP 메서드 시맨틱, 에러 응답 공통 포맷, 페이지네이션/정렬 규약)이 없다. 파일럿에 비례하는 최소 규약이라도 인터뷰에서 정해두면 FE/BE 병렬 개발 시 재작업을 막는다. springdoc-openapi로 스펙을 코드에서 자동 생성하면 문서화 규칙을 저비용으로 충족한다.

## Positions

**구체 권고(합의 시 team.md `## Code Style`/`discovered-rules.md` 반영):**

- **[권고]** 저장소 토폴로지는 **monorepo**(`frontend/`, `backend/` 디렉터리)로 확정 — 트렁크 기반·squash Bolt·풀스택 팀과 가장 정합.
- **[권고]** BE 에러 핸들링: `Result<T,E>` 미도입, **`@RestControllerAdvice` + 공통 에러 DTO** 방식. 서비스는 도메인 예외를 던지고 advice가 HTTP 매핑.
- **[권고]** FE 에러 핸들링: **중앙 API 클라이언트 래퍼에서 정규화**, 컴포넌트 산발 try-catch 지양.
- **[권고 → 하드 제약 후보]** ALWAYS API 경계에서 요청/응답 **DTO를 사용하고 JPA Entity를 직접 노출하지 않는다.**
- **[권고]** Named exports 기본, "프레임워크가 default를 요구하는 경우만 예외".
- **[권고]** Spring 패키지 구조는 **레이어(controller/service/repository/dto/domain)** 기준 표준 배치. 파일럿 규모에서 도메인(feature)별 패키징까지는 불요, 단일 모듈로 시작.
- **[권고]** React 파일 조직: **feature/기능 단위 폴더**(예: `features/cohort/`)로 컴포넌트·훅·API를 co-locate. `components/`에는 공용 UI만.
- **[권고]** 네이밍: React 컴포넌트 파일·심볼 `PascalCase`, 훅 `useXxx`, 비컴포넌트 유틸 `camelCase`; Java 클래스 `PascalCase`, 메서드/필드 `camelCase`, 패키지 소문자 — 언어 관용 유지(초안과 정합, 프로젝트 전역 리네임 규칙은 두지 않음).

**인터뷰용 미결 질문:**

1. **레포 토폴로지**: monorepo(권고) vs FE/BE 분리 레포? (다른 컨벤션의 선행 결정)
2. **BE 에러 전략**: `@RestControllerAdvice` + 공통 에러 DTO(권고)로 확정할지, `Result<T,E>` 도입 의사가 있는지?
3. **공통 에러 응답 포맷**: 필드 규약(`code`/`message`/`timestamp`/`path` 등) 합의 필요.
4. **DTO/Entity 분리를 하드 제약으로 승격**할지, 관례 수준으로 둘지?
5. **API 계약 도구**: springdoc-openapi 자동 생성 채택 여부와 REST URL/메서드/페이지네이션 최소 규약.
6. **React 상태·데이터 패칭 라이브러리**: TanStack Query 등 도입 여부(에러/로딩 상태 규약에 직결).
7. **ESLint 프리셋**: Airbnb vs 팀 커스터마이징(초안이 "예상"으로 남겨둠) 확정.
8. **Java 포맷터**: IntelliJ 기본 vs Google Java Format 중 택일(초안에 둘 다 나열됨).

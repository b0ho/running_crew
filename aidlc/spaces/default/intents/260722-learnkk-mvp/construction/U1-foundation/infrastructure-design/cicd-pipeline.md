# CI/CD Pipeline — U1 foundation (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U1-foundation
> 리드 aws-platform · 관점 devsecops(파이프라인 보안)·compliance
> 상위 입력: `nfr-design/security-design.md`(시크릿·CI 게이트), `nfr-design/reliability-design.md`(recreate·검증), `nfr-design/performance-design.md`, `logical-components.md`, `inception/application-design/components.md`(FE/BE 분리), `services.md`, `functional-design/business-logic-model.md`
> 근거 관행: team.md/project.md Mandated — **머지 전 CI 린트+테스트, 실패 시 블럭**, GitHub Actions, git SHA 버저닝, squash-merge, FE/BE 분리 파이프라인, 무승인 프로덕션 자동배포 금지(Forbidden). U1이 공유 파이프라인 골격 확립.

## 1. 저장소·파이프라인 토폴로지

team.md("FE/BE 분리 저장소, 각 독립 CI/CD, OpenAPI 계약 동기화"):
- **learnkk-web(React) 저장소** / **learnkk-api(Spring) 저장소** 각각 독립 GitHub Actions 워크플로.
- API 계약 변경은 springdoc-openapi 산출 스펙으로 FE/BE 조율(계약 동기화).

## 2. CI 단계(머지 전 — Mandated 게이트)

`security-design.md`·team.md Mandated("린트+테스트 그린까지 머지 블럭"):

**learnkk-api (Spring)**
1. checkout → JDK 17 셋업.
2. **린트/포맷 체크**: Google Java Format 검증(실패 시 블럭).
3. **빌드**: `./gradlew build`(또는 maven).
4. **테스트**: JUnit 5 + MockMvc + **Testcontainers**(실 DB 트랜잭션·락 검증). U3 동시성 테스트(ExecutorService+CountDownLatch) 포함. 실패 시 블럭.
5. 커버리지 리포트(핵심 도메인 80% 팀 목표, 획일적 하드 게이트 아님 — team.md).

**learnkk-web (React)**
1. checkout → Node 셋업.
2. **린트**: ESLint + Prettier 체크(실패 시 블럭).
3. **빌드**: 프로덕션 빌드.
4. **테스트**: Jest + React Testing Library. 실패 시 블럭.

- **머지 정책**: `main`에서 분기한 단기 feature 브랜치 → PR → CI 그린 → **squash-merge**(Mandated, 장수명 브랜치 금지). Bolt 브랜치는 Bolt 슬러그 단일 커밋.

## 3. 아티팩트 & 버저닝

team.md("git SHA 버저닝, Docker recreate"):
- CI 그린 후 Docker 이미지 빌드, 태그 = **git SHA**(`learnkk-api:<sha>`, `learnkk-web:<sha>`). 로컬 레지스트리 또는 서버 직접 로드. semver는 확장 시 검토.

## 4. CD 단계(배포)

`deployment-architecture.md` §2~3:
- **staging**: `main` 머지 시 자동 → 서버 staging 인스턴스로 이미지 push/pull → **recreate**(기존 정지→새 이미지 기동) → 헬스체크 통과 확인 → 인증 플로우 스모크.
- **production**: **수동 승인 게이트 후** 배포(Forbidden: 무승인 자동 프로덕션 금지). GitHub Actions environment protection(승인자 지정) 또는 수동 워크플로 디스패치. recreate.
- **롤백**: 직전 git SHA 태그 이미지로 recreate 재기동(`deployment-architecture.md` §3).

## 5. 파이프라인 시크릿 관리

`security-design.md`(시크릿 환경변수화):
- CI/CD 시크릿(서버 접속 키·DB 비밀번호·관리자 시드 비밀번호)은 **GitHub Actions Secrets**로 주입, 코드/로그 노출 금지.
- 배포 대상 서버는 `.env`(커밋 금지)로 런타임 시크릿 주입(`infrastructure-services.md` §5).

## 6. 파일럿 보류 & 확장 트리거

- **보류**(`cid:practices-discovery:c3`, team.md): 의존성 SCA·시크릿 스캔·컨테이너 이미지 스캔, blue-green/canary. 확장 시 CI에 보안 스캔 스텝·고급 배포 추가.
- 파일럿 하드 유지: 린트+테스트 머지 게이트, git SHA 버저닝, squash-merge, 수동 승인 프로덕션.

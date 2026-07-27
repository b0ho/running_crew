**Collaborator:** aidlc-devsecops-agent

## Contribution

LearnKK 파일럿(React+Spring, 로컬 서버, 자체 계정 인증, 풀스택 4~6명, 전담 보안 인력 없음)의 관점에서 리드 초안(team-practices.md / discovered-rules.md)과 증거(evidence.md), project.md 결정을 보안·공급망 렌즈로 검토했다. 파일럿 단계이며 "특별 통제 없음"이 명시된 점을 존중해 무거운 통제(SOC2/PCI 매핑, 상시 DAST, 전용 SIEM 등)는 확장 후속 과제로 미루되, **거의 무료이고 CI에 자연히 얹히는 기본 위생(hygiene) 통제**만 파일럿 기본값으로 제안한다.

핵심 관찰:

1. **린트/포맷은 있으나 보안 스캔이 초안에 부재.** team-practices.md의 Code Style에는 Prettier/ESLint가 있고 "머지 전 CI 실행" 규칙도 확정됐지만, discovered-rules.md와 team-practices.md 어디에도 **비밀정보 스캔(secret scanning)**, **의존성 취약점 스캔(SCA)**, **정적 분석(SAST)**이 언급되지 않는다. 로컬 서버라도 소스는 git에 커밋(AGENTS.md의 Git Integration 참조)되므로 자격증명 유출 위험은 실재한다.

2. **의존성 핀 고정 규칙이 project.md에 명시적으로 없다.** 브리핑은 "dependency pinning (project.md rule)"을 언급했으나 project.md와 두 초안 어디에도 명시 핀 고정 규칙이 보이지 않는다. React(npm/yarn lockfile)와 Spring(Gradle/Maven) 모두 lockfile/버전 고정을 확정해야 공급망 재현성과 스캔 정확도가 확보된다. → 인터뷰에서 확정 필요.

3. **자체 계정 인증의 최소 보안 베이스라인이 규칙화되지 않았다.** feasibility 결정(project.md `cid:feasibility:c3`)이 "보안은 검증된 Spring Security 기능으로 완화한다"고 했으므로, 이를 실행 가능한 하드 제약으로 승격할 근거가 이미 있다. 최소한 (a) BCrypt/Argon2 등 검증된 알고리즘의 비밀번호 해싱, (b) 평문/역가역 저장 금지, (c) 세션/토큰의 안전한 처리를 discovered-rules에 넣을 만하다.

4. **파일 업로드가 검증되지 않은 공격면이다.** 출석 증빙 파일 첨부(project.md `cid:scope-definition:c2`, `cid:rough-mockups:c2`, 이미지/문서)는 파일럿의 유일하고 명확한 신뢰 경계 밖 입력이다. 파일럿이라도 (a) 확장자/MIME/매직바이트 검증, (b) 파일 크기 상한, (c) 원본 파일명으로 경로 조합 금지(path traversal), (d) 웹 루트 밖 저장 및 직접 실행 불가 위치 저장은 거의 비용 없이 넣을 수 있는 방어다. 안티바이러스 스캔(ClamAV 등)은 파일럿에서 선택으로 두되 인터뷰에서 여부를 물어야 한다.

5. **PII 처리 — "특별 통제 없음"의 범위를 명확히.** 이메일·성명·닉네임(project.md `cid:scope-definition:c4`)은 이미 최소 수집 원칙이 Mandated에 있다. 규제 매핑은 파일럿에서 정당하게 보류하되, **전송 구간 보호(로컬망이라도 로그인/PII 엔드포인트 TLS 권장)**와 **로그에 PII·비밀번호 미기록** 정도는 저비용 위생 규칙으로 남길 가치가 있다.

6. **로컬 서버의 비밀 관리.** 로컬 배포라도 DB 비밀번호, 세션 시크릿 등이 필요하다. `.env`/외부화 설정을 git에서 제외(.gitignore 확인)하고 커밋 금지하는 규칙이 있어야 secret scanning 규칙과 정합한다.

기존 초안과의 정합성: 위 제안들은 모두 이미 확정된 "머지 전 CI 린트/테스트 실행, 실패 시 블럭" 규칙에 스캔 잡을 추가하는 형태라 새 인프라가 필요 없고 파일럿 규모에 비례한다. 어느 것도 무거운 통제 보류 원칙과 충돌하지 않는다.

## Positions

### 권고 (파일럿 기본값으로 채택 제안)

- **[P1] Secret scanning을 CI에 추가.** gitleaks 또는 GitHub 기본 secret scanning 같은 무료 도구를 머지 전 CI에 얹고, 커밋된 비밀정보 탐지 시 블럭. discovered-rules에 `ALWAYS 자격증명·시크릿은 소스에 커밋하지 않고 외부화 설정으로 주입한다` 추가 권고.
- **[P1] 의존성 취약점 스캔(SCA).** npm audit(프론트) + OWASP Dependency-Check 또는 Gradle 플러그인/Dependabot(백엔드)을 CI에 추가. 파일럿에서는 High/Critical만 게이트하고 나머지는 경고로 처리(프로포셔널).
- **[P1] Lockfile/의존성 버전 고정 확정.** `ALWAYS 프론트엔드(npm/yarn lockfile)와 백엔드(Gradle/Maven) 의존성 버전을 고정(lockfile 커밋)한다` — 재현성과 스캔 정확도 확보. project.md의 dependency pinning 의도를 하드 제약으로 명문화.
- **[P1] 비밀번호 해싱 규칙.** `ALWAYS 비밀번호는 Spring Security의 검증된 어댑티브 해시(BCrypt/Argon2)로 저장하고 평문·가역 저장을 금지한다` — feasibility `cid:feasibility:c3` 결정을 실행 규칙으로 승격.
- **[P1] 파일 업로드 검증.** `ALWAYS 업로드 파일은 유형(MIME/확장자)·크기를 검증하고, 원본 파일명으로 저장 경로를 구성하지 않으며, 웹 루트 밖 비실행 위치에 저장한다` — 출석 증빙 첨부 기능의 필수 방어.
- **[P2] 로그 위생.** `NEVER 비밀번호·토큰·PII 원문을 애플리케이션 로그에 기록하지 않는다` — 저비용, 파일럿 적합.
- **[P2] ESLint 보안 플러그인(선택).** 이미 ESLint가 있으므로 `eslint-plugin-security` 정도만 얹으면 SAST 성격의 최소 정적 점검을 흡수 가능. Spring 측은 파일럿에서 별도 SAST 도구 도입 대신 코드 리뷰로 커버 권고.
- **[Defer] 전용 DAST, 침투 테스트, SOC2/HIPAA/PCI 통제 매핑, 상시 취약점 모니터링(Inspector/GuardDuty류)** 은 클라우드 이관·전사 확장 시점으로 명시 보류. (project.md `cid:feasibility:c1`, `cid:feasibility:c2`와 정합)

### 인터뷰용 미결 질문

1. **Secret scanning 도구**: gitleaks vs GitHub 내장 secret scanning 중 무엇을 CI에 넣을까? (선택한 CI 도구 — evidence.md 미결 #1 — 에 종속)
2. **SCA 게이트 임계값**: 의존성 취약점 스캔에서 파일럿은 어느 심각도(예: High+)부터 머지를 블럭할까, 아니면 전부 경고로만 둘까?
3. **파일 업로드 안티바이러스 스캔**: 출석 증빙 첨부에 ClamAV 등 서버측 AV 스캔을 파일럿에 포함할까, 아니면 유형·크기 검증만으로 충분하다고 볼까?
4. **허용 파일 유형·크기 상한**: 출석 증빙으로 허용할 확장자/MIME 화이트리스트와 최대 파일 크기는? (project.md `cid:approval-handoff:c1`에 "증빙 파일 제약"이 INCEPTION 확정 항목으로 이미 등록됨)
5. **전송 구간 TLS**: 로컬 서버 파일럿에서 로그인·PII 엔드포인트에 TLS(자체 서명 인증서 등)를 적용할까, 아니면 평문 HTTP로 두고 확장 시 도입할까?
6. **비밀 주입 방식**: 로컬 서버의 DB 비밀번호·세션 시크릿을 `.env`/환경변수 중 무엇으로 외부화하고, `.gitignore` 등록을 어떻게 강제할까?
7. **비밀번호 정책 수준**: 파일럿에서 최소 길이·복잡도·잠금(lockout) 정책을 둘까, 아니면 해싱만 하고 정책은 확장 시로 미룰까?

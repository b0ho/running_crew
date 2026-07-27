# Practices Discovery — 인터뷰 (LearnKK 파일럿)

> Inception · practices-discovery · 리드 pipeline-deploy (스포크: quality·developer·devsecops)
> 5개 팀 관행 섹션 + 보안 위생을 확정합니다. 각 문항에 **A=권고안**을 두었으니, 대부분 A로 두고 바꿀 것만 골라 주셔도 됩니다.
> 답변 방식: **직접 편집**(`[Answer]:`) / **자유 대화**. "전부 권고안대로"라고 답하셔도 됩니다.

---

## [Way of Working]

### Q1. 저장소 구조는?
- A. **(권고)** monorepo — `frontend/`(React) + `backend/`(Spring) 단일 저장소
- B. FE/BE 분리 저장소
- X. 기타

[Answer]:b

### Q2. Construction 진행 방식(Bolt 실행)은?
- A. **(권고)** 첫 Bolt(스켈레톤) 후 나머지는 매 Bolt 게이트(사람 확인)
- B. 첫 Bolt 후 자율 진행
- X. 기타

[Answer]:a

## [Walking Skeleton]

### Q3. 워킹 스켈레톤 Bolt를 먼저 실행할까요?
- A. **(권고)** ON — 첫 슬라이스(코호트 개설→선착순 참여)를 스켈레톤으로 먼저 관통
- B. OFF — 스켈레톤 의식 없이 일반 Bolt로 시작
- X. 기타

[Answer]:a

## [Testing Posture]

### Q4. 커버리지 정책은?
- A. **(권고)** 핵심 도메인(선착순/정원·출석 인증·집계·인증) 80% 목표, 전역 머지 게이트는 "린트+테스트 그린"까지
- B. 전 코드 80% 라인 커버리지를 머지 차단 하드 게이트로 강제
- C. 커버리지 게이트 없음(그린만)
- X. 기타

[Answer]:a

### Q5. 정원/선착순 동시성 제어 전략은?
- A. **(권고)** DB 유니크 제약 + 비관적 락(정원 초과 방지 확실)
- B. 낙관적 락(@Version)
- C. 미정 — 설계 단계에서 결정
- X. 기타

[Answer]:a

### Q6. 백엔드 통합 테스트 인프라는?
- A. **(권고)** Testcontainers(실 DB 근접, 정합성 신뢰)
- B. H2(경량·빠름)
- X. 기타

[Answer]:a

### Q7. E2E/스모크 테스트 범위는?
- A. **(권고)** 핵심 플로우 스모크 1~2개(로그인→대시보드→참여→출석)만, 전면 E2E 보류
- B. 전면 E2E 도입
- C. 없음
- X. 기타

[Answer]:a

## [Deployment]

### Q8. CI 도구는?
- A. **(권고)** GitHub Actions
- B. Jenkins
- C. GitLab CI
- X. 기타(직접 입력)

[Answer]:a

### Q9. 로컬 서버 인스턴스 관리 방식은?
- A. **(권고)** Docker 컨테이너(이후 클라우드 이관 용이) + recreate 방식 재배포
- B. systemd 서비스
- C. 단순 프로세스 재시작
- X. 기타

[Answer]:a

### Q10. 아티팩트 버저닝은?
- A. **(권고)** git SHA 기반
- B. semver
- C. 빌드 번호
- X. 기타

[Answer]:a

## [Code Style]

### Q11. 백엔드 에러 핸들링은?
- A. **(권고)** `@RestControllerAdvice` + 공통 에러 DTO(code/message/timestamp/path). Result<T,E> 미도입
- B. Result<T,E> 패턴 도입
- X. 기타

[Answer]:a

### Q12. DTO/Entity 경계를 하드 제약으로?
- A. **(권고)** ALWAYS API 경계에서 DTO 사용, JPA Entity 직접 노출 금지 (하드 제약)
- B. 관례 수준(권장만)
- X. 기타

[Answer]:a

### Q13. API 계약 문서화 도구 / 포맷터 / ESLint는?
- A. **(권고)** springdoc-openapi 자동 생성 + Java=Google Java Format + React=ESLint(팀 커스터마이징)+Prettier, named exports 기본
- B. 다른 조합 원함(X에 기재)
- X. 기타

[Answer]:a

## [Security 위생 — 파일럿 기본]

### Q14. CI 보안 스캔(저비용)을 넣을까요?
- A. **(권고)** secret scanning(gitleaks) + 의존성 취약점 스캔(High/Critical만 게이트) + lockfile 고정
- B. 넣지 않음(확장 시 도입)
- X. 기타

[Answer]:b

### Q15. 인증/파일 업로드 보안 하드 제약은?
- A. **(권고)** 비밀번호 BCrypt 해싱(평문 금지) + 업로드 파일 MIME/크기 검증·웹루트 밖 저장·경로 조작 방지
- B. 파일럿에선 최소화(해싱만)
- X. 기타

[Answer]:b

### Q16. PII/전송 위생은?
- A. **(권고)** 로그에 비밀번호·PII 미기록 + 로그인/PII 엔드포인트 TLS(자체 서명) + 시크릿 .env 외부화(.gitignore 강제)
- B. 파일럿에선 평문 HTTP 허용, 확장 시 TLS
- X. 기타

[Answer]:b

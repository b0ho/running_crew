# 성능 테스트 지침 (performance-test-instructions) — LearnKK 파일럿

> Construction · build-and-test 스테이지 · 리드 QUALITY
> 입력: `nfr-requirements/performance-requirements.md`, `nfr-design/performance-design.md`, U2~U6 `code-summary.md`(N+1·페이지네이션·인덱스)
> 전제: **소규모 파일럿(<100명)·로컬 단일 인스턴스**. 통계적 p95/부하 검증은 Operation 단계 performance-validation으로 이관.

## 1. NFR 성능 목표 (검증 대상 latency)

`performance-requirements.md` §1의 목표 latency(BCrypt cost=10 기준)를 검증 기준으로 삼는다.

| 연산 | 목표 | 지배 비용 |
|---|---|---|
| 로그인(BCrypt 검증 포함) | ≤ 500ms | BCrypt matches ~200~350ms(cost=10) |
| 회원가입 | ≤ 600ms | BCrypt encode + insert |
| 세션 확인(`/me`) | ≤ 150ms | 세션 조회(해싱 없음) |
| 정적/헬스체크 | ≤ 100ms | DB 미접근 |

추가로 유닛 설계가 명시한 조회/이력 경로 목표(설계 예산 기준):
- 코호트 상세·목록 조회: 페이지네이션(기본 20건) + N+1 회피로 회차 수 무관 상수 쿼리(≤4). 목표 지표성 응답 **≤500ms**.
- 관리자 지표/이력 조회: 실시간 집계(캐시 없음), 이력 페이지네이션(기본 20건, 최신순). 이력 조회 목표 **≤350ms** 수준(파일럿 규모).

> 파일럿에서는 통계적 p95를 강제하지 않는다(표본 부족, `performance-requirements.md` §4). 목표 latency 초과 여부만 단건 스모크로 판정한다.

## 2. 검증 방법 (파일럿 스모크)

- **단건 latency 스모크**: 실 compose 스택(Docker 가용 시)에서 핵심 경로를 단건 호출하고 응답 시간을 목표 대비 확인한다.

```bash
# 예: 로그인 latency 단건 측정(실 스택 기동 후)
curl -s -o /dev/null -w "login: %{time_total}s\n" \
  -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@learnkk.local","password":"<pw>"}' -c /tmp/cj
# 세션 확인
curl -s -o /dev/null -w "me: %{time_total}s\n" http://localhost:8080/api/auth/me -b /tmp/cj
```

- **판정**: 목표 latency 초과 여부만 확인(초과 시 원인 분석). BCrypt가 로그인 지배 비용이므로, cost를 상향(보안 강화)하면 로그인 목표를 재산정한다(NFR-SEC-1).
- **본 스테이지 로컬 실행**: Docker 미가용으로 라이브 latency 스모크는 미측정(build-test-results.md에 기록). 코드 경로상 지배 비용(BCrypt·인덱스 조회·상수 쿼리)은 설계·단위/구조 테스트로 확인됨.

## 3. 부하 테스트 접근 (load test approach)

- **파일럿**: 경량 스모크 수준. 목표 처리량은 인증 엔드포인트 ≥ 20 req/s(파일럿 충분, `performance-requirements.md` §2).
- **본격 부하/통계 p95**: Operation 단계 `performance-validation`에서 표본 ≥100 요청으로 산출(도구 예: k6/JMeter/Gatling). 파일럿 construction 범위 밖.
- **HikariCP 풀 사이징 근거**(performance-design §3): RPS × 평균 처리시간 × 1.5 = 20 × 0.4 × 1.5 ≈ 12 → `maximum-pool-size=15`. 로컬 PostgreSQL `max_connections`(기본 100) 이내로 안전.

## 4. 기준 인덱스 확인 (index baseline)

집계·조회·조인 경로가 인덱스로 뒷받침되는지 확인한다(Flyway 마이그레이션 기준).

| 인덱스 | 마이그레이션 | 뒷받침 경로 |
|---|---|---|
| `users.email` UNIQUE | V1 | 로그인/가입 조회 O(log n) — email 소문자 정규화로 인덱스 적중 |
| `ix_cohort_status`, `mentor_id`, `created_at`, `announcement(cohort_id, created_at)` | V2 | 코호트 목록·소유 조회·공지 최신순 |
| enrollment `UNIQUE(cohort_id, mentee_id)` + 인덱스 2종 | V3 | 중복 방지·확정 집계·대기 목록 |
| `attendance_evidence(session_id, created_at)` | V4 | 증빙 이력 |
| `ix_final_report_cohort_submitted(cohort_id, submitted_at)`, certificate `UNIQUE(cohort_id, mentee_id)` | V5 | 보고서 이력·수료증 멱등 |
| `ix_attendance_evidence_created(created_at)`, `ix_final_report_submitted(submitted_at)` | V6 | 전역 최신순 이력(cohortId 필터 없는 경로) |

- **N+1 회피 확인**: 코호트 상세는 리포지토리 상수 쿼리(≤4)로 회차 수 무관(U2 `code-summary.md` §2). 이력은 생성자 표현식 JPQL 조인으로 단일 로딩(U6). 통합 테스트가 쿼리 카운트를 단언(Docker 가용 CI).
- 실 실행 계획(`EXPLAIN`) 검증은 실 DB 필요 → 라이브 스모크/CI에서 확인.

## 5. 파일럿 규모 전제와 확장 트리거

- **미도입(파일럿)**: 애플리케이션 캐시(Redis/Memcached)·CDN·API 응답 캐시·비동기 큐·머티리얼라이즈드 뷰·사전 집계 테이블. 근거: <100명·단일 인스턴스에서 BCrypt가 지배 비용이라 캐시 이득 미미(performance-design §5, U6 INV-U6-2 실시간 집계).
- **확장 트리거**(performance-design §5):
  - 활성 사용자 100명 초과 또는 인증 응답이 목표를 지속 미달 → 먼저 수직 확장(리소스·풀 크기 상향), 그다음 조회 캐시 검토.
  - 인스턴스 ≥ 2 → 커넥션 풀 총합이 DB 한도 이내가 되도록 재산정 + 세션 외부화.
  - 지표 조회 지연 증가 → 캐시(TTL)/사전 집계 도입 검토(U6 performance-design §3).

## 6. 상위 산출물 참조

- latency 예산 배분·풀 사이징: `nfr-design/performance-design.md` §1·§3.
- 성능 목표·검증 방침: `nfr-requirements/performance-requirements.md`.
- N+1 회피·페이지네이션·인덱스 구현: U2 `code-summary.md` §2, U6 `code-summary.md` §2.

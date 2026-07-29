# Performance Design — U6 admin-metrics (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/performance-requirements.md`(지표·이력 목표), `nfr-requirements/scalability-requirements.md` §0(기준 인덱스), `nfr-requirements/tech-stack-decisions.md`(집계 쿼리·Pageable), `functional-design/business-logic-model.md`(W-U6-1 집계 §2)
> 전제: <100명·로컬 단일 인스턴스(U1 성능 전제 상속). 읽기 전용 소비자.

## 1. 응답시간 예산(latency budget)

`performance-requirements.md` §1 목표를 분해한다.

| 연산 | 총 목표 | 설계 예산 |
|---|---|---|
| 운영 지표(overview) | ≤ 500ms | 4개 집계 쿼리(종료됨 코호트 대상 COUNT/SUM) 각 ~80ms + DTO 조립 |
| 증빙 이력(20건 페이지) | ≤ 350ms | 조인(Evidence×Session×Cohort×User) + 페이지네이션 ~250ms + 직렬화 |
| 보고서 이력(20건 페이지) | ≤ 350ms | 조인(Report×Cohort×User) + 페이지네이션 ~250ms + 직렬화 |

## 2. 집계 성능 & 기준 인덱스(핵심)

`performance-requirements.md` §2 + `scalability-requirements.md` §0을 확정한다.

- **실시간 계산(캐시 없음)**: 지표는 조회 시점 소스 데이터에서 실시간 계산(INV-U6-2, FR-11 데이터 일치). 데이터 소량(<100명)이라 캐시 불필요.
- **기준 인덱스(파일럿 필수 — 확장 아님)**: 아래 인덱스가 code-generation 시 반드시 생성되어야 목표가 재현된다. 각 인덱스는 소스 유닛(U2~U5) 스키마에 속하나 U6 조회 성능의 전제이므로 U6가 요구로 명시한다(각 소유 유닛/인프라 단계에서 생성).

| 인덱스 | 소유 유닛 | U6 용도 |
|---|---|---|
| `cohort(status)` | U2 | 종료됨 코호트 필터·완주 수 |
| `enrollment(cohort_id, status)` | U3 | 확정 멘티 수(수료율 분모) |
| `certificate(cohort_id)` | U5 | 증서 수 집계 |
| `attendance_evidence(created_at)` | U4 | 증빙 이력 최신순 정렬 |
| `final_report(submitted_at)` | U5 | 보고서 이력 최신순 정렬 |

- **N+1 회피**: 이력 조회는 조인 쿼리(fetch join 또는 projection DTO)로 업로더/작성자 성명을 한 번에 로딩. 페이지네이션(20건)으로 응답 크기 상한.

## 3. 파일럿 스코프아웃 & 확장 트리거

- **미도입**: 지표 캐시(TTL), 머티리얼라이즈드 뷰, 사전 집계 테이블. 근거: 실시간 집계 비용이 파일럿 규모에서 낮음(INV-U6-2 데이터 일치 우선).
- **확장 트리거**: 데이터가 수만+ 코호트/참여로 성장하거나 지표 응답이 목표 지속 초과 시 → 집계 캐시(TTL) 또는 사전 집계 스케줄 도입(`scalability-design.md` §2). 이력은 페이지네이션 유지. 성능 엄밀 검증은 Operation performance-validation으로 이관(U1 방침 상속).


## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 검증 범위

U6-admin-metrics nfr-design 단계 산출물 5개(performance-design, security-design, scalability-design, reliability-design, logical-components)를 상위 계약(nfr-requirements 5개, functional-design/business-logic-model) 대비 적대적으로 검증하였다. Defect를 가정하고 반증을 시도하는 관점으로 진행하였으며, READY는 반증 실패 후 도달한 판정이다.

### 검증 항목별 결과

#### 1. 크로스유닛 계약 일관성 ✅
- **말단 소비자 확인**: U1~U5 전체 유닛에서 U6 호출 없음을 grep으로 확인. U6은 DAG의 종착점(U1→U2→(U3∥U4)→U5→U6).
- **읽기 전용 경계**: INV-U6-1(쓰기 없음)이 5개 산출물 전체에 일관되게 명시됨. `@Transactional(readOnly=true)` 설계됨.
- **소스 유닛 계약**: U2(종료 코호트·회차), U3(confirmedCount/confirmedEnrollments), U4(증빙 이력), U5(증서·보고서)에 대한 읽기 계약이 logical-components §2 및 business-logic-model §5와 일치.
- **순환 의존 부재**: U6이 쓰기를 하지 않으며 다른 유닛이 U6을 호출하지 않으므로 순환 불가능.

#### 2. 관리자 전용 인가 일관성 ✅
- `@PreAuthorize("hasRole('ADMIN')")` 명시(security-design §1, logical-components §1).
- MetricsController·HistoryController 모두 ADMIN 인가 적용.
- R-U6-01 일관 참조, 비관리자 403·미인증 401 매핑 일관.

#### 3. 지표 정확성 & 0 나눗셈 안전 ✅
- **실시간 계산(INV-U6-2)**: 캐시 없이 조회 시점 계산 → FR-11 데이터 일치 보장.
- **분모 0 가드(INV-U6-3)**: `COALESCE` 쿼리 + 애플리케이션 분모 0 → 0% 이중 방어(reliability-design §1). R-U6-04/05 일관 참조.
- **집계 범위 일관(INV-U6-4)**: 출석률·수료율·완주 수 모두 종료됨(CLOSED) 코호트 기준. 진행중/모집중 제외로 의미 명확.

#### 4. 기준 인덱스 요구 명확성 ✅
- 5개 인덱스(`cohort(status)`, `enrollment(cohort_id,status)`, `certificate(cohort_id)`, `attendance_evidence(created_at)`, `final_report(submitted_at)`) 목록이 performance-design §2 + scalability §0 양쪽에 동일하게 명시됨.
- 각 인덱스의 **소유 유닛**(U2~U5)과 **U6 용도**(CLOSED 필터·분모 집계·이력 정렬)가 표로 정리됨.
- "각 소유 유닛/인프라 단계에서 생성"으로 구현 책임 명확화. 인덱스 소유 혼란 가능성이 설계 문서에서 선제 해소됨.
- 인덱스가 ≤500ms/350ms 목표를 뒷받침함을 확인(파일럿 규모 <100명 전제).

#### 5. 페이지네이션 일관성 ✅
- 증빙 이력·보고서 이력 모두 **20건 페이지** 일관(performance-design §1·2, scalability-design §3, logical-components §1).
- tech-stack-decisions에서 Spring Data `Pageable` 선택 명시.
- 대량 데이터 대비 응답 크기 상한 확보.

#### 6. DTO 경계 & Entity 미노출 ✅
- 응답 DTO(MetricsOverviewDto, EvidenceHistoryItemDto, ReportHistoryItemDto) domain-entities.md에 정의됨.
- "Entity 미노출(U1 Mandated)" 명시(security-design §3).
- ArchUnit 검증 언급으로 타 유닛 일관 정책 확인.
- N+1 방지(조인 쿼리/projection DTO) 설계됨.

#### 7. 성능 목표 달성 가능성 ✅
- 4개 집계 쿼리(각 ~80ms) + DTO 조립 → 500ms 목표 현실적.
- 조인·페이지네이션 이력(~250ms + 직렬화) → 350ms 목표 합리적.
- 기준 인덱스 5개가 전제로 명시되어 목표 재현 조건 명확.
- 파일럿 규모(<100명)에서 실시간 집계 비용 낮음, 확장 트리거(수만+ 데이터 시 캐시 도입) 문서화됨.

#### 8. 크로스레퍼런스 유효성 ✅
- 모든 규칙 ID(R-U6-01~07) business-rules.md 해소.
- 모든 불변식 ID(INV-U6-1~4) business-rules.md 해소.
- 모든 상위 산출물(nfr-requirements 5개, functional-design/business-logic-model) 존재.
- DTO/컴포넌트/서비스 참조(MetricsService, HistoryService, MetricsRepository, FileStorageService.load) 일관.

#### 9. 구현 가능성(개발자 관점) ✅
- 서비스 경계 명확(MetricsService, HistoryService, MetricsRepository).
- 구체 쿼리 패턴 제시(`SELECT COUNT(*) FROM cohort WHERE status='CLOSED'`, COALESCE, 조인 쿼리).
- 트랜잭션 경계(`@Transactional(readOnly=true)`), 인가 패턴(`@PreAuthorize`), DTO 형상 모두 정의됨.
- 크로스유닛 계약 explicit(U6은 읽기만, 다른 유닛이 U6 미호출).
- 파일럿 보류(캐시·머티리얼라이즈드 뷰·사전 집계) 명시적 스코프아웃 + 확장 트리거 문서화.
- 개발자가 본 산출물만으로 U6을 구현 가능함을 확인.

### 소견

U6-admin-metrics는 **읽기 전용 말단 소비자**로서 조직 전체 데이터를 열람하는 관리자 전용 리포팅 유닛이다. 핵심 설계 관심사는:

1. **관리자 전용 인가**(ROLE_ADMIN `@PreAuthorize`) → 조직 데이터 열람 보호
2. **읽기 전용 무결성**(INV-U6-1, readOnly 트랜잭션) → 데이터 변조 표면 없음
3. **지표 정확성**(INV-U6-2~4: 실시간 계산·0 나눗셈 안전·CLOSED 범위 일관) → FR-11 일치 보장
4. **기준 인덱스 5개**(파일럿 필수) → 성능 목표 재현 조건
5. **페이지네이션(20건)** → 이력 응답 크기 상한
6. **DTO 경계** → Entity 미노출·PII 보호

적대적 검증 결과, 상기 관심사가 5개 nfr-design 산출물에 일관되게 설계되었으며, 상위 계약(nfr-requirements, functional-design)과 정합하고, 크로스유닛 계약이 해소되며, 순환 의존이 없고, 개발자가 본 산출물만으로 구현 가능함을 확인하였다.

파일럿 스코프아웃 항목(지표 캐시·머티리얼라이즈드 뷰·사전 집계)은 명시적으로 보류되었고 확장 트리거(데이터 수만+ 건 또는 목표 지속 초과)가 문서화되어 있다.

**판정 근거:** 반증 시도 실패. 설계 일관성·정합성·구현 가능성 충족.

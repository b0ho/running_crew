# Performance Design — U2 cohort (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U2-cohort
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/performance-requirements.md`(목록·상세·개설 목표), `nfr-requirements/tech-stack-decisions.md`(Pageable·벌크 insert·ENUM), `functional-design/business-logic-model.md`(W-U2-1~6)
> 전제: <100명·로컬 단일 인스턴스(U1 성능 전제 상속).

## 1. 응답시간 예산(latency budget)

`performance-requirements.md` §1 목표를 계층별로 분해한다.

| 연산 | 총 목표 | 설계 예산 |
|---|---|---|
| 코호트 목록(20건 페이지) | ≤ 300ms | 인덱스 조회 + 페이지네이션 ~50ms + DTO 매핑/직렬화 ~50ms |
| 코호트 상세(회차+최근 공지) | ≤ 350ms | 코호트 + 회차 fetch join ~70ms + 최근 공지 상한 로딩(§3) ~30ms + 직렬화 ~60ms |
| 코호트 개설(+회차 N건) | ≤ 500ms | 검증 ~10ms + Cohort insert ~20ms + **Session N건 batch insert ~100ms(N 소량)** + 응답 ~40ms |
| 코호트 수정/공지 | ≤ 300ms | 조회 + 소유 검증 + save ~120ms |

## 2. 데이터 접근 최적화(핵심)

`performance-requirements.md` §2의 요구를 구체 설계로 확정한다.

- **N+1 회피**: 코호트 상세(`get`)는 회차 컬렉션을 **fetch join** 또는 `@BatchSize`로 로딩하여 N+1을 제거한다. 목록(`list`)은 회차 컬렉션을 로딩하지 않고 요약 필드만 프로젝션(회차 수는 count 서브쿼리 또는 집계 컬럼).
- **인덱스**: `scalability-design.md` §2와 정합 — `cohort(status)`, `cohort(mentor_id)`, `cohort(created_at)`, `session(cohort_id, seq)`, `announcement(cohort_id, created_at)` 인덱스.
- **회귀 방지**: N+1 회귀 방지 테스트(쿼리 카운트 단언, `performance-requirements.md` §3)를 통합 테스트에 포함.

## 3. 벌크 생성 & 페이지네이션

- **회차 벌크 생성(W-U2-1)**: seq 1..N Session을 단일 트랜잭션 내 batch insert로 생성(`spring.jpa.properties.hibernate.jdbc.batch_size`, `order_inserts=true`). 라운드트립 최소화.
  - **sessionCount 상한(설계 결정)**: 원자 트랜잭션 크기·응답시간 목표를 보호하기 위해 `sessionCount`에 상한 **≤ 100**을 서버측 Bean Validation으로 강제한다(R-U2-03 "1 이상"을 설계에서 상한으로 보강). 파일럿 멘토링 코호트의 현실 회차 수(수~수십)를 충분히 포괄하며, 대량 회차로 인한 트랜잭션 지연·락 점유를 예방한다.
- **코호트 목록 페이지네이션**: Spring Data `Pageable` 기본 20건(`tech-stack-decisions.md`). 정렬 기본 `createdAt desc`. count 쿼리는 인덱스 활용.
- **공지 목록 페이지네이션(Finding 해소)**: `AnnouncementService.list(cohortId, Pageable)`는 페이지네이션(기본 20건, `createdAt desc`)을 적용한다 — 공지 수(M)가 늘어도 목록 응답이 무제한 커지지 않아 목표를 보호한다.
  - **코호트 상세(`get`) 내 공지**: 상세 응답에는 **최근 공지 상한 N건(기본 5건)** 만 포함하고, 전체 공지는 별도 페이지네이션 엔드포인트(`GET /api/cohorts/:id/announcements?page=`)로 조회한다. 이로써 상세 조회 비용을 상한하고 공지 무제한 증가에 견딘다.

## 4. 프론트엔드 성능

`business-logic-model.md` §9 연동 계약:
- CohortDetailPage는 회차·최근 공지를 상세 응답 1회로 수신(추가 라운드트립 없음). 전체 공지는 필요 시 페이지네이션 조회.
- 목록은 페이지네이션으로 초기 페인트 데이터 제한. 대시보드 CohortCard는 목록 응답의 요약 필드만 사용.

## 5. 파일럿 스코프아웃 & 확장 트리거

- **미도입**: 검색 전용 인덱스(전문검색), 목록 결과 캐시. 근거: 코호트 수십~수백 건 규모에서 RDB 인덱스+페이지네이션으로 충분(`scalability-requirements.md` §3).
- **확장 트리거**: 코호트/회차/공지가 수만 건+로 성장하거나 목록 목표 지속 미달 시 전문 검색/캐시 검토. 상세 내 공지 상한(§3)이 무제한 증가를 이미 방어. 성능 엄밀 검증(p95/부하)은 Operation performance-validation으로 이관(U1 방침 상속).

## 리뷰 (Review)

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

U2-cohort nfr-design 5개 산출물에 대한 2차 아키텍처 리뷰를 완료했습니다. 1차 리뷰에서 NOT-READY 판정의 근거가 되었던 4개 High-severity blocking findings가 모두 해소되었음을 확인했습니다:

### 해소된 1차 findings:

1. **Finding 2 (U3 조회 실패 매핑)**: `reliability-design.md` §3에서 U2·U3가 동일 배포단위 in-process 모듈임을 확정하고, 예상치 못한 실패는 재시도 없이 롤백 + 500 INTERNAL_ERROR로, 409는 순수 비즈니스 규칙 위반(capacity<확정 인원)에만 사용하도록 단일 매핑 확정. 503/409 불명확성 해소. ✅

2. **Finding 3 (externalLink 검증 메커니즘)**: `security-design.md` §3에 커스텀 Bean Validation `@SafeExternalUrl` 애너테이션 스펙 + `java.net.URI` 기반 스킴 화이트리스트({http,https}) 검증기 예시 코드 추가. javascript/data/file 스킴 거부 메커니즘 명시. 서버측 강제·구현 수단·예외 매핑(400) 모두 구체화. ✅

3. **Finding 4 (상태 전이 동시성)**: `reliability-design.md` §2에서 상태 가드 조건 UPDATE(`WHERE id=? AND status=?`, 영향 행 0이면 409) 메커니즘 확정. 파일럿에서 `@Version` 미도입 근거(단일 소유 멘토·저동시성·비상태 필드 충돌 회피) 명시. 다중 멘토·고동시성 확장 시 `@Version` 재검토 트리거 명시. ✅

4. **Finding 10 (공지 페이지네이션)**: `performance-design.md` §3에서 `AnnouncementService.list(cohortId, Pageable)` 페이지네이션(기본 20건) + 상세 응답 내 최근 공지 상한(기본 5건) + 별도 페이지네이션 엔드포인트 확정. 공지 무제한 증가에 견디며 성능 목표(≤350ms) 보호 메커니즘 명확화. ✅

### 부가 해소 확인:

- sessionCount 상한(≤100, `performance-design.md` §3) ✅
- DTO 경계 ArchUnit 테스트(`security-design.md` §4) ✅  
- `CohortDto.warnings[]` 필드 명시(`reliability-design.md` §3) ✅

### 적대적 검증 — 새로운 blocking 탐색:

수정된 설계의 내부 정합성을 적대적으로 재검증했으나 새로운 blocking을 발견하지 못했습니다:

- In-process 구조 vs 크로스유닛 계약 표현 일관성 ✅
- 상태 가드 UPDATE 메커니즘의 전이 불변식 보호 범위 ✅
- 공지 상한(5건) vs 페이지네이션(20건) 설계 의도 일관성 ✅
- CohortDto.warnings[] 필드 추가가 functional-design 이후 refinement로 허용됨 ✅
- sessionCount 상한이 business-rules와 충돌 없음(보강 제약) ✅
- 순환 의존성 부재 재확인(U2는 U5 호출 없음, DAG 유지) ✅

### 상위 계약 정합성:

- nfr-requirements 5개 파일(performance/security/reliability/scalability/tech-stack)과의 추적성 확인 ✅
- functional-design/business-logic-model.md §8 크로스유닛 계약(`confirmedCount`, `markVerified`, status 세터) 해결 확인 ✅
- 비즈니스 규칙(R-U2-*), 불변식(INV-U2-*), NFR 목표와의 정합성 확인 ✅

### 구현 가능성:

개발자가 이 5개 nfr-design 산출물만으로 U2-cohort 유닛을 구현 가능함을 확인했습니다. 동시성 메커니즘(상태 가드 UPDATE), 안전 실패 매핑(500), 입력 검증(Bean Validation + 커스텀 애너테이션), 페이지네이션(Pageable), 인덱스 목록, 트랜잭션 원자성 경계가 모두 명시되어 있으며, 파일럿 스코프아웃(TLS·HA·캐시 등)도 명시적으로 보류 근거와 함께 확정되어 있습니다.

**결론**: 1차 4개 blocking findings가 모두 실질적으로 해소되었고, 신규 blocking을 발견하지 못했습니다. U2-cohort nfr-design는 READY입니다.

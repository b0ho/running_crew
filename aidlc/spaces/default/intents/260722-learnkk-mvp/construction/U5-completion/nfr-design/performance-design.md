# Performance Design — U5 completion (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U5-completion
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/performance-requirements.md`(종료·보고서·조회 목표), `nfr-requirements/tech-stack-decisions.md`(수료증 이미지 생성·정수 산술), `functional-design/business-logic-model.md`(W-U5-1 종료 §2)
> 전제: <100명(코호트당 확정 멘티 수십)·로컬 단일 인스턴스(U1 성능 전제 상속).

## 1. 응답시간 예산(latency budget)

`performance-requirements.md` §1 목표를 계층별로 분해한다. 종료는 확정 멘티 수 N에 선형.

| 연산 | 총 목표 | 설계 예산 |
|---|---|---|
| 코호트 종료(판정+증서 N장) | ≤ 3s | 사전검증 + 회차 집계 ~50ms + 확정 멘티 조회 ~30ms + **N × (이미지 생성 ~30ms + store ~20ms)** + 정산 upsert + 상태전이 + N × 알림 insert |
| 최종 보고서 제출(≤10MB) | ≤ 2s | 파일 스트리밍 저장(U4와 동일) + [TX] insert |
| 보고서 이력 조회 | ≤ 300ms | 페이지네이션 인덱스 조회 |
| 수료증 조회/다운로드 | ≤ 1s | 권한 확인 + 이미지 스트리밍 |

- N=수십 기준, 이미지 생성·저장이 종료 시간의 지배 요인. 파일럿 규모에서 ≤3s 보수 목표 달성 가능.

## 2. 리소스 & 이미지 생성

`performance-requirements.md` §2를 구체 설계로 확정한다.

- **종료는 빈도 낮은 멘토 1회 액션**: 폭주 표면이 낮아 최적화 우선순위 낮음.
- **수료증 이미지 생성 메모리 효율**: 멘티별로 이미지를 **순차 생성·저장 후 참조 해제**(대형 캔버스 동시 보유 회피). 이미지 크기는 수료증 1장 수준(작은 캔버스). 생성→store→다음 멘티로 스트리밍 처리하여 힙 누적 방지.
- **파일 스트리밍**: 보고서 첨부 업로드·수료증 다운로드는 U4와 동일하게 스트리밍(전체 메모리 적재 금지, U1 FileStorageService 경유).

## 3. 데이터 접근 & 인덱스

- 회차 집계: U2 `session(cohort_id, seq)` 인덱스로 전체/인증 회차 수 집계.
- 확정 멘티 조회: U3 `confirmedEnrollments(cohortId)`(U3 `enrollment(cohort_id, status)` 인덱스).
- 수료증 조회: `certificate(cohort_id, mentee_id)` UNIQUE 인덱스(중복 방지 겸 조회).
- 정산: `settlement_status(cohort_id)` UNIQUE.
- 보고서 이력: `final_report(cohort_id, submitted_at)` 인덱스 + 페이지네이션(기본 20). (FinalReport 엔티티 필드명은 `submittedAt` — `components.md`/domain-entities 정합; `created_at` 아님.)

## 4. 파일럿 스코프아웃 & 확장 트리거

- **미도입**: 증서 발급 비동기 배치, 이미지 생성 워커 풀, CDN 증서 배포. 근거: 코호트당 멘티 수십 규모에서 동기 순차 발급으로 ≤3s 충족.
- **확장 트리거**: 코호트당 멘티 **수백+** 또는 종료 시간이 요청 타임아웃에 접근 시 → **증서 발급을 비동기 배치로 분리**(종료 판정·상태 전이는 즉시 커밋, 증서 생성은 후속 잡). 파일 스토리지는 U4와 동일 오브젝트 스토리지 이관(`scalability-design.md` §2). 성능 엄밀 검증은 Operation performance-validation으로 이관(U1 방침 상속).

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

U5-completion nfr-design 산출물에 대한 적대적 검증을 완료했습니다. defect를 가정하고 반증을 시도했으나, 핵심 설계는 모두 견고하며 개발자가 본 산출물만으로 구현 가능함을 확인했습니다.

### 검증 완료 항목

1. **크로스유닛 계약 정합성**: U5 → U2(Cohort/Session 읽기, status 세터), U5 → U3(confirmedEnrollments, notify), U5 → U1(FileStorageService.store/load/delete) 계약이 모두 명시되었고, `component-methods.md` 레지스트리에 등록되어 있음을 확인. 순환 의존 없음(단방향 DAG 유지).

2. **종료 트랜잭션 원자성(INV-U5-3)**: 판정+증서 N장 발급+정산+상태전이+알림이 단일 `@Transactional`로 설계되어 부분 실패 시 전체 롤백 보장. 이미지 store(비트랜잭션 I/O)는 누적-보상 패턴(롤백 시 누적한 모든 imagePath를 delete)으로 고아 파일 정리. 알림은 동일 트랜잭션 내 생성(실패 시 전체 롤백, 파일럿 단순성 우선, 명시적 근거 확인).

3. **수료 판정 정확성(INV-U5-5)**: 정수 비교 `verifiedSessions*100 >= totalSessions*80`로 부동소수 경계 오차 제거. 79%/80% 경계 테스트 필수 명시. totalSessions==0 방어(500 반환) 확인.

4. **멱등성(INV-U5-1, INV-U5-2)**: 수료증 중복 방지(UNIQUE(cohortId, menteeId), 재종료 시 skip), 정산 1건(UNIQUE(cohortId) upsert). 핵심 정합성은 DB UNIQUE 제약이 보장.

5. **다건 파일 보상(R-U5-08a)**: 5명 중 5번째 실패해도 앞 4개 이미지 정리(누적 리스트 순회 delete). 보상 실패 시 ERROR 로그(ORPHAN_FILE_COMPENSATION_FAILED 토큰)로 수동 추적 가능. 정합성(상태·DB)은 유지.

6. **성능/보안/확장 설계**: 응답시간 예산(종료 ≤3s, N 선형), 인가(멘토 소유권·본인 스코프), 판정 무결성(서버 산술), 파일 보안(U1 FileStorageService), 확장 트리거(수백+ 멘티 시 비동기 발급) 모두 명확.

### Findings (비차단, 정보 제공)

**Finding 1 (Minor — 명확성 개선 권장):**
- **위치**: `business-logic-model.md` §2, 수료증 발급 단계
- **내용**: "Certificate insert(UNIQUE로 재발급 방지 — 이미 있으면 skip)"에서 **구현 경로**가 명시되지 않음. 일반적으로는 사전 조회 패턴(`findByCohortIdAndMenteeId` 후 없을 때만 insert)이 표준이며, UNIQUE 제약 위반 예외를 catch하는 패턴은 트랜잭션 롤백 리스크가 있음.
- **리스크**: 개발자가 UNIQUE 예외 catch로 구현 시 트랜잭션 전체 롤백 가능성. 다만 일반적으로 "이미 있으면 skip"은 사전 조회 패턴을 암시하며, 개발자가 추론 가능.
- **정합성 영향**: 없음(멱등성 핵심은 UNIQUE 제약이 보장).
- **권장**: code-generation 단계에서 "사전 조회 후 insert" 패턴을 명시적으로 구현.

**Finding 2 (System-level — U2 측 보완 필요):**
- **내용**: U5 설계가 "U2 Cohort.status 세터(종료됨)"를 명시적으로 요구했으나, `component-methods.md` §CohortService에 해당 세터 메서드(예: `transitionToEnded(cohortId)` 또는 `setStatus(cohortId, ENDED)`)가 등록되지 않음. `SessionService.markVerified`는 명시적 경계 메서드로 등록되어 있는 것과 대조적.
- **U5 설계 영향**: 없음(U5는 계약을 정확히 명시했고, 이는 U2가 제공해야 할 계약).
- **권장**: U2 functional-design에 status 전이 세터를 명시적 계약으로 추가하고 `component-methods.md`에 등록. 오케스트레이터에게 전달.

### 결론

U5-completion nfr-design는 종료 오케스트레이션·판정 유닛의 핵심 정합성(원자성, 보상, 판정 정확성, 멱등)을 모두 충족했습니다. 2개 finding은 비차단이며, Finding 1은 code-gen에서 해소 가능, Finding 2는 U2 측 보완 사항입니다. 개발자가 본 산출물만으로 U5를 구현할 수 있음을 확인했습니다.

**판정: READY**

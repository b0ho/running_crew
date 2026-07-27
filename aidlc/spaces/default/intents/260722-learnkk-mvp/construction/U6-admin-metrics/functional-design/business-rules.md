# Business Rules — U6 admin-metrics (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U6 책임), `unit-of-work-story-map.md`(US-14/15), `requirements-analysis/requirements.md`(FR-10/11), `application-design/components.md`(소스 엔티티), `component-methods.md`(MetricsService.overview, HistoryService), `services.md`(MetricsService·HistoryService)
> 규칙 표기: R-U6-nn. U1 공통 에러 핸들러 재사용. 읽기 전용 유닛(쓰기 규칙 없음).

## 1. 권한 규칙 (US-14/15 / FR-10)

| ID | 규칙 | 위반 시 |
|---|---|---|
| R-U6-01 | 운영 지표·증빙 이력·보고서 이력 조회는 **관리자(ROLE_ADMIN)만**(`@PreAuthorize("hasRole('ADMIN')")`, U1 R-U1-16a) | 403 FORBIDDEN |
| R-U6-02 | 미인증 접근은 401 UNAUTHORIZED | 401 |
| R-U6-03 | U6은 읽기 전용이며 어떤 데이터도 수정하지 않는다 | (불변) |

## 2. 운영 지표 산식 (US-14 / FR-11) — 정확한 정의

지표는 **실제 데이터와 일치**해야 한다(FR-11 수용기준). 각 산식을 명시한다.

| ID | 지표 | 정의(산식) |
|---|---|---|
| R-U6-04 | **출석률(attendanceRate)** | 종료됨 코호트 대상, `Σ(코호트별 인증 회차 수) / Σ(코호트별 전체 회차 수)`. 분모(전체 회차 합)가 0이면 0으로 표시(0 나눗셈 방지). 백분율 표시, 소수 1자리 반올림 |
| R-U6-05 | **수료율(completionRate)** | 종료됨 코호트 대상, `발급 증서 수 / 종료됨 코호트의 확정 멘티 총수`. 분모가 0이면 0. 백분율, 소수 1자리 |
| R-U6-06 | **완주 코스 수(completedCohortCount)** | `count(Cohort WHERE status=종료됨)` |
| R-U6-07 | **발급 증서 수(certificateCount)** | `count(Certificate)` |

- **집계 범위 일관성**: 출석률·수료율은 모두 **종료됨(CLOSED) 코호트**만 대상으로 한다(진행중 코호트는 판정 미확정이므로 제외). 이 범위 기준을 지표 응답에 명시(예: "종료된 코호트 N건 기준").
- **소수 처리**: 백분율은 정수 산술 후 표시 단계에서 반올림(예: `Math.round(rate*1000)/10`). 내부 비교/판정에는 사용하지 않음(표시 전용).

## 3. 이력 조회 규칙 (US-15 / FR-10)

| ID | 규칙 |
|---|---|
| R-U6-08 | **증빙 이력**과 **보고서 이력**은 **각각 별도 뷰**로 조회(FR-10 수용기준: 분리 조회) |
| R-U6-09 | 증빙 이력: U4 AttendanceEvidence를 코호트·회차·업로더(성명) 조인해 목록 반환. 페이지네이션 기본 20건, 최신순 |
| R-U6-10 | 보고서 이력: U5 FinalReport를 코호트·작성자(성명)·첨부 유무와 함께 목록 반환. 페이지네이션 20건, 최신순 |
| R-U6-11 | 이력 항목의 파일 다운로드는 U1 `FileStorageService.load` 경유(관리자 권한 확인 후) |

## 4. 신규 예외 → HTTP 매핑 (U1 공통 표 재사용)

| ID | 예외 | HTTP | code |
|---|---|---|---|
| R-U6-12a | Spring Security `AccessDeniedException`(비관리자) | 403 | FORBIDDEN (U1 R-U1-17f 재사용) |
| R-U6-12b | `AuthenticationException`(미인증) | 401 | UNAUTHORIZED (U1 R-U1-17e 재사용) |

- U6은 읽기 전용이라 신규 도메인 예외가 거의 없다. 권한 예외는 U1 핸들러 재사용.

## 5. 불변식 (Invariants)

- INV-U6-1: U6은 어떤 영속 데이터도 쓰지 않는다(순수 읽기).
- INV-U6-2: 지표는 조회 시점의 실제 데이터에서 계산되며 별도 캐시/중복 저장을 두지 않는다(FR-11 일치 보장, 파일럿 규모 <100명이라 실시간 집계로 충분).
- INV-U6-3: 출석률·수료율의 분모 0은 0%로 표시(예외 없이 안전 처리).
- INV-U6-4: 출석률·수료율 집계 범위는 종료됨 코호트로 일관.

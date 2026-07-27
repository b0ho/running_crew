# Domain Entities — U6 admin-metrics (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U6 책임), `unit-of-work-story-map.md`(US-14/15), `requirements-analysis/requirements.md`(FR-10/11), `application-design/components.md`(엔티티 소스), `component-methods.md`(MetricsService·HistoryService), `services.md`(MetricsService·HistoryService)
> 범위: U6는 **영속 엔티티를 소유하지 않는다**. 다른 유닛의 데이터를 read-only로 집계·조회하는 리포팅/조회 유닛이다. 본 문서는 U6가 읽는 소스와 산출 DTO(읽기 모델)를 정의한다.

## 1. U6의 성격 (소유 엔티티 없음)

U6은 운영 지표 집계(US-14)와 관리자 이력 조회(US-15)를 담당하는 **읽기 전용 유닛**이다. 신규 테이블/엔티티를 만들지 않고, 다른 유닛이 소유한 데이터를 read-only로 읽어 집계·조회 DTO로 반환한다. 쓰기 연산이 없다.

## 2. 읽는 데이터 소스 (소유 유닛)

| 소스 데이터 | 소유 유닛 | U6 사용처 |
|---|---|---|
| Cohort(status, sessionCount) | U2 | 완주 코스 수, 회차 수 |
| Session(status 인증) | U2/U4 | 출석률(인증 회차) |
| Enrollment(CONFIRMED) | U3 | 수료율 분모(확정 멘티 수) |
| AttendanceEvidence | U4 | 증빙 이력 조회(US-15) |
| Certificate | U5 | 증서 수, 수료율 분자 |
| FinalReport | U5 | 보고서 이력 조회(US-15) |

## 3. 산출 DTO (읽기 모델)

### 3.1 MetricsOverviewDto (US-14 / FR-11)
| 필드 | 타입 | 정의 |
|---|---|---|
| completedCohortCount | int | 완주(종료됨) 코호트 수 |
| attendanceRate | decimal | 전체 출석률(business-rules R-U6-04 정의) |
| completionRate | decimal | 수료율(R-U6-05 정의) |
| certificateCount | int | 발급 증서 수 |

### 3.2 EvidenceHistoryItemDto (US-15)
| 필드 | 타입 | 설명 |
|---|---|---|
| evidenceId, cohortTitle, sessionSeq, mimeType, size, uploadedBy(성명), createdAt | — | 증빙 이력 1건(관리자 뷰). 파일 다운로드는 U1 load 링크 |

### 3.3 ReportHistoryItemDto (US-15)
| 필드 | 타입 | 설명 |
|---|---|---|
| reportId, cohortTitle, authorName, hasAttachment, submittedAt | — | 보고서 이력 1건(관리자 뷰) |

## 4. 크로스유닛 계약 (U6가 요구하는 read-only 조회)

U6은 집계·이력을 위해 아래 read-only 데이터에 접근한다. 파일럿 기본 구현은 **리포팅 읽기 모델**(U6 소유 `MetricsRepository`/`HistoryRepository`의 read-only JPQL/native 집계·조인 쿼리)이며, 소스 테이블을 읽기만 한다(쓰기 없음). 대안으로 소유 유닛의 read API 조합도 허용(결과 동일). 어느 경로든 U6은 쓰기를 하지 않고 다른 유닛의 도메인 로직을 우회하지 않는다(순수 조회). 상세 쿼리는 business-logic-model §2/3/4 참조.

| 방향 | 계약 | 비고 |
|---|---|---|
| U6 → U2 (읽기) | 종료됨 코호트 수·회차 수 조회 | U2 제공 |
| U6 → U3 (읽기) | `EnrollmentService.confirmedCount/confirmedEnrollments` | U3 제공 |
| U6 → U4 (읽기) | 증빙 이력 목록(코호트/회차/업로더 조인) | U4 제공(HistoryService가 U4 데이터 조회) |
| U6 → U5 (읽기) | 증서 수·수료 데이터·보고서 이력 | U5 제공 |

## 5. 경계

- U6은 읽기 전용이므로 어떤 유닛도 수정하지 않고, 어떤 유닛도 U6을 호출하지 않는다(말단 소비자).
- 집계 산식·권한은 business-rules에서 정의. 지표는 실제 데이터와 일치해야 한다(FR-11).

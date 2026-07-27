# Domain Entities — U5 completion (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U5-completion
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U5 책임), `unit-of-work-story-map.md`(US-4 종료/US-11/12/13), `requirements-analysis/requirements.md`(FR-7/8/9), `application-design/components.md`(FinalReport·Certificate·SettlementStatus·FK), `component-methods.md`(CompletionService·ReportService), `services.md`(CompletionService·ReportService)
> 범위: U5 소유 FinalReport·Certificate·SettlementStatus 상세화. Cohort/Session(U2)·Enrollment(U3)·AttendanceEvidence/Session.status(U4)는 읽기.

## 1. U5 소유 엔티티

| 엔티티 | 소유 | U5에서의 처리 |
|---|---|---|
| FinalReport | **U5** | 최종 보고서 제출·조회 |
| Certificate | **U5** | 수료 판정 시 수료증 발급 |
| SettlementStatus | **U5** | 정산 조건 판정 결과 |
| Cohort/Session | U2 | 읽기 + 종료됨 상태 전이(U2 세터 호출) |
| Enrollment | U3 | 확정 멘티 조회(수료 판정 대상) |
| Session.status/AttendanceEvidence | U4/U2 | 인증 회차 수 조회(출석률) |

## 2. FinalReport 엔티티 (US-11 / FR-7)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| cohortId | BIGINT FK→Cohort.id | NOT NULL, ON DELETE CASCADE | 대상 코호트 |
| authorId | BIGINT FK→User.id | NOT NULL, ON DELETE RESTRICT | 작성자(멘토 또는 멘티) |
| body | TEXT | NOT NULL | 자유 서식 본문 |
| filePath | VARCHAR(512) | NULL 허용 | 선택 첨부(U1 FileStorage 저장 경로) |
| submittedAt | TIMESTAMP | NOT NULL | 제출 시각 |

- 자유 서식 텍스트 + 선택적 파일 첨부(FR-7, `cid:scope-definition`). 첨부는 U1 FileStorageService 사용(U4와 동일한 store+TX+delete 보상 패턴).

## 3. Certificate 엔티티 (US-12 / FR-8)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| cohortId | BIGINT FK→Cohort.id | NOT NULL, ON DELETE CASCADE | 대상 코호트 |
| menteeId | BIGINT FK→User.id | NOT NULL, ON DELETE RESTRICT | 수료 멘티 |
| imagePath | VARCHAR(512) | NOT NULL | 수료증 이미지 경로(U1 FileStorage) |
| issuedAt | TIMESTAMP | NOT NULL | 발급 시각 |

- UNIQUE(cohortId, menteeId): 멘티당 코호트별 수료증 1장(중복 발급 방지).
- 수료증은 **단순 이미지 1장**(`cid:market-research:c2` — 복잡한 발급 시스템 불필요). 코호트 종료 판정 시 수료 멘티에게 발급.

## 4. SettlementStatus 엔티티 (US-13 / FR-9)

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| cohortId | BIGINT FK→Cohort.id | NOT NULL, ON DELETE CASCADE, **UNIQUE** | 대상 코호트(1:1) |
| mentorId | BIGINT FK→User.id | NOT NULL, ON DELETE RESTRICT | 멘토 |
| satisfied | BOOLEAN | NOT NULL | 정산 조건 충족 여부 |
| evaluatedAt | TIMESTAMP | NOT NULL | 판정 시각 |

- 실제 정산/결제 없음. `satisfied`와 "정산 조건 충족" 메시지 수준(`cid:scope-definition:c5`, FR-9 — 온라인 결제/정산 Won't).

## 5. 판정 기준 (서로 다름 — cid:requirements-analysis:c1)

| 판정 | 대상 | 기준 |
|---|---|---|
| **수료(Certificate)** | 멘티 | 인증 출석 회차 수 ≥ 전체 회차 수의 80% (FR-8) |
| **정산(SettlementStatus)** | 멘토 | 전 회차 출석 인증 완료 **AND** 최종 보고서 제출 (FR-9) |

- 두 기준은 별개(멘티 수료 vs 멘토 정산). business-rules에서 정확한 산식·경계 정의.

## 6. 크로스유닛 계약 (U5 요구/제공)

| 방향 | 계약 | 비고 |
|---|---|---|
| U5 → U2 (읽기) | Cohort·Session(전체 회차 수·status) 조회 | U2 제공 |
| U5 → U2 (호출) | Cohort.status 세터(종료됨 전이) | U2 제공(U2 §8) |
| U5 → U3 (읽기) | `EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>` — 확정 멘티 **목록**(수료증 발급 순회용) | U3가 노출해야 할 계약(추가 요구) |
| (U5 자체) | `ReportService.mentorReportExists(cohortId, mentorId): boolean` — 정산 판정용 멘토 보고서 존재 조회 | U5 소유(FinalReport) |
| U5 → U3 (호출) | `NotificationService.notify` — 수료 결과 통지 | U3 제공 |
| U5 → U1 (호출) | `FileStorageService.store/load/delete` — 보고서 첨부·수료증 이미지 | U1 제공 |
| U6 → U5 (읽기) | 수료율·증서 수 집계, 보고서 이력(US-15) | U5 제공 |

## 7. 다른 유닛과의 경계

- U5는 종료 오케스트레이션·판정의 단일 소유자(`cid:units-generation:c2`). U2 데이터를 읽고 상태 전이를 U2 세터로 수행하되 U2는 U5를 호출하지 않는다(단방향, 순환 없음).
- 수료증·보고서 파일은 U1 FileStorage 사용(U4와 동일 보상 패턴).
- U6은 U5의 수료/정산/보고서 데이터를 read-only로 집계.

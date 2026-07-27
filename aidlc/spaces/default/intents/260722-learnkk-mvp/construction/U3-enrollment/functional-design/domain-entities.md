# Domain Entities — U3 enrollment (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U3-enrollment
> 리드 architect (서포트 developer)
> 상위 입력: `units-generation/unit-of-work.md`(U3 책임·복잡도 L), `unit-of-work-story-map.md`(US-6a/6b/7/8), `requirements-analysis/requirements.md`(FR-3/4/10), `application-design/components.md`(Enrollment·Notification·FK·UNIQUE), `component-methods.md`(EnrollmentService·AdminApprovalService·NotificationService), `services.md`(동시성 노트)
> 범위: U3가 소유하는 Enrollment·Notification 엔티티 상세화. Cohort(U2)·User(U1)는 참조만.

## 1. U3 소유 엔티티

| 엔티티 | 소유 | U3에서의 처리 |
|---|---|---|
| Enrollment | **U3** | 선착순 참여/대기/거절, 동시성 제어, 상태 전이 |
| Notification | **U3** | 상태 변경 알림 생성·조회. U5(수료 통지)·U8(승인/거절)에서 U3의 알림 API 호출 |
| Cohort | U2 | capacity·status 읽기(참여 판정) |
| User(mentee) | U1 | menteeId FK 참조 |

## 2. Enrollment 엔티티

`components.md` 정의를 FR-3에 맞춰 상세화.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| cohortId | BIGINT FK→Cohort.id | NOT NULL, ON DELETE CASCADE | 대상 코호트 |
| menteeId | BIGINT FK→User.id | NOT NULL, ON DELETE RESTRICT | 신청자(컨텍스트 역할 멘티) |
| status | ENUM | NOT NULL | 확정(CONFIRMED) / 대기중(WAITING) / 거절(REJECTED) |
| createdAt | TIMESTAMP | NOT NULL | 신청 시각(선착순 순서 기준) |
| decidedAt | TIMESTAMP | NULL | 관리자 승인/거절 시각 |
| version | BIGINT | NOT NULL, default 0 | 낙관적 락(`@Version`). 관리자 동시 승인/거절 경합 방지(알림 중복 방지, business-logic-model §4) |

- **UNIQUE(cohortId, menteeId)**: 한 사용자는 한 코호트에 하나의 Enrollment만. 중복·동시 경쟁의 최종 방어선(components.md, services.md 동시성 노트).
- createdAt은 선착순 판정 및 대기열 순서의 기준.

### 2.1 Enrollment 상태 생명주기

```
[신청]
  ├─ 정원 여유 ─> [확정(CONFIRMED)]
  └─ 정원 마감 ─> [대기중(WAITING)] --관리자 승인--> [확정(CONFIRMED)]
                              \--관리자 거절--> [거절(REJECTED)]
```
<!-- Text fallback: 신청 시 정원 여유면 확정, 마감이면 대기중이 된다. 대기중은 관리자 승인으로 확정, 거절로 거절 상태가 된다. 파일럿에서 취소·자동승격·재신청 전이는 없다. -->

- **파일럿 범위 외 전이(`cid:user-stories:c4`)**: 대기 신청 취소, 확정 취소 시 대기자 자동 승격, 거절 후 재신청. 이들 전이는 두지 않는다.
- 확정→(취소) 없음: 확정은 종결 상태(파일럿).

## 3. Notification 엔티티

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT PK | NOT NULL | 대리 키 |
| userId | BIGINT FK→User.id | NOT NULL, ON DELETE CASCADE | 수신자 |
| type | ENUM/VARCHAR | NOT NULL | 알림 유형(ENROLLMENT_CONFIRMED / ENROLLMENT_REJECTED / COMPLETION_RESULT 등) |
| message | VARCHAR(500) | NOT NULL | 표시 메시지 |
| read | BOOLEAN | NOT NULL, default false | 읽음 여부 |
| createdAt | TIMESTAMP | NOT NULL | 생성 시각 |

- NotificationService는 U3 소유이나 알림 생성은 여러 유닛의 이벤트에서 발생: U3(승인/거절), U5(수료 결과 통지). 타 유닛은 U3의 `notify(userId,type,message)` API를 호출(§5 계약).

## 4. 관계·FK 정책 (components.md 준수)

- Enrollment.cohortId → Cohort.id (CASCADE), Enrollment.menteeId → User.id (RESTRICT).
- Notification.userId → User.id (CASCADE).
- UNIQUE(cohortId, menteeId) on Enrollment — 동시성/중복 최종 방어선.

## 5. 크로스유닛 계약 (U3 제공/요구)

| 방향 | 계약 | 비고 |
|---|---|---|
| U2 → U3 (읽기) | **`EnrollmentService.confirmedCount(cohortId): int`** — 확정(CONFIRMED) 참여 수 | U2 정원 축소 검증(R-U2-09) 요구 계약. U3가 반드시 노출 |
| U5/U6 → U3 (읽기) | **`EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>`** — 확정 멘티 목록 | U5 수료증 발급 순회·U6 집계. U3가 노출 |
| U5/U8 → U3 (쓰기) | `NotificationService.notify(userId, type, message)` | U5 수료 통지·승인거절 알림 |
| U3 → U2 (읽기) | Cohort.capacity·status 조회 | 참여 판정용 |
| U3 → U1 (읽기) | User 조회(menteeId 유효성) | — |

## 6. 다른 유닛과의 경계

- U3는 Cohort(U2)를 읽어 정원·상태를 판정하되 Cohort를 수정하지 않는다(단방향).
- 확정 인원(confirmedCount)은 U3가 소유·집계해 U2·U6에 read-only로 제공.
- U3→U5 호출 없음. U5가 U3 데이터(확정 참여)를 읽어 수료 판정.

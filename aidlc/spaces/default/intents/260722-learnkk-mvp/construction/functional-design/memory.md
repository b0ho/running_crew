# Functional Design — 관찰 일지 (memory)

> Construction · functional-design 단계 진행 일지. 유닛별 반복(per-unit)으로 진행.
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-27T02:07:58Z — U1-foundation은 walking skeleton 유닛이며 kind=service; 엔진 directive의 produces는 3종(business-logic-model, business-rules, domain-entities)만 요구(frontend-components는 produces_kinds에서 ui-kind에만 적용되어 service 유닛에서는 미산출). auth UI(로그인/가입 폼) 흐름은 business-logic-model의 프론트 연동 절에 요약으로 포함.
- 2026-07-27T02:07:58Z — Construction 단계 질문은 예외적으로만 생성. U1은 인증 방식(자체 계정), 비밀번호(BCrypt 하드 제약), RBAC 관리자(Flyway 시드), PII 최소 수집(이메일·성명·닉네임)이 상위 스테이지에서 이미 확정되어 genuine gap이 없으므로 질문 라운드 없이 산출물 생성으로 진행.

## Deviations

- 2026-07-27T02:07:58Z — 스테이지 Step 3(질문 파일 생성)을 U1에서는 생략. 근거: Construction 최소 질문 원칙 + U1 결정사항이 memory(project/team)와 requirements에 모두 pin되어 미결 갭이 없음. 미결 사항 발생 시 해당 시점에 질문 파일을 생성.

## Tradeoffs

- 2026-07-27T02:07:58Z — 인증 토큰 방식: 파일럿 규모(<100명)·단일 서버 전제이므로 상태 저장 세션(Spring Security 기본 세션 쿠키) 대신 무상태 JWT를 강제하지 않음. 상위 결정에 토큰 방식 미확정 → business-rules에 "세션 기반 인증(HttpOnly 쿠키) 우선, JWT는 확장 시 재검토"로 기록. FE/BE 분리 저장소이므로 CORS + credentials 설정 필요.

## Open questions

- 2026-07-27T02:07:58Z — 로그인 실패 잠금(브루트포스 방지) 정책 미정. 파일럿 잔여 리스크(보안 위생 보류 결정과 정합)로 두되, 확장 시 rate-limit/lockout 도입 검토.

- 2026-07-27T02:20:00Z — (Deviation) U1에 frontend-components.md를 추가 산출. 엔진 directive의 produces는 3종만 요구(produces_kinds가 frontend-components를 ui-kind에만 적용)했으나, unit-of-work.md가 U1을 "service + 해당 UI 포함"으로 정의하고 인증 UI가 워킹 스켈레톤 관통 경로의 필수 요소이므로 optional_produces의 frontend-components를 산출해 리뷰어 지적(스코프 모순)을 해소. 이 판단은 모든 유닛이 kind=service이면서 UI를 포함하는 구조에 일반 적용될 수 있음 — 후속 유닛에서도 UI 있는 경우 frontend-components 산출 검토.
- 2026-07-27T02:20:00Z — (Interpretation) component-methods.md의 login(): AuthToken 시그니처와 파일럿 세션 인증 방식의 표현 차이를 "AuthToken은 세션으로 실체화"로 정합. 상위 inception 계약(immutable)을 수정하지 않고 functional-design에서 해석을 명시하는 방식으로 계약 드리프트 해소.
- 2026-07-27T02:20:00Z — (Tradeoff) 관리자 경로 인가를 필터체인 URL 열거 대신 @PreAuthorize 메서드 애너테이션으로 결정. U1은 역할 매핑만 확립하고 U3/U6이 애너테이션으로 자체 인가 → 유닛 간 필터 설정 충돌·순환 회피.

- 2026-07-27T02:30:00Z — (Open question→Contract) U2 정원 축소 검증(R-U2-09)이 U3의 확정 인원 조회를 요구하나 component-methods.md의 EnrollmentService는 join/myApplications만 선언. U2 설계에 크로스유닛 계약 `EnrollmentService.confirmedCount(cohortId): int`(read-only)를 명시하고 U3 functional-design이 이를 구현하도록 요구사항으로 기록. **U3 유닛 처리 시 이 계약을 반드시 노출할 것.**
- 2026-07-27T02:30:00Z — (Interpretation) component-methods의 CohortService.end는 U5 종료 오케스트레이션이 호출하는 내부 상태 전이 연산으로 실체화. 사용자 대면 종료 엔드포인트는 U2가 아니라 U5(CompletionService)가 노출. cid:units-generation:c2 소유권 단일화와 정합.
- 2026-07-27T02:30:00Z — (Deviation) 모집중→진행중 전이를 멘토 명시 액션(POST /cohorts/:id/start)으로 한정. 파일럿에 스케줄러가 없어 startDate 자동 도래 전이는 배제(확장 후속). 다른 유닛의 시간 기반 전이 설계 시 동일 원칙 적용.

- 2026-07-27T02:35:00Z — (Contract for U3) U3 functional-design은 `EnrollmentService.confirmedCount(cohortId): int`(read-only) 노출 필수. (Contract for U4) U4는 Session.status 예정→인증 전이 시 U2 제공 `SessionService.markVerified(sessionId)` 호출(리포지토리 직접 접근 금지). U2/U4/U3 유닛 처리 시 반영할 것.

- 2026-07-27T02:50:00Z — (Interpretation, U5-relevant) U4 증빙은 **회차 단위**(멘토가 회차 증빙 업로드 → 회차 인증 → 코호트 확정 멘티 전원의 해당 회차 출석 인정). 멘티별 개별 증빙은 파일럿 범위 외. 결과적으로 한 코호트 내 모든 확정 멘티의 출석률이 동일. **U5 수료 판정(출석≥80%) 설계는 이 전제(회차 단위·멘티 균일)를 사용할 것.** components.md의 AttendanceEvidence.menteeScope 필드는 이 모델에서 미사용(확장 시 멘티별 증빙 지점).
- 2026-07-27T02:50:00Z — (Contract, U6-relevant) 증빙 이력 조회(관리자, US-15)는 U6 HistoryService가 U4 AttendanceEvidence를 read-only로 읽어 제공. U4는 이력 데이터를 제공만.

- 2026-07-27T03:00:00Z — (Contract for U1) U4 uploadEvidence의 트랜잭션 롤백 보상 삭제를 위해 U1 `FileStorageService.delete(path)` 필요. component-methods.md는 store/load만 선언 → U1 functional-design(이미 작성됨, 게이트는 스테이지 말미 일괄)에 delete 추가 요구. code-generation 시 U1 FileStorageService에 delete 포함할 것. (U2→U3 confirmedCount와 동일한 크로스유닛 계약 패턴.)
- 2026-07-27T03:00:00Z — (Deviation) 파일 업로드는 store(비트랜잭션)를 트랜잭션 밖에서 먼저 수행하고, Evidence 저장+markVerified를 하나의 DB 트랜잭션으로 묶어 INV-U4-1 보장. 롤백 시 delete 보상, 실패 시 경로 로그+500. 고아 파일만 잔여 리스크(파일럿 허용). 파일+DB 결합이 있는 다른 유닛(U5 보고서/증서 파일)도 동일 패턴 적용.

- 2026-07-27T03:15:00Z — (Contract) U5 수료증 발급은 U3 `EnrollmentService.confirmedEnrollments(cohortId): List<EnrollmentDto>`(목록, count 아님) 필요 → U3 artifacts에 계약 materialize 완료(W-U3-3, §5). 정산 판정 멘토 보고서 존재는 U5 자체 `ReportService.mentorReportExists(cohortId, mentorId)`. 수료증 이미지: 멘티별 store→imagePath 누적→Certificate insert, TX 롤백 시 누적 imagePath 전부 delete(루프 레벨 보상, R-U5-08a). code-generation 시 U3에 confirmedEnrollments 포함할 것.

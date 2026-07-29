# Project-Level Rules

> Project-specific specialisation and corrections. Loaded after `org.md` and
> `team.md` as strict-additive guidance; contradictions with broader policy
> are rejected. Populated by practices-discovery and the self-learning loop.
>
> Use sparingly: most teams don't need a project layer. Reach for it
> only when this specific project needs stable, durable guidance beyond the
> team practice (for example, package-specific release checks or an additional
> regression suite for a legacy component).

## Way of Working

<!-- Project-specific specialisation. Example: -->
<!-- This monorepo requires package-scoped branch names and a package owner -->
<!-- review in addition to the team's normal merge policy. -->

- 직접 Build 시 범위를 좁게(필수 기능 중심) 유지하여 유지보수 부담을 완화한다. (learned 2026-07-23) <!-- cid:market-research:c4 -->
- 팀은 4~6명 전원 풀스택(React·Spring 능숙)·풀타임이며, 표준 CRUD 웹 기능 백로그를 충분히 감당한다. (learned 2026-07-24) <!-- cid:team-formation:c1 -->
- 전담 디자인/PM이 없으므로 파일럿 UI를 단순하게 유지해 갭을 완화한다(범위를 좁게 유지 원칙과 정합). (learned 2026-07-24) <!-- cid:team-formation:c2 -->
- practices-discovery는 greenfield에서 org.md 기본값 + project.md 결정을 기반으로 hub-and-spoke(리드 초안 → quality/developer/devsecops 블라인드 기여 → 인터뷰 → 리드 통합)로 진행한다. (learned 2026-07-24) <!-- cid:practices-discovery:c1 -->
- 선착순 참여(US-6a)와 정원 마감 대기·동시성(US-6b)을 분리해 워킹 스켈레톤 슬라이스(개설→선착순 참여→단일 회차 인증)를 얇게 유지한다. (learned 2026-07-24) <!-- cid:user-stories:c3 -->
- 워킹 스켈레톤(Bolt 1)은 전원 공동으로 규약·CI를 정렬하고, 이후 U3·U4 병렬 구간은 2개 페어로 나누되 지식 사일로 방지를 위해 로테이션·공동 리뷰한다. (learned 2026-07-26) <!-- cid:delivery-planning:c2 -->
## Walking Skeleton

<!-- Project-specific specialisation. Example: -->
<!-- The walking skeleton must exercise the legacy service adapter as well -->
<!-- as the new service boundary. -->

## Testing Posture

<!-- Project-specific specialisation. -->

## Deployment

<!-- Project-specific specialisation. -->

- 초기 파일럿은 로컬 서버에 배포하고 퍼블릭 클라우드 선택/비용 산정은 보류한다. 클라우드 이관은 확장 후속 과제다. (learned 2026-07-24) <!-- cid:feasibility:c2 -->
## Code Style

<!-- Project-specific specialisation. -->

- UI는 Tailwind CSS 경량 커스텀 디자인 시스템(기성 UI 라이브러리 미도입), 미니멀·중립 톤 + 강조색 1개로 한다. 코호트 상세에 멤버 탭(간단 목록)을 포함한다. (learned 2026-07-24) <!-- cid:refined-mockups:c1 -->
## Tech Stack

<!-- Technology choices locked for this project. -->

- 인증은 파일럿에서 상태 저장 세션(Spring Security HttpOnly·SameSite 쿠키) 우선, 무상태 JWT는 강제하지 않고 확장 시 재검토. FE/BE 분리 저장소이므로 CORS + credentials(세션 쿠키) 설정 필요. (learned 2026-07-27) <!-- cid:functional-design:c4 -->
## Decided

<!-- Decisions made in earlier stages that should not be re-asked. -->
<!-- Format: DECIDED: [decision] (Stage [slug], [date]) -->

- 핵심 문제는 문서·Jira·이메일·개별 채팅으로 산발된 사내 멘토링 과정을 단일 플랫폼으로 통합 관리하는 것으로 프레이밍한다. (learned 2026-07-23) <!-- cid:intent-capture:c1 -->
- 워크플로 스코프(enterprise=진행 범위/엄격도)와 제품 범위 결정(무결제·비공식 증서·소규모·화상 없음)은 별개로 취급한다. 제품 범위는 scope-definition/requirements에서 확정한다. (learned 2026-07-23) <!-- cid:intent-capture:c2 -->
- 오픈소스 LMS는 baseline 참고 기준으로만 사용하고 그대로 채택하지 않는다. 필요한 기능만 자체 구현한다. (learned 2026-07-23) <!-- cid:market-research:c1 -->
- 핵심 차별화는 산발된 멘토링 과정의 통합 관리와 사내 멘토링 흐름 맞춤이다. 증서(수료증·지급 기록증)는 단순한 수료증 이미지 1장 수준의 부가 산출물이며 차별화 요소가 아니다. (learned 2026-07-23, corrected 2026-07-22 by 사용자 게이트 피드백) <!-- cid:market-research:c2 -->
- "mvp"는 워크플로 스코프(enterprise)와 별개로 초기 최소 산출물(파일럿)을 의미한다. 사내 연동/클라우드/데이터 통제는 파일럿에서 제외하고 전사 확장 시점에 재평가한다. (learned 2026-07-24) <!-- cid:feasibility:c1 -->
- 인증은 자체 계정(이메일/사번)으로 하고 사내 SSO는 연동하지 않는다(출시 속도 우선). 보안은 검증된 Spring Security 기능으로 완화한다. (learned 2026-07-24) <!-- cid:feasibility:c3 -->
- 코호트 참여는 선착순 자동 참여로 하고 멘토 승인은 두지 않는다. 정원(capacity)을 두며, 정원 마감 시에만 시스템 관리자가 수동 점검·승인한다. (learned 2026-07-24) <!-- cid:scope-definition:c1 -->
- 출석은 멘토가 증빙자료를 첨부 업로드하면 인증되는 증빙 기반 인증제로 한다. 증빙 이력을 보관한다. (learned 2026-07-24) <!-- cid:scope-definition:c2 -->
- 최종 보고서 제출을 Must 기능으로 포함하고, 운영 지표 조회도 Must로 둔다(증빙 이력·최종 보고서 이력 조회 포함). (learned 2026-07-24) <!-- cid:scope-definition:c3 -->
- 회원가입 시 수집하는 개인정보는 이메일·성명·닉네임만으로 최소화한다(사번 등 미수집). (learned 2026-07-24) <!-- cid:scope-definition:c4 -->
- UI 내비게이션은 반응형으로 하며, 데스크톱은 상단 탭 바, 모바일은 하단 탭 바로 전환한다(좌측 사이드바 미사용). 로그인 후 첫 화면은 내 코호트 대시보드다. (learned 2026-07-24) <!-- cid:rough-mockups:c1 -->
- 출석 증빙 업로드는 파일 첨부(이미지/문서) 방식으로 하며 텍스트 메모 입력이 아니다. 첨부 즉시 해당 회차 출석이 인증된다. (learned 2026-07-24) <!-- cid:rough-mockups:c2 -->
- IDEATION 종합 결과 GO 판정으로 INCEPTION 진행. 미결 사항(수료·정산 조건 정의, 최종 보고서 형식, 증빙 파일 제약, 로컬 서버 구체 형태, 관리자/운영 역할 분리)은 INCEPTION에서 확정한다. (learned 2026-07-24) <!-- cid:approval-handoff:c1 -->
- 저장소는 FE(React)/BE(Spring) 분리 저장소로 한다(monorepo 아님). 분리로 인한 API 계약 조율 오버헤드는 OpenAPI 계약 동기화로 관리한다. (learned 2026-07-24) <!-- cid:practices-discovery:c2 -->
- 수료 기준(멘티: 인증 출석 회차 ≥ 전체의 80%)과 정산 조건 기준(멘토: 전 회차 출석 인증 완료 + 최종 보고서 제출)은 서로 다른 기준으로 분리 정의한다. (learned 2026-07-24) <!-- cid:requirements-analysis:c1 -->
- 코호트는 회차(세션) 단위 구조로 하며, 멘토가 개설 시 회차 수를 정하고 출석·증빙 인증은 회차 기반으로 이뤄진다. (learned 2026-07-24) <!-- cid:requirements-analysis:c2 -->
- 역할은 컨텍스트 역할(코호트 개설=멘토, 참여=멘티)과 명시적 관리자 역할(시드로 부트스트랩)로 구성한다. 관리자 권한은 일반 회원가입으로 부여되지 않는다. (learned 2026-07-24) <!-- cid:user-stories:c1 -->
- 코호트 상태는 모집중→진행중→종료됨으로 전이하며, 멘토의 '코호트 종료' 액션이 수료·정산 판정과 완주 코스 수 집계의 트리거다. (learned 2026-07-24) <!-- cid:user-stories:c2 -->
- 도메인은 10개 엔티티(User/Cohort/Session/Enrollment/AttendanceEvidence/Announcement/FinalReport/Certificate/SettlementStatus/Notification)와 12개 서비스로 분해한다. RBAC 최초 관리자는 Flyway 마이그레이션 시드로 부트스트랩하고, 테스트 전략(핵심 도메인 80%·Testcontainers·동시성 테스트)을 ADR로 문서화한다. (learned 2026-07-24) <!-- cid:application-design:c1 -->
- 코호트는 하드 삭제 대신 '종료됨' 상태 전이를 우선한다. FK 정책은 코호트 하위(회차·참여·증빙·공지·보고서·증서·정산) ON DELETE CASCADE, User는 이력 있으면 RESTRICT. (learned 2026-07-24) <!-- cid:application-design:c2 -->
- 구현은 6개 기능 수직 슬라이스 유닛으로 분해한다: U1 foundation, U2 cohort, U3 enrollment, U4 attendance, U5 completion, U6 admin-metrics. 의존 DAG는 U1→U2→(U3∥U4)→U5→U6. (learned 2026-07-24) <!-- cid:units-generation:c1 -->
- 코호트 종료 액션 오케스트레이션과 수료·정산 판정 소유권은 U5(completion)에 단일화한다. U5는 U2(코호트/회차)를 읽기만 하며 U2는 U5를 호출하지 않는다(순환 회피). (learned 2026-07-24) <!-- cid:units-generation:c2 -->
- Bolt 시퀀스는 U1(워킹 스켈레톤)→U2→(U3∥U4)→U5→U6이며 매 Bolt마다 사용자 게이트를 둔다. 최대 리스크인 U3 동시성은 U2 직후 조기 착수한다. (learned 2026-07-26) <!-- cid:delivery-planning:c1 -->
## Scope Overrides

<!-- Custom scope rules for this project. -->

- 시장 규모(TAM/SAM/SOM) 정량 산정은 생략한다. 외부 판매가 아닌 사내 도구이므로 사내 도입 규모로 대체한다. (learned 2026-07-23) <!-- cid:market-research:c3 -->
- 정산서는 실제 정산 처리 없이 '정산 조건 충족' 메시지 수준으로 단순화한다. 온라인 결제/정산은 범위에서 제외(Won't)한다. (learned 2026-07-24) <!-- cid:scope-definition:c5 -->
- 파일럿에서는 보안 위생(secret/SCA 스캔·파일 업로드 검증·TLS)을 보류하고 비밀번호 BCrypt 해싱만 하드 제약으로 둔다. 확장(클라우드 이관·전사 도입) 시 재검토한다. (learned 2026-07-24) <!-- cid:practices-discovery:c3 -->
- 대기 신청 취소, 확정 취소 시 대기자 자동 승격, 거절 후 재신청은 이번 파일럿 범위에서 제외한다. (learned 2026-07-24) <!-- cid:user-stories:c4 -->
## Forbidden

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: NEVER [behavior] (affirmed [date]) -->
<!-- Example: NEVER throw exceptions across service layer boundaries (affirmed 2026-05-17) -->

> `NEVER ...` 형식으로 표현된 금지 행동. (affirmed 2026-07-24)
- NEVER 장수명 feature 브랜치를 유지하지 않는다(1~2일 내 머지 원칙). (affirmed 2026-07-24)
- NEVER 프로덕션 배포를 **수동 승인 없이** 자동으로 실행하지 않는다(파일럿 단계에서 안전망 필요). (affirmed 2026-07-24)
- NEVER 비밀번호를 **평문으로 저장**하지 않는다. (affirmed 2026-07-24)
- NEVER 사내 SSO 연동을 초기 파일럿에 포함하지 않는다(출시 속도 우선; 자체 계정 인증 사용). (affirmed 2026-07-24)
- NEVER 퍼블릭 클라우드 인프라를 초기 파일럿에 포함하지 않는다(로컬 서버 호스팅; 클라우드 이관은 확장 후속 과제). (affirmed 2026-07-24)
## Mandated

<!-- Populated by practices-discovery affirmation gate. -->
<!-- Format: ALWAYS [behavior] (affirmed [date]) -->
<!-- Example: ALWAYS use Result<T,E> for fallible operations in service layer (affirmed 2026-05-17) -->

> `ALWAYS ...` 형식으로 표현된 필수 행동. (affirmed 2026-07-24)
- ALWAYS API 경계에서 요청/응답 **DTO를 사용**하고, JPA Entity를 직접 노출하지 않는다. 순환 참조·과다 노출·계약 결합을 방지한다. (affirmed 2026-07-24)
- ALWAYS 비밀번호는 **BCrypt로 해싱**하여 저장한다. 평문 또는 가역 암호화 저장을 금지한다. (affirmed 2026-07-24)
- ALWAYS 머지 전 CI에서 **린터와 테스트를 실행**하며, 실패 시 머지를 블럭한다. (affirmed 2026-07-24)
- ALWAYS Bolt 브랜치는 `main`에서 분기하고 **squash-merge**로 `main`에 다시 머지한다. (affirmed 2026-07-24)
- ALWAYS 개인정보는 **최소 수집 원칙**을 따른다(이메일·성명·닉네임만; 사번 등 미수집). (affirmed 2026-07-24)
## Corrections

<!-- Project-specific corrections from human feedback. -->
- 증서(수료증·지급 기록증)는 차별화 요소가 아니라 단순한 수료증 이미지 1장 수준의 부가 산출물이다. 별도의 복잡한 발급 시스템 불필요. (corrected 2026-07-22, market-research 게이트 피드백)
<!-- Format: NEVER/ALWAYS [behavior] (learned [date]) -->
- Construction 단계에서는 질문을 예외적으로만 생성한다. 상위 스테이지(ideation/inception)에서 결정이 pin되어 genuine gap이 없으면 질문 라운드·질문 파일을 생략하고 산출물 생성으로 진행한다. (learned 2026-07-27) <!-- cid:functional-design:c2 -->
- 서브에이전트(리뷰어 등)를 디스패치할 때, 산출물·리뷰(## Review 포함)를 반드시 한글로 작성하도록 브리핑에 명시한다. team.md Mandated(모든 산출물 한글) 규칙이 서브에이전트 출력에도 적용됨 — 리뷰어는 기본 영어로 작성하므로 명시 지시 필요. (learned 2026-07-27) <!-- cid:nfr-design:lang-subagent -->
- 유닛 functional-design에서 확립한 크로스유닛 계약(서비스 시그니처)이 중앙 application-design/component-methods.md 레지스트리에 누락될 수 있으므로, nfr-design(및 이후 설계) 리뷰 시 계약 레지스트리와의 정합을 점검하고 누락분을 조율 반영한다. (learned 2026-07-28) <!-- cid:nfr-design:contract-registry-reconciliation -->
- 파일럿 단일 소유·저동시성 상태 전이는 @Version 낙관적 락 대신 상태 가드 조건 UPDATE(UPDATE ... WHERE id=? AND status=?, 영향 행 0이면 409)를 기본 메커니즘으로 한다. 다중 편집자·고동시성으로 확장 시 @Version 재검토. (learned 2026-07-28) <!-- cid:nfr-design:state-transition-guarded-update -->
- 인덱스/마이그레이션의 컬럼명이 엔티티 필드명과 일치하는지 설계·리뷰 단계에서 대조한다(예: FinalReport.submittedAt ↔ final_report(submitted_at)). 불일치는 code-generation에서 실패하는 구조적 결함이므로 크로스유닛 리뷰에서 조기 검출한다. (learned 2026-07-28) <!-- cid:infrastructure-design:index-entity-fieldname-consistency -->

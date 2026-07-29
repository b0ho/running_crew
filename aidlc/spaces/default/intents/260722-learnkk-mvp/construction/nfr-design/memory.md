# NFR Design — 관찰 일지 (memory)

> Construction · nfr-design 단계 진행 일지. 유닛별 반복.
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-27T04:00:00Z — nfr-design은 nfr-requirements의 "무엇(목표)"에 대한 "어떻게(구체 설계)"다. 파일럿은 고급 NFR 패턴(서킷브레이커·캐시 티어·오토스케일·멀티리전)을 보류하므로, 설계는 파일럿에서 실제 채택하는 구체 메커니즘(세션/BCrypt 설정, 트랜잭션 경계, 로컬 스토리지, recreate 배포, 인덱스)을 명시하고 보류 패턴을 명확히 스코프아웃한다.
- 2026-07-27T04:00:00Z — 리뷰어 서브에이전트에는 team.md Mandated(한글 산출물) 준수를 위해 `## Review`를 한글로 작성하도록 명시 지시한다(project.md ## Corrections `cid:nfr-design:lang-subagent` 적용).

## Deviations

- 2026-07-27T04:00:00Z — Construction 질문 라운드 생략(project.md ## Corrections c2): NFR 설계가 nfr-requirements+관행에 pin되어 genuine gap 없음.

## Tradeoffs

- 2026-07-27T04:00:00Z — 파일럿에서 캐시·서킷브레이커·오토스케일 미도입. 근거: <100명·로컬 단일 서버에서 불필요한 복잡도. 대신 확장 트리거와 전환 경로(FileStorageService 백엔드 교체, 세션 스토어 외부화/JWT, 지표 캐시)를 설계에 명시해 확장 시 매끄러운 전환 보장.

## Open questions

- 2026-07-27T04:00:00Z — 헬스체크·모니터링 상세(엔드포인트·지표 수집)는 Operation 단계 observability-setup에서 구체화. 현재는 설계 수준 명시.

## Interpretations

- 2026-07-28T00:00:00Z — 매직바이트(파일 콘텐츠 시그니처) 검증 책임을 공통 FileStorageService가 아니라 도메인 서비스(U4 AttendanceService)에 배치; 근거: 증빙 특화 강화 규칙이라 응집도가 높고 U1 공통 계약을 변경하지 않아도 됨. FileStorageService는 U1 기본 검증(확장자+선언 MIME+크기)만.
- 2026-07-28T00:00:00Z — U2↔U3 등 "크로스유닛 서비스 호출"은 파일럿 단일 배포단위(learnkk-api)의 in-process 호출로 해석. 따라서 "원격 API 장애(503)" 자리표시를 500(내부 오류)+롤백으로 구체화하고 409는 순수 비즈니스 규칙 위반에만 사용.

## Deviations

- 2026-07-28T00:00:00Z — nfr-design(U4/U5) 리뷰 중 공유 계약 레지스트리 `inception/application-design/component-methods.md`를 조율 편집: FileStorageService.delete, EnrollmentService.confirmedCount/confirmedEnrollments, SessionService.markVerified, CohortService.start/transitionToEnded를 추가. 근거: 이 계약들은 각 유닛 functional-design(승인됨)에서 이미 확립됐으나 중앙 레지스트리에 누락 → 리뷰어가 통합 실패 리스크로 지적(U4 Blocking 1). 유닛 설계와의 정합 조율(가산적 reconciliation)이며 새 설계 결정 아님.

## Tradeoffs

- 2026-07-28T00:00:00Z — 상태 전이 동시성(U2 코호트 상태, U3 관리자 승인)을 `@Version` 낙관적 락 대신 **상태 가드 조건 UPDATE**(`WHERE status=?`, 영향행 0이면 409)로 확정. 근거: 파일럿 단일 소유·저동시성에서 전 필드 낙관적 락 오버헤드/충돌 노이즈 불필요, 전이 불변식만 정확히 보호. 확장(다중 편집자) 시 @Version 재검토.
- 2026-07-28T00:00:00Z — 알림(U3 notify) 생성을 확정/종료 트랜잭션과 **동일 트랜잭션**으로 확정(U3 join, U5 endCohort). 근거: 단일 인스턴스·단일 DB에서 "확정됐는데 알림 유실" 창 제거가 가장 단순·안전. 비동기 best-effort 알림은 메시지 브로커 도입 확장 과제.

## Open questions

- 2026-07-28T00:00:00Z — 유닛 functional-design에서 확립한 크로스유닛 계약을 중앙 component-methods.md에 자동 반영하는 절차(현재는 nfr-design 리뷰 시점에 수동 조율)가 표준 관행으로 승격될지 게이트에서 확인 필요.

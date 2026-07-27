# Initiative Brief — LearnKK (사내 멘토링 통합 플랫폼 파일럿)

> Ideation · approval-handoff 단계 산출물 · IDEATION → INCEPTION 핸드오프 브리프
> 리드 delivery (서포트: product) · 비기술 이해관계자 대상 요약
> 상위 입력: intent-statement · scope-document · intent-backlog · competitive-analysis · feasibility-assessment · constraint-register · team-assessment · wireframes

## 요약 (Executive Summary)

LearnKK는 문서·Jira·이메일·개별 채팅으로 흩어진 **사내 멘토링 과정을 단일 플랫폼으로 통합**하는 파일럿이다.
직접 Build(React+Spring, 로컬 서버)로 소규모(<100명) 파일럿을 빠르게 출시해, 통합 관리와 사내 흐름 맞춤이라는
핵심 가치를 검증한다. 타당성 판정은 **GO**, 팀(4~6명 풀스택·풀타임)은 실행 준비 완료.

## 목표 & 가치 (Goal & Value)

- `intent-statement.md`: 산발된 멘토링 과정의 통합 관리·추적 가능성 확보.
- 차별화: **통합 관리 + 사내 멘토링 흐름 맞춤** (증서는 단순 부가 산출물).

## 범위 (Scope — scope-document 요약)

- **Must**: 코호트 개설(정원) · 선착순 참여(정원 마감 시 관리자 승인) · 출석 증빙 인증 · 공지 · 회원가입(이메일·성명·닉네임) · 수료증/정산서(단순) · 최종 보고서 · 운영 지표·이력 조회
- **첫 슬라이스**: 코호트 개설 → 선착순 참여
- **Won't**: 온라인 결제/정산 · 내장 화상 · SSO · 클라우드 · 기존 시스템 연동 · 복잡한 증서 검증

## 접근 방식 (Approach)

- **Build**(competitive-analysis/build-vs-buy): 상용은 소규모에 가격 부담, 오픈소스는 참고만.
- **스택**: React + Spring, 초기 **로컬 서버** 호스팅.
- **인증**: 자체 계정(SSO 미연동).
- **UI**: 반응형(데스크톱 상단탭→모바일 하단탭), 내 코호트 대시보드 첫 화면.

## 제약 & 리스크 (Constraints & Risks — constraint-register/raid 요약)

- 로컬 서버 → 확장 시 클라우드 이관 필요(컨테이너화 권고).
- 직원 데이터 취급: 파일럿은 특별 통제 없음(임시), 전사 확장 시 정책 재확인.
- 전담 디자인/PM 부재 → UI 단순 유지로 완화.

## 팀 & 실행 (Team & Delivery — team-assessment 요약)

- 4~6명 전원 풀스택(React·Spring 능숙)·풀타임 → 백로그 감당 가능.
- 작업 축: 참여 흐름 / 활동·증빙 / 계정·지표.

## 핸드오프 (Handoff to INCEPTION)

- 다음 단계: **Practices Discovery** → Requirements/User Stories → Units/Design.
- INCEPTION에서 확정할 오픈 이슈: 수료·정산 조건 정의, 최종 보고서 형식, 증빙 파일 제약, 로컬 서버 구체 형태, 관리자/운영 역할 분리 여부.
- 권고 판정: **GO — INCEPTION 진행**.

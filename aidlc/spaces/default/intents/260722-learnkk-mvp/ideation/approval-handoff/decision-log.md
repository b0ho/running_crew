# Decision Log — LearnKK (IDEATION 단계 종합)

> Ideation · approval-handoff 단계 산출물 · IDEATION에서 내린 핵심 의사결정 기록
> 출처: 각 단계 questions/artifacts 및 project.md 학습(§13)

## 핵심 의사결정 (Key Decisions)

| # | 결정 | 근거/출처 | 대안 |
|---|---|---|---|
| D1 | 워크플로 스코프 mvp → **enterprise**(전체 32스텝) | 사용자 결정 | mvp/feature |
| D2 | 문제 = 산발된 사내 멘토링 과정의 **통합 관리** | intent-capture | — |
| D3 | 대상 = 단일 회사 내부(직원↔직원), 소규모 파일럿(<100명) | intent-capture Q2/Q6 | 외부 공개 |
| D4 | **직접 Build** (React+Spring) | market-research Q5, build-vs-buy(+6) | Buy/Partner |
| D5 | 오픈소스는 **참고 baseline만**, 채택 아님 | market-research Q1 | 오픈소스 채택 |
| D6 | 차별화 = 통합 관리·사내 흐름 맞춤 / **증서는 단순 이미지 1장** | 게이트 피드백(정정) | 증서를 차별화로 |
| D7 | 초기 호스팅 = **로컬 서버**(클라우드 보류) | feasibility Q3 | AWS/온프레미스 |
| D8 | 인증 = **자체 계정**(이메일·성명·닉네임만), SSO 미연동 | feasibility Q1 / scope Q1 | 사내 SSO |
| D9 | 참여 = **선착순 자동**, 정원 마감 시 **관리자 승인** | scope Q1 | 멘토 승인 |
| D10 | 출석 = **멘토 증빙 파일 첨부 → 인증제** | scope Q1 / mockups | 멘티 자가 체크 |
| D11 | 정산서 = **"정산 조건 충족" 메시지** 수준(온라인 정산 제외) | scope Q1 | 온라인 정산 |
| D12 | **최종 보고서 제출** + **운영 지표** Must 포함 | scope Q2 | Should/제외 |
| D13 | UI = **반응형**(데스크톱 상단탭→모바일 하단탭), 첫 화면 내 코호트 대시보드 | rough-mockups Q1~Q3 | 사이드바 |
| D14 | 팀 = 4~6명 풀스택·풀타임 → 백로그 감당 | team-formation | — |

## 미결 사항 (Deferred to INCEPTION)

- 수료 조건 임계값 및 "정산 조건" 정의 (requirements)
- 최종 보고서 형식(자유 vs 템플릿), 증빙 파일 형식/용량 제한 (requirements/functional-design)
- 로컬 서버 구체 형태(개발자 PC vs 사내 상시 서버) (infra)
- 시스템 관리자와 운영 담당자 역할 분리 여부 (requirements)
- 오픈소스 참고 범위(참고만 vs 컴포넌트 채택) (application-design)

## 정정 이력 (Corrections)

- C1 (2026-07-22): 증서를 "차별화 요소"에서 "단순 수료증 이미지 1장 수준 부가 산출물"로 정정 (market-research 게이트 피드백).

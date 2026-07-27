# Constraint Register — LearnKK (사내 멘토링 통합 플랫폼)

> Ideation · feasibility 단계 산출물 · 기술/조직/규제 제약 등록부
> 상위 입력: `intent-capture/intent-statement.md`, `market-research/build-vs-buy.md`, `market-research/market-trends.md`
> 근거: feasibility-questions.md (Q1~Q6)

## 기술 제약 (Technical Constraints)

| ID | 제약 | 출처 | 영향 |
|---|---|---|---|
| TC-1 | 스택은 React + Spring 로 고정 | Q2 | 설계·구현 언어/프레임워크 확정 |
| TC-2 | 초기 호스팅은 로컬 서버 (클라우드 미사용) | Q3 | 배포·확장 방식 제한, 클라우드 이관은 후속 과제 |
| TC-3 | 인증은 자체 계정(이메일/사번), 사내 SSO 연동 없음 | Q1 | 계정/세션 자체 구현 필요, SSO 통합 제외 |
| TC-4 | 기존 사내 시스템(Jira/문서 등) 연동 없음 | Q1 | 독립 신규 시스템, 데이터 마이그레이션 불필요 |
| TC-5 | 증서는 단순 이미지 1장 수준 부가 산출물 | 사용자 확정(competitive-analysis) | 복잡한 발급/검증 시스템 불필요 |

## 조직 제약 (Organizational Constraints)

| ID | 제약 | 출처 | 영향 |
|---|---|---|---|
| OC-1 | 빠른 파일럿 우선 (수 주 내 최소 동작) | Q5 | 기능 범위를 필수 중심으로 좁게 유지 |
| OC-2 | 조직적 블로커/사내 표준 강제 없음 | Q6 | 승인·표준 준수 오버헤드 낮음 |
| OC-3 | 초기 도입 규모 소규모 파일럿(<100명) | intent-statement Q6 | 성능/확장 요구 낮음, 경량 운영 |

## 규제/컴플라이언스 제약 (Regulatory Constraints)

| ID | 제약 | 출처 | 영향 |
|---|---|---|---|
| RC-1 | 파일럿은 특별 통제 없음(사내 일반 도구 수준, 임시) | Q4=A | 초기엔 규제 통제 미적용 |
| RC-2 | 직원 데이터(참여·출석·강의 수행 기록) 포함 | compliance 관점 | 전사 확장 시 접근통제·보관정책 재확인 필요 (RAID로 이월) |

## 참고

- 위 제약은 초기 최소 산출물(파일럿) 기준이며, 전사 확장 시 TC-2(호스팅)·RC-1/RC-2(데이터 통제)는 재평가 대상이다.
- `build-vs-buy.md`의 "범위를 좁게 유지" 권고 및 project.md의 Way of Working 학습과 정합한다.

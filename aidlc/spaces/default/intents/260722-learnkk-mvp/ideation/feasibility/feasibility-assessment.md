# Feasibility Assessment — LearnKK (사내 멘토링 통합 플랫폼)

> Ideation · feasibility 단계 산출물 · 관점: architect(리드) + aws-platform + compliance
> 상위 입력: `intent-capture/intent-statement.md`, `market-research/competitive-analysis.md`, `market-research/market-trends.md`, `market-research/build-vs-buy.md`
> 근거: feasibility-questions.md (Q1~Q6)

## 기술 타당성 (Technical Viability)

`build-vs-buy.md`의 직접 Build 결정을 전제로, 초기 최소 산출물(파일럿)의 기술 타당성을 평가한다.

- **스택**: React(프론트) + Spring(백엔드) — 팀이 익숙한 표준 웹 스택(Q2). 성숙도·자료가 풍부해 위험 낮음.
- **호스팅**: 초기엔 **로컬 서버**(Q3). 퍼블릭 클라우드/온프레미스 인프라 없이 파일럿을 띄워 검증. 클라우드 이관은 확장 시점의 후속 과제.
- **인증**: 사내 SSO 연동 없이 **자체 계정(이메일/사번) 로그인**(Q1). 연동 복잡도를 제거해 파일럿을 빠르게 출시(Q5=빠른 파일럿).
- **기능 범위**: `intent-statement.md`의 통합 관리(코호트 개설·모집·진도/출석·공지)가 핵심. 증서는 단순 이미지 1장 수준의 부가 산출물(사용자 확정, competitive-analysis 참조).
- **판정**: 표준 스택 + 로컬 호스팅 + 자체 로그인 + 좁은 기능 범위 → **기술 타당성 높음, 리스크 낮음**.

## AWS/인프라 관점 (Platform View)

- 초기 파일럿은 **클라우드 미사용**(Q3=로컬 서버). AWS 서비스 선택·비용 산정은 이 단계에서 보류.
- `market-trends.md`가 지적한 경량·자체호스팅 선호와 정합. 파일럿 검증 후 확장 시 클라우드 이관(컨테이너화·관리형 DB 등)을 재평가.
- 권고: 로컬이라도 **컨테이너(Docker) 기반**으로 구성해 두면 이후 클라우드 이관 비용이 낮아짐 (application-design에서 결정).

## 컴플라이언스 관점 (Compliance View)

- 사용자 답변 기준 파일럿은 **사내 일반 도구 수준, 특별 통제 없음(임시)**(Q4=A).
- 다만 멘토링 참여·출석·강의 수행 기록은 **직원 데이터(내부/PII성)** 를 포함 → `raid-log.md`에 가정·리스크로 등재.
- 로컬 서버 보관은 외부 반출이 없다는 점에서 초기 리스크를 낮추나, 전사 확장(intent-statement Q6) 시 접근통제·보관정책 재확인 필요.

## 종합 판정 (Overall)

- **GO** — 초기 최소 산출물(파일럿)은 기술·인프라·컴플라이언스 모두 낮은 리스크로 실현 가능.
- 전제: 기능 범위를 필수(table-stakes) 중심으로 좁게 유지(project.md Way of Working 학습과 정합), 클라우드/사내 데이터 통제는 확장 시점에 재평가.

# Mob / Team Composition — LearnKK (파일럿)

> Ideation · team-formation 단계 산출물
> 상위 입력: `scope-definition/intent-backlog.md`, `team-assessment.md`(동일 단계)
> 근거: team-formation-questions.md (Q1=4~6명, Q4=풀타임)

## 작업 조직 제안 (Proposed Composition)

전원 풀스택 4~6명·풀타임이므로, 도메인별로 얇게 나누되 상호 리뷰가 쉬운 **경량 페어 중심** 구성을 제안한다.

- **작업 축 A — 참여 흐름**: 코호트 개설(정원)·선착순 참여·정원 마감 시 관리자 승인·공지 (B-M1, B-M2, B-M4)
- **작업 축 B — 활동/증빙**: 출석 증빙 인증·수료증/정산서·최종 보고서 (B-M3, B-M6, B-M7)
- **횡단 — 계정/지표**: 회원가입·로그인(B-M5), 운영 지표·이력 조회(B-M8) — 초기엔 공용으로 착수

## 협업 방식

- 첫 슬라이스(코호트 개설 → 선착순 참여)는 **전원 또는 코어 페어**가 함께 관통(walking skeleton)하여 아키텍처·규약을 정렬.
- 이후 작업 축 A/B로 페어를 나눠 병렬 진행, 계정·지표는 공용 모듈로 공유.
- AI-DLC Construction 단계에서는 이 축 구분이 Bolt/작업 단위 분해의 밑바탕이 된다(units-generation에서 구체화).

## 리스크 & 권고

- 소규모라 지식 사일로 방지를 위해 축 간 **주기적 로테이션/공동 리뷰** 권고.
- 전담 디자인 부재 → UI 규약을 초기에 한 번 합의해 두면 축 간 일관성 유지에 유리.

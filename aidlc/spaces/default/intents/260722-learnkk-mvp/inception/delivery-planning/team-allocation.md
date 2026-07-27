# Team Allocation — LearnKK (파일럿)

> Inception · delivery-planning 단계 산출물
> 상위 입력: `team-formation`(4~6명 풀스택·풀타임), `bolt-plan.md`, `team-practices.md`
> 근거: delivery-planning-questions.md (Q3=A)

## 팀

- 4~6명 전원 풀스택(React+Spring), 풀타임. 전담 QA/디자인 없음(개발자 겸임).

## Bolt별 배정

| Bolt | 배정 |
|---|---|
| Bolt 1 (스켈레톤) | **전원 공동**(아키텍처·규약·CI 정렬) |
| Bolt 2 (U2) | 코어 페어 |
| Bolt 3 (U3) ∥ Bolt 4 (U4) | **2개 페어로 분할 병렬**(U3 동시성 페어 + U4 파일 페어) |
| Bolt 5 (U5) | 페어 |
| Bolt 6 (U6) | 페어 |

## 협업 방식

- 스켈레톤은 전원 공동으로 규약(에러 DTO·API 계약·테스트 셋업) 확립.
- 병렬 구간(U3·U4)은 페어 로테이션·공동 리뷰로 지식 사일로 방지(team-assessment 권고).
- 전담 QA 없으므로 PR 리뷰에 테스트 리뷰 포함.

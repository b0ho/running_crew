# Delivery Planning — 확인 질문 (LearnKK 파일럿)

> Inception · delivery-planning · 리드 delivery (서포트 architect)
> 상위 입력: units-generation(DAG), stories, mockups, design, team-practices
> 각 문항 **A=권고안**. "전부 권고안대로" 가능.
> (team-practices: 워킹 스켈레톤 ON, 매 Bolt 게이트 — 이미 확정)

---

## 제안 Bolt 계획 (유닛 DAG U1→U2→(U3∥U4)→U5→U6 기반)

| Bolt | 유닛 | 성격 | 게이트 |
|---|---|---|---|
| Bolt 1 | U1 foundation | **워킹 스켈레톤**(실행 앱+인증 end-to-end) | 단독·게이트 |
| Bolt 2 | U2 cohort | 코호트·회차·공지 | 게이트 |
| Bolt 3 | U3 enrollment | 선착순·대기·승인·알림 | 게이트 |
| Bolt 4 | U4 attendance | 회차 증빙 인증 | 게이트 |
| Bolt 5 | U5 completion | 보고서·수료·정산 | 게이트 |
| Bolt 6 | U6 admin-metrics | 지표·이력 | 게이트 |

- U3·U4는 U2 이후 병렬 가능 → 팀 여력 시 동시 진행.

### Q1. 위 Bolt 순서(6개)에 동의하시나요?
- A. **(권고)** 동의 — U1 스켈레톤 우선, 이후 DAG 순
- B. 순서 조정 필요(X에 기재)
- X. 기타

[Answer]:a

### Q2. 시퀀싱 우선순위 성향은?
- A. **(권고)** 스켈레톤 우선 후 의존 순서(리스크 높은 U3 동시성은 U2 직후 조기 착수)
- B. 가치 우선(사용자 체감 기능 먼저)
- C. 리스크 우선(동시성·판정 등 난제 최우선)
- X. 기타

[Answer]:a

### Q3. 팀 배정(4~6명 풀스택)은?
- A. **(권고)** 스켈레톤(Bolt1)은 전원 공동 → 이후 U3·U4 병렬 시 2개 페어로 분할
- B. 유닛별 1명 전담
- C. 전원 항상 모브
- X. 기타

[Answer]:a

### Q4. 외부 의존성 확인 (파일럿)
- A. **(권고)** 없음 — 로컬 서버·자체 계정·오픈소스 라이브러리만(외부 SaaS/API 연동 없음)
- B. 있음(X에 기재)
- X. 기타

[Answer]:a

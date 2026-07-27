# Units Generation — 분해 계획 질문 (LearnKK 파일럿)

> Inception · units-generation · 리드 architect (서포트 delivery)
> 상위 입력: application-design(5), requirements, stories
> 각 문항 **A=권고안**. "전부 권고안대로" 가능. (구현 순서는 다음 단계 Delivery Planning에서 결정 — 여기선 무엇이 무엇에 의존하는지 위상만 정의)

---

### Q1. 유닛 경계 전략은?
- A. **(권고)** 기능/도메인 수직 슬라이스(각 유닛이 FE+BE 함께) — Bolt 기반 전달에 정합
- B. 계층별(FE 유닛 / BE 유닛 분리)
- C. 서비스별(백엔드 서비스 단위)
- X. 기타

[Answer]:a

### Q2. 유닛 granularity는?
- A. **(권고)** 중간 입자(기능군 6개 내외)
- B. 세분화(스토리 단위로 잘게)
- C. 크게(2~3개 큰 덩어리)
- X. 기타

[Answer]:a

### Q3. 배포 모델은?
- A. **(권고)** 단일 배포(로컬 Docker, FE+BE 각 1 컨테이너) — 유닛은 개발 단위이지 독립 배포 아님
- B. 유닛별 독립 배포
- X. 기타

[Answer]:a

---

## 제안 분해안 (권고, Q1=A·Q2=A·Q3=A 기준)

| 유닛 | kind | 내용 | 스토리 | 의존 |
|---|---|---|---|---|
| U1 foundation | service | 저장소 스캐폴딩(FE/BE), 공통 에러DTO·Security·FileStorage, User/Auth(회원가입·로그인), RBAC 시드 | US-0,1,2 | — |
| U2 cohort | service | 코호트 개설·수정·종료, 회차 생성, 공지 | US-3,4,5 | U1 |
| U3 enrollment | service | 선착순 참여·대기·관리자 승인·알림 | US-6a,6b,7,8 | U2 |
| U4 attendance | service | 회차 증빙 인증·진도 조회 | US-9,10 | U2 |
| U5 completion | service | 최종 보고서·수료 판정·정산 판정·수료증 | US-11,12,13 | U3,U4 |
| U6 admin-metrics | service | 운영 지표·증빙/보고서 이력 | US-14,15 | U3,U4,U5 |

- 병렬 기회: U3와 U4는 U2 이후 서로 독립(병렬 가능).
- 워킹 스켈레톤 후보 슬라이스는 U1→U2→U3(6a) 일부 관통(순서는 Delivery Planning에서 결정).

### Q4. 위 제안 분해안(6개 유닛)에 동의하시나요?
- A. **(권고)** 동의 — 이대로 생성
- B. 수정 필요(X에 기재)
- X. 기타

[Answer]:a

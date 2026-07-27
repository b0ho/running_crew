# Bolt Plan — LearnKK (파일럿)

> Inception · delivery-planning 단계 산출물 · 리드 delivery (서포트 architect)
> 상위 입력: `units-generation/unit-of-work*.md`(DAG), `user-stories/stories.md`, `refined-mockups/mockups.md`, `application-design/components.md`, `practices-discovery/team-practices.md`
> 근거: delivery-planning-questions.md (Q1~Q4 전부 권고안)
> team-practices: 워킹 스켈레톤 ON, Construction Autonomy = 매 Bolt 게이트.

## Bolt 시퀀스

| Bolt | 유닛 | 성격 | 주요 스토리 |
|---|---|---|---|
| **Bolt 1** | U1 foundation | **워킹 스켈레톤**(단독·게이트) | US-0, US-1, US-2 |
| Bolt 2 | U2 cohort | 일반 Bolt | US-3, US-4(수정), US-5 |
| Bolt 3 | U3 enrollment | 일반 Bolt (U2 직후, 조기 착수) | US-6a, US-6b, US-7, US-8 |
| Bolt 4 | U4 attendance | 일반 Bolt (U2 이후, U3와 병렬 가능) | US-9, US-10 |
| Bolt 5 | U5 completion | 일반 Bolt | US-4(종료), US-11, US-12, US-13 |
| Bolt 6 | U6 admin-metrics | 일반 Bolt | US-14, US-15 |

## 워킹 스켈레톤 (Bolt 1)

- 목적: 실행 가능한 앱 골격 + 인증을 end-to-end로 세워 아키텍처·CI·배포 파이프라인을 검증.
- 범위: FE/BE 스캐폴딩, 공통(에러 DTO·Security·FileStorage·springdoc), 회원가입/로그인, RBAC 시드.
- **단독·게이트**: Bolt 1 완료 후 사용자 승인 → 이후 Bolt 진행. Autonomy = 매 Bolt 게이트(team-practices).

## 시퀀싱 근거 (요약; 상세는 risk-and-sequencing-rationale.md)

- 스켈레톤 우선(Q2=A): 위상 선행 U1을 스켈레톤으로.
- U3(동시성)는 최대 리스크이므로 U2 직후 조기 착수.
- U3·U4 병렬 가능(팀 여력 시).

## Bolt 게이트 정책

- 매 Bolt 게이트(사용자 확인 후 다음 Bolt). Bolt 브랜치 squash-merge → main.

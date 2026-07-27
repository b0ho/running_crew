# Unit of Work Dependency — LearnKK (파일럿)

> Inception · units-generation 단계 산출물 · 유닛 간 의존 위상(DAG)
> 상위 입력: `unit-of-work.md`, `application-design/component-dependency.md`, `services.md`, `components.md`, `component-methods.md`, `decisions.md`, `requirements-analysis/requirements.md`, `user-stories/stories.md`
> 위상(topology)만 기술 — 구현 순서/크리티컬 패스는 Delivery Planning(2.8)에서 결정.

## 의존 DAG (프로즈)

- U1 foundation: 의존 없음(모든 유닛의 선행 — 공통 인프라·인증).
- U2 cohort: U1에 의존(인증·공통).
- U3 enrollment: U2에 의존(코호트·정원·회차 존재 전제).
- U4 attendance: U2에 의존(회차 존재 전제).
- U5 completion: U2, U3, U4에 의존(코호트/회차 읽기 + 확정 참여·출석 인증·보고서 기반 판정). 코호트 종료 오케스트레이션을 소유.
- U6 admin-metrics: U3, U4, U5에 의존(참여·출석·수료/증서 집계).

## 통합 지점 (Integration Points)

- U1(추이적): 공통 인증(세션/토큰), 에러 DTO 규약, FileStorage API — U2를 통해 추이적으로 도달.
- U2→U3/U4: Cohort/Session 엔티티·조회 API.
- U3→U5/U6: Enrollment(확정 멤버) 상태.
- U4→U5/U6: Session 출석 인증 상태·증빙.
- U5→U6: Certificate/SettlementStatus 집계 소스.

## 병렬 개발 기회

- U2 완료 후 **U3와 U4는 상호 독립**이므로 병렬 개발 가능(여러 유효 위상 순서 존재).

## 머신 판독 엣지 블록 (REQUIRED)

```yaml
units:
  - name: U1-foundation
    kind: service
    depends_on: []
  - name: U2-cohort
    kind: service
    depends_on: [U1-foundation]
  - name: U3-enrollment
    kind: service
    depends_on: [U2-cohort]
  - name: U4-attendance
    kind: service
    depends_on: [U2-cohort]
  - name: U5-completion
    kind: service
    depends_on: [U2-cohort, U3-enrollment, U4-attendance]
  - name: U6-admin-metrics
    kind: service
    depends_on: [U3-enrollment, U4-attendance, U5-completion]
```

> 비순환 확인: U1→U2→(U3,U4)→U5→U6 방향, 역방향 엣지 없음. U5는 코호트 종료 오케스트레이션 소유자로서 U2(코호트/회차)를 읽으므로 U2에 의존한다(U2는 U5를 호출하지 않음 → 순환 없음).
> U1 의존은 **추이적**이다: U3~U6는 U2를 통해 U1(공통 인증·에러 DTO·FileStorage)에 도달하므로 직접 엣지를 중복 선언하지 않는다.

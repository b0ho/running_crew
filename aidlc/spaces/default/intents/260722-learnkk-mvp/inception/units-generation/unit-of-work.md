# Unit of Work — LearnKK (파일럿)

> Inception · units-generation 단계 산출물 · 리드 architect (서포트 delivery)
> 상위 입력: `application-design/`(components·component-methods·services·component-dependency·decisions), `requirements-analysis/requirements.md`, `user-stories/stories.md`
> 근거: units-generation-questions.md (Q1=수직 슬라이스, Q2=중간 입자, Q3=단일 배포)

## 유닛 정의

각 유닛은 FE(React)+BE(Spring) 수직 슬라이스이며, 개발 단위다(독립 배포 아님, Q3=A). kind는 모두 `service`(배포 실행체 백엔드 + 해당 UI 포함).

### U1 foundation (kind: service, 복잡도: L)
- 책임: FE/BE 저장소 스캐폴딩, 공통 인프라(전역 에러 DTO·@RestControllerAdvice, Spring Security+BCrypt, FileStorageService, springdoc-openapi 셋업), User 도메인, 회원가입/로그인, RBAC 역할 모델 + 관리자 Flyway 시드.
- 인도물: 실행 가능한 최소 앱 골격 + 인증.
- 스토리: US-0, US-1, US-2.

### U2 cohort (kind: service, 복잡도: M)
- 책임: 코호트 개설·수정, 회차(Session) N건 생성, 코호트/회차 조회, 공지(외부 링크). (코호트 "종료" 액션 오케스트레이션은 판정과 결합되므로 U5가 소유; U2는 상태 필드와 CRUD를 제공.)
- 스토리: US-3, US-4(수정), US-5.

### U3 enrollment (kind: service, 복잡도: L)
- 책임: 선착순 자동 참여, 정원 마감 대기, 동시성 제어(유니크 제약+비관적 락), 관리자 승인/거절, 알림 생성/조회.
- 스토리: US-6a, US-6b, US-7, US-8.

### U4 attendance (kind: service, 복잡도: M)
- 책임: 회차 증빙 파일 업로드·검증(이미지/pdf ≤10MB), 출석 인증, 증빙 이력, 진도/출석 조회.
- 스토리: US-9, US-10.

### U5 completion (kind: service, 복잡도: M)
- 책임: **코호트 종료 액션 오케스트레이션**(U2 코호트/회차 읽어 상태 종료됨 전이), 최종 보고서 제출, 수료 판정(출석≥80%)·수료증 발급, 정산 조건 판정, 종료 요약. U2/U3/U4 데이터를 읽음.
- 스토리: US-4(종료 액션), US-11, US-12, US-13.

### U6 admin-metrics (kind: service, 복잡도: M)
- 책임: 운영 지표 집계(완주/출석률/수료율/증서 수), 증빙 이력·보고서 이력 조회(관리자).
- 스토리: US-14, US-15.

## 배포 모델

- 단일 배포(Q3=A): FE 컨테이너 1 + BE 컨테이너 1(로컬 Docker). 유닛은 개발/전달 단위이며 독립 배포하지 않음.

## 복잡도 요약

- L: U1, U3 / M: U2, U4, U5, U6.

## 구현 노트 (위상·특성만; 순서 아님)

- U1은 공통 인프라를 제공하므로 위상상 다른 유닛의 선행 의존이다(구현 순서 처방이 아님).
- 동시성(U3)·판정(U5)은 핵심 도메인 테스트 집중 대상(team-practices 80%).
- **구현 순서/우선순위/크리티컬 패스는 이 단계에서 정하지 않는다** — Delivery Planning(2.8)의 경제적 결정 영역.

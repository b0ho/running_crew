# Shared Infrastructure — U3 enrollment (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U3-enrollment
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/logical-components.md`·`scalability-design.md`·`reliability-design.md`·`security-design.md`·`performance-design.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: 공유 인프라는 U1-foundation 확립. 본 문서는 U3가 공유 리소스와 맺는 관계·경계만 명시.

## 1. U3가 사용하는 공유 리소스(U1 확립)

| 공유 리소스 | U3 사용 |
|---|---|
| PostgreSQL(`learnkk-db`) | Enrollment·Notification 테이블(U3 소유) + 비관적 락/UNIQUE 동시성 제어 |
| `learnkk-api` 컨테이너 | 참여/알림 도메인 모듈 |
| `learnkk-web` 컨테이너 | 참여/알림 UI |
| GitHub Actions 파이프라인 | U3 동시성 테스트 포함(`cicd-pipeline.md` §2) |
| 파일 볼륨(`uploads`) | **U3 미사용** |

## 2. 스키마 소유·접근 경계

- **U3 소유 테이블**: Enrollment, Notification. Flyway 순서 U2 다음((U3∥U4)).
- **접근 경계(캡슐화)**: U3는 U2 Cohort.capacity·status를 읽기(`CohortService.get`)만 한다. U3가 제공하는 계약: `EnrollmentService.confirmedCount`/`confirmedEnrollments`(read, U2/U5/U6 사용), `NotificationService.notify`(write, U5/U8 호출) — component-methods.md 레지스트리 등록.
- **순환 없음**: U3는 U5를 호출하지 않음. U3·U4는 병렬 유닛, 상호 직접 의존 없음(둘 다 U2 읽음).

## 3. 다중 인스턴스 안전(U3 강점) & 확장

정원 정합이 DB(행 락+UNIQUE)에 위임되어 다중 인스턴스에서도 안전(`scalability-design.md` §2). 공유 리소스 확장 경로(세션 외부화·공유 스토리지·서비스 분리)는 U1 shared-infrastructure §4와 공통.

# Shared Infrastructure — U2 cohort (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U2-cohort
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/logical-components.md`·`scalability-design.md`·`reliability-design.md`·`security-design.md`·`performance-design.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: 공유 인프라는 U1-foundation이 확립. 본 문서는 U2가 그 공유 리소스와 맺는 관계·경계만 명시.

## 1. U2가 사용하는 공유 리소스(U1 확립)

`U1-foundation/infrastructure-design/shared-infrastructure.md` 인벤토리 중 U2 사용분:
| 공유 리소스 | U2 사용 |
|---|---|
| PostgreSQL(`learnkk-db`) | Cohort·Session·Announcement 테이블(U2 소유) 추가 |
| `learnkk-api` 컨테이너 | 코호트 도메인 모듈 공존 |
| `learnkk-web` 컨테이너 | 코호트 UI |
| GitHub Actions 파이프라인 | U2 테스트 포함(`cicd-pipeline.md` §2) |
| 파일 볼륨(`uploads`) | **U2 미사용** |

## 2. 스키마 소유·접근 경계

- **U2 소유 테이블**: Cohort, Session, Announcement. Flyway 마이그레이션 순서는 U1 다음(DAG U1→U2).
- **접근 경계(캡슐화)**: U2는 타 유닛 테이블에 직접 접근하지 않는다. U3의 확정 인원은 `EnrollmentService.confirmedCount`(read) 계약으로만 조회(`logical-components.md` §2). 반대로 U2는 `SessionService.markVerified`(U4 호출)·Cohort.status 세터·`transitionToEnded`(U5 호출)를 **서비스 계약으로 제공**(component-methods.md 레지스트리 등록).
- **순환 없음**: U2는 U5를 호출하지 않음(종료 오케스트레이션은 U5 소유).

## 3. 확장 시 분리 경로(U1 공통)

다중 인스턴스·서비스 분리 경로는 U1 shared-infrastructure §4와 공통. U2의 서비스 계약 캡슐화가 향후 분리 시 원격 API 승격을 용이하게 함.

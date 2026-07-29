# Infrastructure Services — U5 completion (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U5-completion
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 인프라 서비스 상속. U5 고유는 스키마·파일 볼륨(보고서·수료증)·이미지 생성 의존성.

## 1. 데이터베이스(공유 PostgreSQL — U5 스키마)

- **테이블**: FinalReport, Certificate, SettlementStatus. FK: 모두 cohortId→Cohort ON DELETE CASCADE(`components.md`).
- **제약·인덱스**(`performance-design.md` §3·`reliability-design.md` §3): UNIQUE `certificate(cohort_id, mentee_id)`(멱등 발급), UNIQUE `settlement_status(cohort_id)`(정산 1건 upsert), `final_report(cohort_id, submitted_at)`(이력 페이지네이션 — FinalReport 필드는 `submittedAt`).
- Flyway 마이그레이션(U1 이력 추가, DAG U5 — (U3∥U4) 다음).

## 2. 파일 스토리지(공유 uploads 볼륨)

- 보고서 첨부·수료증 이미지 저장은 U1 `FileStorageService.store/load/delete` 경유(U4와 공유 볼륨, 서버 UUID 파일명).
- **다건 보상**: 종료 트랜잭션 롤백 시 누적 imagePath 전부 `delete`(`reliability-design.md` §2). 보상 실패 로그 토큰 `ORPHAN_FILE_COMPENSATION_FAILED`.
- 볼륨 백업 대상 포함(`reliability-design.md` §5).

## 3. 이미지 생성 런타임 의존성

수료증 PNG 렌더링 라이브러리(code-generation 선택)와 폰트를 `learnkk-api` 컨테이너 이미지에 포함(배포 의존성, `deployment-architecture.md` §2). 메모리 효율 순차 생성(`performance-design.md` §2).

## 4. 캐시/큐/시크릿

- 캐시·큐 미도입(파일럿). 증서 발급 비동기 큐는 확장 트리거(`scalability-design.md` §2).
- 시크릿·디스커버리 U1 상속. U5 신규 시크릿 없음.

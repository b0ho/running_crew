# Infrastructure Services — U6 admin-metrics (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 인프라 서비스 상속. U6은 신규 스키마·저장소 없음(읽기 전용).

## 1. 데이터베이스(공유 PostgreSQL — 읽기 전용 접근)

- **신규 테이블 없음**: U6은 소스 유닛(U2~U5) 테이블을 조인·집계만 한다(`MetricsRepository` 읽기 전용 JPQL/native, INV-U6-1).
- **기준 인덱스(파일럿 필수 — 소스 유닛 소유)**: `cohort(status)`(U2), `enrollment(cohort_id, status)`(U3), `certificate(cohort_id)`(U5), `attendance_evidence(created_at)`(U4), `final_report(submitted_at)`(U5). U6 조회 성능(≤500ms/350ms) 전제이므로 U6가 요구로 명시하되 생성은 각 소유 유닛 마이그레이션(`performance-design.md` §2·`scalability-design.md` §2).
- **격리**: READ_COMMITTED, `readOnly=true`로 커밋된 상태만 조회(`reliability-design.md` §2).

## 2. 캐시/큐/검색/파일

- **캐시**: 미도입(실시간 집계, INV-U6-2). 확장 시 지표 캐시(TTL, `scalability-design.md` §3).
- **큐·검색**: 미도입.
- **파일**: U6은 파일을 저장하지 않음. 이력 다운로드 링크만 U1 `FileStorageService.load` 경유(읽기).

## 3. 시크릿·디스커버리

U1 상속. U6 신규 시크릿 없음.

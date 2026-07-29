# Shared Infrastructure — U6 admin-metrics (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U6-admin-metrics
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/logical-components.md`·`scalability-design.md`·`reliability-design.md`·`security-design.md`·`performance-design.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: 공유 인프라는 U1-foundation 확립. U6은 말단 읽기 전용 소비자로 공유 리소스를 읽기만.

## 1. U6이 사용하는 공유 리소스(U1 확립, 읽기 전용)

| 공유 리소스 | U6 사용 |
|---|---|
| PostgreSQL(`learnkk-db`) | 소스 유닛(U2~U5) 테이블 **조인·집계 읽기만**(신규 테이블 없음) |
| `learnkk-api` / `learnkk-web` | 리포팅 도메인 모듈 / 관리자 UI |
| 파일 볼륨(`uploads`) | 이력 다운로드 링크만(U1 load 경유 읽기, 저장 없음) |
| GitHub Actions 파이프라인 | U6 지표/이력 테스트 포함 |

## 2. 접근 경계 — 읽기 전용 말단 소비자

- **신규 소유 테이블 없음**: U6은 소스 스키마를 읽기만 한다(INV-U6-1). 기준 인덱스는 소스 유닛(U2~U5) 소유(`infrastructure-services.md` §1).
- **읽기 경로**: 파일럿 기본은 `MetricsRepository`가 공유 스키마를 직접 조인·집계하는 리포팅 읽기 모델. 소스 유닛 read API 조합도 가능(결과 동일). 어느 경로든 **쓰기 없음**.
- **말단·순환 없음**: 어떤 유닛도 U6을 호출하지 않으며 U6은 아무것도 쓰지 않는다. DAG 종착점(U1→U2→(U3∥U4)→U5→U6). 순환 불가능.

## 3. 확장

데이터 대규모 시 지표 캐시·머티리얼라이즈드 뷰·사전 집계 테이블 도입(`scalability-design.md` §3). U6은 무상태 읽기 전용이라 다중 인스턴스 확장 시 추가 제약 없음(세션은 U1 트리거). 클라우드·스토리지 확장은 U1 shared-infrastructure §4 공통.

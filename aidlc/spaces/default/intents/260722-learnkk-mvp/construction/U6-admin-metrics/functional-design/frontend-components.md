# Frontend Components — U6 admin-metrics (LearnKK 파일럿)

> Construction · functional-design 단계 산출물(조건부 UI) · 유닛 U6-admin-metrics
> 리드 architect (서포트 developer)
> 상위 입력: `application-design/components.md`(S6 관리자: 지표·증빙이력·보고서이력 탭), `requirements-analysis/requirements.md`(FR-10/11, NFR-3), `unit-of-work-story-map.md`(US-14/15), `component-methods.md`(MetricsService·HistoryService)
> 규약: U1 공통 셸 + U3가 시작한 AdminPage 탭 구조 재사용. team.md React 규약, Tailwind 경량 커스텀. 관리자 전용.

## 1. 컴포넌트 계층 (U6 추가분 — AdminPage 탭)

```
AdminPage (관리자 전용, RequireAdmin 가드)
 ├─ 대기승인 탭 (U3)
 ├─ 지표 탭 (U6) — MetricsOverview
 │   └─ MetricCard x4 (완주 코스 수 / 출석률 / 수료율 / 증서 수)
 ├─ 증빙 이력 탭 (U6) — EvidenceHistoryTable (페이지네이션)
 └─ 보고서 이력 탭 (U6) — ReportHistoryTable (페이지네이션)
```
<!-- Text fallback: 관리자 페이지에 U3의 대기승인 탭과 함께 U6의 지표 탭(지표 카드 4개), 증빙 이력 탭, 보고서 이력 탭(각각 페이지네이션 테이블)을 둔다. 관리자 전용 가드로 보호된다. -->

## 2. 컴포넌트별 설계

### 2.1 MetricsOverview / MetricCard (US-14 / FR-11)
- 데이터: `GET /api/admin/metrics`(overview). 4개 지표 카드: 완주 코스 수, 출석률(%), 수료율(%), 발급 증서 수.
- 집계 범위 라벨 표시("종료된 코호트 N건 기준")로 사용자가 지표 의미를 이해.
- 분모 0 상황은 "0%" 또는 "데이터 없음"으로 안전 표시.

### 2.2 EvidenceHistoryTable (US-15)
- 데이터: `GET /api/admin/history/evidence?page=`(evidenceHistory). 컬럼: 코호트·회차·업로더·형식·크기·업로드일·다운로드.
- 다운로드 링크는 U1 load 경유(관리자 권한). 최신순, 20건 페이지네이션.

### 2.3 ReportHistoryTable (US-15)
- 데이터: `GET /api/admin/history/reports?page=`(reportHistory). 컬럼: 코호트·작성자·첨부유무·제출일·(첨부 다운로드).
- 증빙 이력과 **별도 탭**(FR-10 분리 조회).

## 3. API 통합 지점 (U6)

| 액션 | 호출 | 메서드(BE) |
|---|---|---|
| 운영 지표 | `GET /api/admin/metrics` | MetricsService.overview |
| 증빙 이력 | `GET /api/admin/history/evidence` | HistoryService.evidenceHistory |
| 보고서 이력 | `GET /api/admin/history/reports` | HistoryService.reportHistory |

- 모든 호출 ApiClient 경유(세션·에러 정규화). 관리자 아닌 접근은 서버 403(R-U6-01); UI는 관리자에게만 탭 노출.

## 4. 접근성·상태 처리

- MetricCard는 수치+라벨 병기, 스크린리더용 설명 텍스트.
- 이력 테이블은 컬럼 헤더 scope 지정, 페이지네이션 키보드 접근.
- 로딩/빈 상태(데이터 없음) 명시 표시.

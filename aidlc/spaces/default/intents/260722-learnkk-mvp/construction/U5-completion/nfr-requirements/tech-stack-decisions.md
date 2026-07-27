# Tech Stack Decisions — U5 completion (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U5-completion
> 리드 architect · 관점 devsecops·quality
> 상위 입력: `U5-completion/functional-design/business-logic-model.md`(종료 오케스트레이션), `business-rules.md`(R-U5-*), `requirements-analysis/requirements.md`(NFR-1/7)

## 1. 상속 스택
U1-foundation 표준 스택 상속. 파일(보고서 첨부·수료증 이미지)은 U1 FileStorageService(store/load/delete) 사용.

## 2. U5 고유 기술 선택
| 항목 | 선택 | 근거 |
|---|---|---|
| 종료 오케스트레이션 | 단일 `@Transactional`(판정+발급+전이+알림) | R-U5-03 원자성 |
| 수료증 이미지 생성 | 서버측 템플릿 렌더링 → PNG(라이브러리는 code-gen 선택, 예: Thumbnailator/Java2D/HTML→이미지) | R-U5-08 수료증 |
| 수료 판정 산술 | 정수 비교 `verified*100 >= total*80` | R-U5-06 부동소수 회피 |
| 정산 upsert | UNIQUE(cohortId) 기반 findOrCreate | R-U5-12 |
| 파일+DB 보상 | store→[TX]→롤백 시 누적 imagePath delete(루프) | R-U5-08a |

## 3. 보류/확장
- U1 보류 상속. 수료증 이미지 생성 라이브러리 선택은 code-generation에서 확정(파일럿은 단순 이미지 1장, `cid:market-research:c2`).

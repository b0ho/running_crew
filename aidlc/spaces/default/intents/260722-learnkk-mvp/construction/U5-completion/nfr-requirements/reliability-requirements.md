# Reliability Requirements — U5 completion (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U5-completion
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `U5-completion/functional-design/business-logic-model.md`(종료 §2 원자성), `business-rules.md`(R-U5-03/06/08a, INV-U5-*), `requirements-analysis/requirements.md`(FR-8/9, NFR-2/4)
> 전제: 로컬 단일 서버(U1 상속). U5는 판정·오케스트레이션 정합성이 핵심.

## 1. 정합성 (핵심 — 종료 오케스트레이션)
- **원자성(INV-U5-3)**: 종료 트랜잭션(수료 판정+수료증 발급+정산 판정+상태전이+알림)은 단일 `@Transactional`. 부분 실패 시 전체 롤백 → "수료증만 발급되고 상태 미전이" 같은 불일치 방지.
- **수료 판정 정확성(INV-U5-5)**: 정수 비교(`verified*100 >= total*80`)로 부동소수 경계 오차 제거. **79/80% 경계 테스트 필수**(US-12 AC).
- **수료증 중복 방지(INV-U5-1)**: UNIQUE(cohortId, menteeId) — 재종료 시 재발급 없음.
- **정산 1건(INV-U5-2)**: UNIQUE(cohortId) upsert.
- **다건 파일 보상(R-U5-08a)**: 멘티별 수료증 이미지 store로 누적한 imagePath를 롤백 시 전부 delete(루프 레벨). 5명 중 5번째 실패해도 앞 4개 정리.

## 2. 결함 허용
- totalSessions==0 정합 오류는 500(R-U5-10). 소유/상태 위반은 403/409(트랜잭션 진입 전 검증).
- 알림(U3 notify) 실패 처리: 종료 판정·발급이 완료되었으면 알림 실패가 전체를 롤백할지는 code-gen 결정(기본: 동일 트랜잭션이면 함께 롤백; 알림을 best-effort 후속으로 분리하려면 확정 커밋 후 별도 발송). 파일럿 기본은 단순성 우선(동일 트랜잭션).

## 3. 가용성 & 내구성
- U1 best-effort 가용성 상속. 보고서 첨부·수료증 이미지는 로컬 볼륨 저장, **U1 일 1회 스냅샷 백업에 파일 볼륨 포함**(U4와 동일 — 증서·보고서 유실 방지).

## 4. 검증
- 종료 트랜잭션 원자성(롤백 시 증서 0·상태 미전이·정산 미기록), 수료 80% 경계(79/80), 재종료 멱등(증서 재발급 없음), 정산 upsert를 통합 테스트(Testcontainers).

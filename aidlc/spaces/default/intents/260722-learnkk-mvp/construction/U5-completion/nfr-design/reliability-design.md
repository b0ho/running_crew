# Reliability Design — U5 completion (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U5-completion(판정·오케스트레이션 정합성 핵심)
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `nfr-requirements/reliability-requirements.md`(원자성·판정 정확성·다건 보상), `functional-design/business-logic-model.md`(W-U5-1 종료 오케스트레이션 §2), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 서버(U1 가용성 상속).

## 1. 종료 오케스트레이션 정합성(핵심 — INV-U5-3)

`reliability-requirements.md` §1 + business-logic-model §2를 확정한다.

- **원자성(INV-U5-3)**: `endCohort` 트랜잭션(회차 집계 → 수료 판정 → 수료증 N장 발급 → 정산 판정 → status 종료됨 전이 → 알림)은 **단일 `@Transactional`**. 부분 실패 시 전체 롤백 → "수료증만 발급되고 상태 미전이", "상태는 종료됐는데 정산 미기록" 같은 불일치 불가.
- **처리 순서**: (1) 사전 검증(존재·소유·진행중) — 트랜잭션 진입 전 4xx. (2) 트랜잭션 내: 집계·판정·발급·전이·알림.
- **수료증 이미지 store는 트랜잭션 내 발생하되 비트랜잭션 I/O** → 롤백 시 §2 보상 필요.

## 2. 다건 파일 보상(loop-level — R-U5-08a)

`reliability-requirements.md` §1의 다건 보상을 확정한다.

- **누적-보상 패턴**: 확정 멘티별로 수료증 이미지를 `FileStorageService.store`로 저장하며 발급된 `imagePath`를 **누적 리스트**에 추가한다. 트랜잭션이 롤백되면 **누적한 모든 imagePath에 대해 `FileStorageService.delete`(멱등)를 호출**해 고아 이미지를 정리한다(U4 단건 보상의 다건 확장).
- **부분 실패 견고성**: 5명 중 5번째 발급에서 실패해도 앞선 4개 이미지가 정리된다(누적 리스트 순회 delete).
- **보상 실패 로그(확정)**: 개별 delete 실패 시 U4와 동일 토큰으로 로깅 — `log.error("ORPHAN_FILE_COMPENSATION_FAILED path={} error={}", imagePath, e.getMessage())`. 운영은 이 토큰을 grep해 수동 정리. 정합성(INV-U5-3)은 유지(상태 미전이·증서 미커밋).
- **구현 위치**: U4와 동일하게 명시적 try/catch로 트랜잭션 실패를 잡아 누적 리스트 보상(파일럿 단순성).

## 3. 판정 정확성 & 멱등성

- **수료 판정 정확성(INV-U5-5)**: 출석률 판정은 **정수 비교** `verifiedSessions * 100 >= totalSessions * 80` 으로 수행(부동소수 경계 오차 제거). **79%/80% 경계 테스트 필수**(US-12 AC, R-U5-06).
- **totalSessions==0 방어**: 정합 오류로 500 반환(R-U5-10) — 0으로 나눔·무의미 판정 방지.
- **수료증 중복 방지(INV-U5-1)**: UNIQUE(cohortId, menteeId). 재종료 시 이미 발급된 수료증은 재발급하지 않음(있으면 skip) → 멱등.
- **정산 1건(INV-U5-2)**: UNIQUE(cohortId) 기반 upsert(findOrCreate) — 코호트당 정산 정확히 1건.

## 4. 결함 허용 & 알림 경계

`reliability-requirements.md` §2:
- 소유/상태 위반은 트랜잭션 진입 전 403/409. totalSessions==0은 500.
- **알림 트랜잭션 경계(확정)**: 파일럿에서는 **알림(U3 notify)을 종료 트랜잭션과 동일 트랜잭션에서 생성**한다(함께 커밋/롤백). 근거: 단일 인스턴스·단일 DB에서 알림은 DB 레코드이며, "종료 판정은 됐는데 결과 알림 유실" 창을 없애는 것이 파일럿에서 가장 단순·안전. best-effort 후속 발송(확정 커밋 후 비동기)은 메시지 브로커 도입 확장 시 재설계(U3 알림 경계 결정과 일관).

## 5. 가용성 & 내구성

`reliability-requirements.md` §3:
- U1 best-effort 가용성 상속.
- **보고서 첨부·수료증 이미지 내구성**: 로컬 볼륨 저장. U1 일 1회 스냅샷 백업 대상에 **파일 볼륨 포함**(U4와 동일) — 증서·보고서 유실 방지.

## 6. 검증(quality)

`reliability-requirements.md` §4:
- 종료 트랜잭션 **원자성**: 강제 롤백 시 증서 0·상태 미전이·정산 미기록 단언(Testcontainers).
- 수료 **80% 경계**(79% 미수료 / 80% 수료) 테스트.
- **재종료 멱등**(증서 재발급 없음).
- 정산 **upsert**(재판정 시 1건 유지).
- **다건 보상**: N개 이미지 store 후 롤백 → N개 전부 delete 확인.

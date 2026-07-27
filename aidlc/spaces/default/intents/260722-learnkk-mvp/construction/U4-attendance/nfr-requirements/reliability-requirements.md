# Reliability Requirements — U4 attendance (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 quality·aws-platform
> 상위 입력: `U4-attendance/functional-design/business-logic-model.md`(업로드 §2 보상), `business-rules.md`(R-U4-13, INV-U4-1~4), `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 서버(U1 상속).

## 1. 정합성 (핵심 — 파일+DB)
- **INV-U4-1 보장**: 회차 인증(markVerified)과 증빙 이력 저장은 **동일 DB 트랜잭션** → "인증됐는데 증빙 없음" 불가.
- **보상(R-U4-13)**: 파일 store(비트랜잭션)는 트랜잭션 밖 선행. 트랜잭션 롤백 시 U1 `FileStorageService.delete`로 고아 파일 보상, delete 실패 시 경로 ERROR 로그(수동 정리). 유일 잔여 리스크는 고아 파일(파일럿 수용).

## 2. 결함 허용
- 파일 검증 실패 → 400(트랜잭션 진입 전). markVerified/이력 저장 실패 → 롤백+보상 delete+500.
- U2 markVerified 호출 실패 시 전체 롤백(회차 미인증·이력 미적재로 일관).

## 3. 가용성 & 내구성
- U1 best-effort 가용성 상속. **증빙 파일 내구성**: 로컬 볼륨 저장, U1 일 1회 스냅샷 백업에 **파일 볼륨 포함**(DB만이 아니라 업로드 볼륨도 백업 대상 — 증빙 유실 방지). 파일 볼륨 백업은 인프라 단계에서 구현.

## 4. 검증
- 업로드→인증→이력 원자성(롤백 시 회차 미인증 & 이력 0), 형식/크기 거부, 고아 파일 보상 delete를 통합 테스트(Testcontainers + 로컬 파일 저장 목/임시 디렉토리).

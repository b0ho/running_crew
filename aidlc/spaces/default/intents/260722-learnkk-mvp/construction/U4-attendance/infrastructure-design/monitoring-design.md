# Monitoring Design — U4 attendance (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U4-attendance
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/reliability-design.md`·`performance-design.md`·`security-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 모니터링 골격 상속. 상세 관측은 Operation observability-setup.

## 1. 헬스·지표(U1 상속 + U4 관측)

U1 `/actuator/health`·Micrometer 공유. U4 고유 관측: 업로드/다운로드 응답시간(`performance-design.md` 목표 대비), uploads 볼륨 사용량(디스크), 진도 조회 시간.

## 2. 로그(파일 보상 핵심)

- **고아 파일 보상 실패**: 고정 토큰 `ORPHAN_FILE_COMPENSATION_FAILED path={} error={}`로 로깅(`reliability-design.md` §2) — 운영 grep 수동 정리.
- 파일 제약 위반(400)·권한 위반(403)·업로드/다운로드 실패는 애플리케이션 로그. 파일 내용·PII 미로깅.

## 3. 백업(파일 볼륨 포함 — 중요)

`reliability-design.md` §4: DB뿐 아니라 **uploads 볼륨도 일 1회 스냅샷** 백업(증빙 유실 방지). 백업 성공/실패 로그 확인, 볼륨 스냅샷 스크립트/rsync는 infrastructure/code-generation 구현.

## 4. 스토리지 용량 모니터링

uploads 볼륨 사용량 임계 접근은 확장 트리거(오브젝트 스토리지 이관, `scalability-design.md` §4)의 신호 — Operation observability-setup에서 임계 알림 승격 권장.

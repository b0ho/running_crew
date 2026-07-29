# Infrastructure Services — U4 attendance (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U4-attendance
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 인프라 서비스 상속. U4 고유는 스키마·파일 볼륨 사용.

## 1. 데이터베이스(공유 PostgreSQL — U4 스키마)

- **테이블**: AttendanceEvidence. FK: Evidence.sessionId→Session ON DELETE CASCADE(`components.md`).
- **인덱스**(`performance-design.md` §3): `attendance_evidence(session_id)`(증빙 존재 확인), `attendance_evidence(session_id, created_at)`(U6 이력 페이지네이션 대비).
- Flyway 마이그레이션(U1 이력 추가, DAG (U3∥U4)).

## 2. 파일 스토리지(공유 uploads 볼륨 — U4 주 사용처)

`services.md`(FileStorageService)·`security-design.md` §6:
- U1이 확립한 `uploads` 명명 볼륨(웹루트 밖) 사용. 저장은 U1 `FileStorageService.store`(스트리밍), 서버 UUID 파일명, MIME(매직바이트)·크기 ≤10MB 검증.
- **보상 delete**: 트랜잭션 롤백 시 U1 `FileStorageService.delete`로 고아 파일 정리(`reliability-design.md` §2).
- **볼륨 백업**: uploads는 일 1회 스냅샷 대상(§reliability). **확장 교체점**: 다중 인스턴스 시 공유/오브젝트 스토리지로 FileStorageService 백엔드 교체(`scalability-design.md` §2).

## 3. 캐시/큐/검색

미도입(U1 방침). 증빙 이력은 인덱스+페이지네이션.

## 4. 시크릿·디스커버리

U1 상속. U4 신규 시크릿 없음.

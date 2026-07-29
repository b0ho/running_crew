# Scalability Design — U4 attendance (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/scalability-requirements.md`(스토리지 성장·다중 인스턴스·트리거), `functional-design/business-logic-model.md`(FileStorage 사용), `nfr-requirements/tech-stack-decisions.md`
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 1. 확장 아키텍처(파일럿)

`scalability-requirements.md` §1~2:
- 파일럿은 단일 인스턴스·수직 확장만(U1 상속).
- **증빙 파일이 유일한 대용량 증가 요인**: 회차당 1~수 건, 파일당 ≤10MB → 파일럿 규모 수 GB 이내.
- AttendanceEvidence 메타는 소량(RDB), 파일 본체는 로컬 볼륨.

## 2. 다중 인스턴스 제약 & 스토리지 교체점(핵심)

`scalability-requirements.md` §2의 요구를 확정한다.

- **파일 저장 위치가 다중 인스턴스의 핵심 제약**: 로컬 볼륨은 단일 인스턴스 전제다. 인스턴스가 2개 이상이 되면 한 인스턴스가 저장한 파일을 다른 인스턴스가 서빙하지 못한다.
- **교체점(격리)**: 이 제약은 **U1 `FileStorageService` 인터페이스 뒤로 격리**되어 있다. 다중 인스턴스 확장 시 구현체를 **공유 스토리지(NFS) 또는 오브젝트 스토리지(S3 등)** 로 교체하면 되고, U4 업로드/다운로드 로직(store/load/delete 계약 사용)은 변경 불필요. 파일럿은 로컬 볼륨 구현체.
- 메타데이터 조회는 `attendance_evidence(session_id)` 인덱스로 확장 견딤.

## 3. 데이터 확장

- 증빙 이력 대량 축적 시 이력 조회(U6 관리자 이력 뷰)는 페이지네이션으로 감당(`performance-design.md` §3 인덱스).
- 파일 볼륨 용량은 파일럿에서 수 GB 이내로 로컬 디스크 충분.

## 4. 확장 트리거

`scalability-requirements.md` §3:
- **스토리지 용량 임계 접근** 또는 **다중 인스턴스 필요** 시 → `FileStorageService`를 오브젝트 스토리지 백엔드로 교체(인터페이스 불변). 필요 시 CDN 결합(`performance-design.md` §4).
- 증빙 이력 대량 축적 시 이력 조회 페이지네이션 적용(U6 뷰). 파일럿 규모에서는 불필요.

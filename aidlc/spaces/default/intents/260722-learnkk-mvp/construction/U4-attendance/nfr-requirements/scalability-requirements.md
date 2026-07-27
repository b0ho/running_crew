# Scalability Requirements — U4 attendance (LearnKK 파일럿)

> Construction · nfr-requirements 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 관점 aws-platform
> 상위 입력: `U4-attendance/functional-design/business-logic-model.md`, `business-rules.md`, `requirements-analysis/requirements.md`(NFR-2/4)
> 전제: 로컬 단일 인스턴스, <100명(U1 상속).

## 1. 데이터·스토리지 성장
- 증빙 파일이 유일한 대용량 증가 요인. 회차당 1~수 건, 파일당 ≤10MB. 파일럿 규모에서 수 GB 이내 예상.
- AttendanceEvidence 메타는 소량(RDB). 파일 본체는 로컬 볼륨.

## 2. 확장 전략 & 다중 인스턴스 고려
- **파일 저장 위치가 다중 인스턴스의 제약**: 로컬 볼륨은 단일 인스턴스 전제. **다중 인스턴스로 확장 시 공유 스토리지(NFS/오브젝트 스토리지: S3 등)로 이관 필요** — 이는 U1 FileStorageService 구현 교체로 흡수(인터페이스 유지, 저장 백엔드 교체). 파일럿은 로컬 볼륨.
- 메타데이터 조회는 sessionId/cohortId 인덱스로 확장 견딤.

## 3. 확장 트리거
- 스토리지 용량 임계 접근 또는 다중 인스턴스 필요 시: FileStorageService를 오브젝트 스토리지 백엔드로 교체(인터페이스 불변). 파일럿 규모에서는 로컬 볼륨으로 충분.
- 증빙 이력 대량 축적 시 이력 조회 페이지네이션(U6 이력 뷰)으로 감당.

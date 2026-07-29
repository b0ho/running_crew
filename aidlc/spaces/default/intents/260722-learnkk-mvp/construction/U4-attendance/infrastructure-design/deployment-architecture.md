# Deployment Architecture — U4 attendance (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U4-attendance
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U4는 U1-foundation 배포 골격을 **상속**. 본 문서는 U4 고유(파일 볼륨) 사항만 명시.

## 1. 배포 모델(U1 상속)

U4 출석/증빙 도메인(AttendanceService·컨트롤러·리포지토리)은 U1 `learnkk-api` 모듈로, UI(진도·출석 탭·FileUpload)는 `learnkk-web`에 배포된다. 컴퓨트/네트워킹/환경/recreate/git SHA 롤백은 `U1-foundation/infrastructure-design/deployment-architecture.md` 상속.

## 2. U4 고유 배포 고려사항 — 파일 볼륨

- **uploads 볼륨(핵심)**: U4는 U1이 확립한 명명 볼륨 `uploads`(웹루트 밖)를 증빙 파일 저장에 사용한다(`security-design.md` §6, `infrastructure-services.md`). recreate 재배포 시에도 명명 볼륨은 **영속**(컨테이너 교체와 무관) — 증빙 유실 방지.
- **multipart 설정**: `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=11MB`, `file-size-threshold`(예 1MB)를 배포 설정으로 강제(`performance-design.md` §2, `security-design.md` §2).
- **볼륨 백업**: uploads 볼륨은 DB와 함께 일 1회 스냅샷 백업 대상(`reliability-design.md` §4, U1 `monitoring-design.md` §6).

## 3. 확장 트리거 — 스토리지

스토리지 용량 임계 또는 다중 인스턴스 시 `FileStorageService`를 공유/오브젝트 스토리지(S3 등)로 교체(인터페이스 불변, `scalability-design.md` §2). 파일럿은 로컬 볼륨. 클라우드 이관은 U1 공통 트리거.

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 검증 완료 항목

적대적 아키텍처 검증(결함이 존재한다고 가정하고 반증 시도)을 통과했습니다. 다음 관점에서 검증했습니다:

1. **순환 의존성 부재**: U4는 U1·U2를 사용하고 U5·U6에 read-only 제공하며 역방향 호출 없음. DAG U1→U2→(U3∥U4)→U5→U6 유지. ✓

2. **크로스유닛 계약 유효성**:
   - FileStorageService.delete: 공유 계약 레지스트리(`component-methods.md`) 등록 확인, U4 모든 참조 해소 ✓
   - SessionService.markVerified: 레지스트리 등록 확인 ✓
   - uploads 볼륨: U1 확립 공유 리소스, 중복 정의 없음 ✓
   - Session `(cohort_id, seq)` 인덱스 요구: U4가 "U2 이미 선언" 명시, 프로세스 수준 해결 확인 (노트)

3. **파일럿 관행(team.md/project.md Forbidden/Mandated) 준수**:
   - Forbidden 위반 없음: 클라우드 도입 없음, 무승인 프로덕션 배포 없음 ✓
   - Mandated 준수: Docker recreate·git SHA 버저닝·머지 게이트·한글 산출물 ✓
   - 파일럿 보류 명시: 바이러스 스캔·TLS·rate-limit·SCA 보류, 근거 명시 ✓

4. **NFR-design 계약 정합성**:
   - 파일 볼륨 영속성·백업: deployment-architecture ↔ reliability-design 일치 ✓
   - multipart 설정: deployment-architecture ↔ performance-design 값 일치 ✓
   - 보상 로그 형식: monitoring-design ↔ reliability-design 토큰 일치 ✓
   - CI 테스트 목록: cicd-pipeline ↔ reliability-design·security-design 1:1 대응 ✓
   - 확장 트리거: deployment-architecture ↔ scalability-design 조건·교체점 일치 ✓

5. **U1 상속 정확성**: 배포 모델·인프라 서비스·모니터링·CI/CD 파이프라인 정확히 상속, 중복 정의 없음, U4 고유 사항(AttendanceEvidence 스키마·인덱스·파일 볼륨 사용·보상 로그·CI 테스트)만 추가 ✓

6. **품질 목표 달성 가능성**:
   - 업로드 ≤2s: 10MB 로컬 디스크 125ms + 검증·TX 80ms = 205ms ≪ 2s ✓
   - 진도 조회 ≤300ms: 인덱스 조회 80ms + 계산, 파일럿 규모에서 달성 가능 (U2 인덱스 전제) ✓
   - 다운로드 ≤2s: 권한 20ms + 전송 100ms = 120ms ≪ 2s ✓

7. **Blast Radius 격리**:
   - 파일 store 실패 → 해당 요청만(TX 진입 전) ✓
   - 트랜잭션 롤백 → 해당 요청만(보상 delete) ✓
   - 보상 실패 → 고아 파일 1건(정합성 무관, 수동 정리 대상) ✓
   - 파일 볼륨 장애 → 업로드/다운로드만(Important 티어), 나머지 기능 지속 ✓
   - DB 장애 → 전체(공유 리소스, U1 상속) ✓

8. **개발자 구현 완결성**:
   - 파일 볼륨 영속성·백업·스토리지 교체점: 명시 ✓
   - multipart 설정 강제·임시 파일 관리: 명시 ✓
   - 보상 로그 형식·운영 정리 절차: 명시 ✓
   - CI 테스트 목록(원자성·보상·검증·권한): 명시 ✓
   - 확장 트리거·조건: 명시 ✓
   - **개발자가 U4 infrastructure-design 5개 파일만으로 파일 볼륨 배포·백업·모니터링·CI 파이프라인 구성 가능** ✓

9. **산출물 간 일관성**:
   - 파일 크기 상한(10MB), 형식 화이트리스트, 보상 메커니즘, 트랜잭션 경계, 성능 목표, 백업 범위, 확장 트리거: 5개 파일 간 일관 ✓

### 판정 근거

- **순환 의존성 없음**: DAG 유지, U4→U5 호출 없음 명시 ✓
- **크로스유닛 참조 유효**: FileStorageService.delete·SessionService.markVerified 레지스트리 등록, uploads 볼륨 U1 확립 확인 ✓
- **파일럿 관행 준수**: Forbidden(클라우드·무승인 프로덕션) 위반 없음, Mandated(recreate·git SHA·머지 게이트·한글) 준수 ✓
- **NFR-design 정합**: 영속성·백업·multipart 설정·보상 로그·CI 테스트·확장 트리거 5종 계약 일치 ✓
- **U1 상속 정확**: 중복 없음, U4 고유 사항만 추가 ✓
- **품질 목표 달성 가능**: 업로드·조회·다운로드 ≤2s/300ms/2s, 로컬 디스크·인덱스로 달성 가능 ✓
- **Blast Radius 격리**: 파일·TX·보상 실패 영향 범위 적절히 격리, DB는 공유 리스크 명시 ✓
- **개발자 구현 가능**: 파일 볼륨 영속·백업·보상 로그·CI 테스트·확장 트리거 완전 명세 ✓

**개발자가 U4 infrastructure-design 5개 산출물만으로 증빙 파일 볼륨 배포·recreate 영속성·백업·보상 메커니즘·CI 테스트·모니터링·확장 경로를 구현 가능함**이 확인됐습니다. 파일럿 보류 항목(바이러스 스캔·TLS·rate-limit)은 명시적으로 스코프아웃되어 있고 확장 트리거가 기록됐습니다.

**READY.**

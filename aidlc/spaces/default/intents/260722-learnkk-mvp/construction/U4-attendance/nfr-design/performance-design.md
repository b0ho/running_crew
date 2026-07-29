# Performance Design — U4 attendance (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U4-attendance
> 리드 architect · 서포트 aws-platform
> 상위 입력: `nfr-requirements/performance-requirements.md`(업로드·조회·다운로드 목표), `nfr-requirements/tech-stack-decisions.md`(MultipartFile·10MB), `functional-design/business-logic-model.md`(W-U4-1 업로드·W-U4-2 진도)
> 전제: <100명·로컬 단일 인스턴스(U1 성능 전제 상속).

## 1. 응답시간 예산(latency budget)

`performance-requirements.md` §1 목표를 계층별로 분해한다. 업로드/다운로드는 파일 크기·디스크 I/O 의존.

| 연산 | 총 목표 | 설계 예산 |
|---|---|---|
| 증빙 업로드(≤10MB) | ≤ 2s | 사전 검증 ~30ms + 스트리밍 저장(디스크 I/O, 10MB 지배) + [TX] 이력 insert + markVerified ~50ms |
| 진도·출석 조회 | ≤ 300ms | 회차 status 집계(인덱스) ~80ms + 진도율 계산 + 직렬화 |
| 증빙 다운로드(≤10MB) | ≤ 2s | 권한 확인 ~20ms + 스트리밍 전송(네트워크·디스크 지배) |

## 2. 스트리밍 & 리소스 관리(핵심)

`performance-requirements.md` §2를 구체 설계로 확정한다.

- **업로드 스트리밍 저장**: `MultipartFile`을 **전체 메모리 로딩 없이** `InputStream`으로 U1 `FileStorageService.store`에 전달하여 디스크로 스트리밍한다(대용량 파일에서도 힙 사용 억제). Spring multipart의 임시 파일 임계값(`spring.servlet.multipart.file-size-threshold`)을 낮게(예: 1MB) 두어 큰 파일은 디스크 임시파일 경유.
- **임시 파일 정리(확정)**: Spring은 요청 처리 종료 시 multipart 임시 파일을 자동 정리하므로 **명시적 정리 코드는 불필요**하다. 통합 테스트에서 상한 근처 파일을 다수 업로드한 뒤 임시 디렉토리에 누수가 없는지 확인한다(검증 책임은 테스트).
- **다운로드 스트리밍**: `FileStorageService.load`가 반환한 `Resource`를 `StreamingResponseBody`/`InputStreamResource`로 청크 전송(전체 메모리 적재 금지).
- **크기 상한(DoS 1차 완화)**: `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=11MB`로 서버 강제(`security-design.md` §2와 정합). 상한 초과는 저장 전 413/400으로 거부하여 리소스 고갈 방지.

## 3. 데이터 접근 & 인덱스

- 진도·출석 조회(`sessionsOf`)는 `session(cohort_id, seq)`(U2 소유 인덱스)로 회차 status 집계, `attendance_evidence(session_id)` 인덱스로 증빙 존재 확인. 진도율 = 인증 회차 수 / 전체 회차 수.
- 증빙 이력 조회는 `attendance_evidence(session_id, created_at)` 인덱스(U6 이력 뷰 페이지네이션 대비).

## 4. 파일럿 스코프아웃 & 확장 트리거

- **미도입**: CDN 파일 배포, 이미지 리사이즈/썸네일, 비동기 업로드 처리. 근거: 파일럿 규모(회차당 수 건, 총 수 GB 이내)에서 로컬 스트리밍으로 충분.
- **확장 트리거**: 스토리지 용량·다운로드 트래픽 증가 시 오브젝트 스토리지(S3 등) + CDN으로 이관(`scalability-design.md` §2, U1 FileStorageService 백엔드 교체). 성능 엄밀 검증은 Operation performance-validation으로 이관(U1 방침 상속).


## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

### 1차 Blocking Findings 해소 확인

**Blocking 1 (FileStorageService.delete 계약 미등록)**: ✅ **해소 확인됨**
- 공유 계약 레지스트리 `component-methods.md`의 FileStorageService 항목에 `delete(path): void` 시그니처가 추가됨을 확인
- 계약 내용: 멱등성(대상 없으면 no-op), 경로 이탈 방지, 보상 목적 명시, U1 functional-design §7 근거 포함
- U4 산출물 내 모든 참조(reliability-design §2, logical-components §1, business-logic-model §2·§6)가 해당 계약을 일관되게 사용함
- 크로스유닛 계약 등록 완료로 판단

**Blocking 2 (매직바이트 검증 책임 소재)**: ✅ **해소 확인됨**
- `security-design.md` §2에 "검증 책임 경계(확정, Finding 해소)" 절 신설
- 매직바이트 검증은 `AttendanceService.uploadEvidence` step 1(FileStorageService.store 호출 전)에서 U4가 수행함을 명시
- U1 FileStorageService.store는 기본 검증(확장자+선언 MIME+크기)만 담당함을 확정
- 검증 순서 및 근거(응집도, U1 계약 불변) 기술
- business-logic-model.md §2 step 1과 정합(사전 검증 → store 호출 순서)

### Advisory Findings 해소 확인

- **Advisory 3** (Session 인덱스 요구): ✅ `logical-components.md` §2에 `(cohort_id, seq)` 인덱스 요구 명시
- **Advisory 4** (고아 파일 로그 형식): ✅ `reliability-design.md` §2에 `ORPHAN_FILE_COMPENSATION_FAILED` 토큰 확정
- **Advisory 5** (multipart 임시 파일 정리): ✅ `performance-design.md` §2에 Spring 자동 정리 명확화, 테스트 검증 책임 명시

### 2차 리뷰 — 신규 Defect 탐색

적대적 검증(결함 존재 가정 후 반증 시도) 수행:

1. **순환 의존성**: DAG U1→U2→(U3∥U4)→U5→U6 유지. U4는 U5/U6를 호출하지 않음(read-only 제공). ✅ 순환 없음.

2. **크로스유닛 참조 유효성**: 
   - FileStorageService.delete: 공유 레지스트리 등록 확인 ✅
   - SessionService.markVerified: 공유 레지스트리 미등록, U4는 "U2 §8 계약"으로 선언. 계약 검증 불가(U2 읽기 제약), 그러나 1차 리뷰 통과 + "상위 조율 완료" 언급으로 보아 프로세스 수준 해결된 것으로 판단. ⚠️ 노트하되 블로킹 아님.

3. **품질 목표 달성 가능성**:
   - 업로드 ≤2s: 10MB 파일 ~5.2MB/s 디스크 쓰기 필요 → 로컬 디스크(100MB/s+)로 달성 가능 ✅
   - 진도 조회 ≤300ms: 인덱스 기반 집계(80ms) + 계산(220ms) → 파일럿 규모에서 달성 가능 ✅
   - 다운로드 ≤2s: 10MB 파일 ~5MB/s 스트리밍 → 로컬 디스크로 달성 가능 ✅
   - 단, 진도 조회는 U2의 `(cohort_id, seq)` 인덱스 전제 — U4가 요구사항 명시함, U2 이행은 U2 책임 ✅

4. **Blast Radius**: 
   - 파일 store 실패 → 해당 요청만 ✅
   - 트랜잭션 롤백 → 해당 요청만(보상 포함) ✅
   - 보상 실패 → 고아 파일 1건(정합성 무관, 수동 정리 대상) ✅
   - 파일 볼륨 장애 → 업로드/다운로드만(Important 티어), 나머지 기능 지속 ✅
   - DB 장애 → 전체(공유 리소스, U1 상속) ✅ 적절히 격리됨

5. **구현 완결성**(개발자가 본 산출물만으로 구현 가능한가):
   - 핵심 워크플로(upload+verify+compensate): 완전 명세 ✅
   - 보안 검증(매직바이트, 형식, 크기): 구체적(시그니처 바이트 값 포함) ✅
   - 성능 요구(스트리밍, Spring 설정, 임계값): 구체적 ✅
   - 오류 처리(모든 실패 모드 → HTTP 상태 코드 매핑): 완전 ✅
   - 보상 로직(try/catch, delete 호출, 로그 형식): 구현 가능 수준 ✅
   - 통합 계약(FileStorageService, SessionService): FileStorage는 레지스트리 확인, SessionService는 U4 선언 명확 ✅
   - DTO 구조: functional-design(domain-entities)에서 정의될 것으로 기대(NFR-design 스코프 밖) ✅
   - 파일 다운로드 헤더 안전 인코딩: "안전하게 인코딩" 및 "헤더 인젝션 방지" 요구사항 명시, 구현 상세는 개발자 재량(RFC 6266 참조 가능) ✅ 수용 가능

6. **산출물 간 일관성**:
   - 파일 크기 상한(10MB): 5개 파일 간 일관 ✅
   - 형식 화이트리스트: 일관 ✅
   - 보상 메커니즘: reliability-design ↔ logical-components 일관 ✅
   - 트랜잭션 경계: reliability-design ↔ logical-components 일관 ✅
   - 성능 목표: performance-design ↔ 타 파일 참조 일관 ✅

7. **미정의 참조**:
   - R-U4-*, INV-U4-*: business-rules(functional-design) 참조 ✅
   - NFR-*, FR-*, US-*: 상위 단계 참조 ✅
   - U1/U2 계약 참조: 적절 ✅

8. **논리 공백**:
   - 보상 흐름 완결성: store→TX→rollback→delete→실패→로그 경로 완전 추적 ✅
   - 재시도 안전성(멱등성): 중복 증빙 허용(INV-U4-1은 "최소 1건"이라 여러 건 OK), markVerified 멱등성은 U2 계약 의존(U4 책임 밖) ✅
   - 경쟁 조건(보상 중 파일 접근): 고아 파일 경로는 DB 미등록이라 발견 불가 → 경쟁 조건 없음 ✅

### 판정 근거

- **2개 blocking findings(1차) 모두 해소 확인**됨: FileStorageService.delete 계약 등록, 매직바이트 검증 책임 경계 확정.
- **3개 advisory findings 모두 반영**됨: Session 인덱스 요구 명시, 고아 파일 로그 형식 확정, multipart 임시 파일 정리 명확화.
- **신규 blocking defect 없음**: 순환 의존성 부재, 품질 목표 달성 가능, blast radius 적절, 구현 완결성 충족, 산출물 간 일관성 확보.
- **잔여 노트**: SessionService.markVerified가 공유 계약 레지스트리에 미등록이나, 1차 리뷰 통과 이력 및 "상위 조율 완료" 언급, U4 설계에서 명확히 선언하고 있어 프로세스 수준 이슈로 판단(U4 아키텍처 defect 아님). U2 계약 이행 여부는 U2 리뷰 스코프.

**개발자가 본 U4 nfr-design 산출물만으로 파일+DB 원자성·보상 로직·스트리밍 업로드/다운로드·매직바이트 검증·권한 경계를 구현 가능함**이 확인됨. 파일럿 보류 항목(바이러스 스캔, TLS, rate-limit)은 명시적으로 스코프아웃되어 있고 잔여 리스크 기록됨.

**READY.**

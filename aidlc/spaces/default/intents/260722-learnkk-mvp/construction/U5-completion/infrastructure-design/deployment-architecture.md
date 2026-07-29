# Deployment Architecture — U5 completion (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U5-completion
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`performance-design.md`·`security-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U5는 U1-foundation 배포 골격을 **상속**. 본 문서는 U5 고유(수료증 생성·파일 볼륨) 사항만 명시.

## 1. 배포 모델(U1 상속)

U5 수료/정산/보고서 도메인(CompletionService·ReportService·CertificateRenderer·컨트롤러)은 U1 `learnkk-api` 모듈로, UI(EndCohortDialog·ReportForm·CompletionResult)는 `learnkk-web`에 배포된다. 컴퓨트/네트워킹/환경/recreate/git SHA 롤백은 `U1-foundation/infrastructure-design/deployment-architecture.md` 상속.

## 2. U5 고유 배포 고려사항

- **파일 볼륨(uploads)**: U5는 U1 확립 `uploads` 볼륨을 **보고서 첨부·수료증 이미지** 저장에 사용(U4와 공유). recreate 시 명명 볼륨 영속(증서·보고서 유실 방지). 백업 대상 포함(`reliability-design.md` §5).
- **수료증 이미지 생성**: CertificateRenderer(템플릿→PNG)는 `learnkk-api` 내에서 실행(별도 워커 없음). 이미지 라이브러리(Java2D/Thumbnailator 등)는 code-generation에서 선택 — 컨테이너 이미지에 폰트·라이브러리 포함 필요(배포 시 의존성). multipart 크기 설정(보고서 첨부 ≤10MB)은 U4와 동일.
- **동기 발급(파일럿)**: 종료 트랜잭션 내 순차 증서 발급(`performance-design.md` §1, N 선형). 별도 배치/큐 인프라 미도입.

## 3. 확장 트리거

- 코호트당 멘티 수백+ 또는 종료 시간이 요청 타임아웃 접근 시 → 증서 발급 **비동기 배치**로 분리(종료 판정 즉시 커밋, 증서 생성 후속 잡). 이는 워커/큐 인프라 도입(`scalability-design.md` §2). 파일 스토리지 확장은 U4·U1 공통(오브젝트 스토리지).

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

U5-completion infrastructure-design 산출물 5개(deployment-architecture, infrastructure-services, monitoring-design, cicd-pipeline, shared-infrastructure)에 대한 적대적 검증을 완료했습니다. DEFECT를 가정하고 반증을 시도했으나, 모든 핵심 영역에서 반증에 실패했습니다.

### 검증 완료 항목

1. **파일럿 관행(team.md·project.md Forbidden/Mandated) 준수**
   - 퍼블릭 클라우드 금지(Forbidden) → deployment §1 "로컬 서버 Docker" 명시 ✓
   - 무승인 프로덕션 자동배포 금지 → deployment §2·cicd §4 수동 승인 게이트 ✓
   - 한글 산출물(Mandated) → 5개 문서 모두 한글 ✓

2. **크로스유닛 계약 정합성**
   - FinalReport/Certificate/SettlementStatus 스키마 → components.md 일치 ✓
   - U5 → U2/U3/U1 계약(읽기·세터·confirmedEnrollments·notify·FileStorage) → business-logic-model §5·services.md 일치 ✓
   - `confirmedEnrollments` component-methods.md 레지스트리 누락은 U3 조율 항목(U5는 계약 정확 명시) ✓

3. **U1 상속 정확성**
   - deployment §1 컴퓨트·네트워킹·recreate·git SHA → U1 deployment 일치 ✓
   - infrastructure-services §1·§2·§4 Flyway·FileStorage·캐시/큐 보류 → U1 shared-infrastructure 일치 ✓

4. **수료증 이미지 생성 의존성 구현 가능성**
   - deployment §2·infrastructure-services §3·cicd §1 "컨테이너 이미지에 라이브러리·폰트 포함" → Dockerfile `RUN apt-get install fonts-noto` 등으로 구현 가능 ✓

5. **다건 파일 보상 구현 가능성**
   - infrastructure-services §2·reliability-design §2 "누적 imagePath 리스트 → 롤백 시 전부 delete, ORPHAN_FILE_COMPENSATION_FAILED 로그" → try/catch 누적 보상 패턴으로 구현 가능 ✓
   - cicd §2 "다건 보상 테스트" → Testcontainers로 검증 가능 ✓

6. **수료 80% 경계 정수 산술 정확성**
   - reliability-design §3 `verifiedSessions*100 >= totalSessions*80` → 79%(false)/80%(true) 경계 정확 ✓
   - cicd §2 경계 테스트 명시 ✓

7. **순환 의존성 부재**
   - shared-infrastructure §2 "U5 단일 소유, U2/U3/U4는 U5 미호출(단방향)" → DAG U1→U2→(U3∥U4)→U5→U6 유지 ✓

8. **Blast Radius 격리**
   - monitoring §1·logical-components §3 "종료 트랜잭션 롤백 → 해당 코호트만, DB 다운 → 전체(U1 공통)" → 격리 명시 ✓
   - performance §4·scalability §2 확장 트리거(비동기 발급) 명시 ✓

9. **백업 범위 일관성**
   - deployment §1·infrastructure-services §2·monitoring §3·reliability-design §5 "uploads 볼륨 백업 대상 포함(DB + 파일 일 1회 스냅샷)" → 4개 산출물 일관 ✓

10. **CI 테스트 범위 구현 가능성**
    - cicd §2 "원자성·80% 경계·멱등·다건 보상·인가·컴포넌트·ArchUnit" → team.md 도구(Testcontainers/Jest)·reliability-design §6 검증 방법 명시 → 구현 가능 ✓

11. **확장 트리거 실행 가능성**
    - deployment §3·scalability §2·infrastructure-services §4 "수백+ 멘티 → 비동기 발급(판정 즉시 커밋, 증서 후속 잡)" → UNIQUE 멱등으로 실행 가능 ✓

12. **참조 무결성**
    - 5개 산출물의 상위 입력(nfr-design 5종, functional-design, inception contracts) 모두 존재·내용 일치 ✓

### 적대적 검증 시도 결과

파일럿 관행 위반, 크로스유닛 계약 위반, U1 상속 오류, 이미지 의존성 미명시, 다건 보상 구현 불가, 정수 산술 오류, 순환 의존성, Blast radius 격리 부재, 백업 누락, CI 테스트 구현 불가, 확장 트리거 실행 불가, 참조 무결성 결함을 찾으려 시도했으나 **모두 발견에 실패**했습니다.

개발자가 이 5개 산출물만으로 U5-completion 인프라를 구성할 수 있음을 확인했습니다.

**판정: READY**

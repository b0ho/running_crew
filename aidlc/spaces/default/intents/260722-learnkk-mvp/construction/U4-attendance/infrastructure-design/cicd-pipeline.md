# CI/CD Pipeline — U4 attendance (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U4-attendance
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/security-design.md`·`reliability-design.md`·`performance-design.md`·`scalability-design.md`·`logical-components.md`, `inception/application-design/components.md`·`services.md`, `functional-design/business-logic-model.md`
> 전제: U1 GitHub Actions 파이프라인 상속. U4는 파일+DB 테스트를 CI에 추가.

## 1. 파이프라인(U1 상속)

U1과 동일 저장소·워크플로. CI·git SHA 아티팩트·CD(staging 자동/production 수동 승인)·롤백·시크릿은 `U1-foundation/infrastructure-design/cicd-pipeline.md` 상속.

## 2. U4 CI 테스트 추가(머지 게이트)

- **파일+DB 원자성**: 업로드→인증(markVerified)→이력 저장이 동일 트랜잭션, 강제 롤백 시 회차 미인증·이력 0건(Testcontainers + 임시 디렉토리 파일 저장).
- **고아 파일 보상 delete**: 롤백 유도 후 저장 파일 삭제 확인.
- **파일 검증**: 형식(매직바이트)·크기(10MB 초과 413/400) 거부 테스트.
- **권한**: 소유 멘토 업로드·참여자/관리자 다운로드 인가 테스트.
- **learnkk-web**: FileUpload·진도 탭 컴포넌트 테스트.
- ArchUnit DTO 경계.

## 3. 배포·시크릿(U1 상속)

recreate·git SHA 롤백·Actions Secrets 상속. multipart 크기 설정은 배포 설정. U4 테이블·인덱스는 Flyway 자동 적용. uploads 볼륨 백업 잡 배포(`monitoring-design.md` §3).

## 4. 파일럿 보류(U1 상속)

파일 바이러스/콘텐츠 심층 스캔 보류(`cid:practices-discovery:c3`, `security-design.md` §3), SCA·이미지 스캔·고급 배포 보류. 확장 시 CI에 스캔 계층 추가.

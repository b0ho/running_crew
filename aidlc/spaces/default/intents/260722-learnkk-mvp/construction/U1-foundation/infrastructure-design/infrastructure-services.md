# Infrastructure Services — U1 foundation (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U1-foundation
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/scalability-design.md`·`reliability-design.md`·`security-design.md`·`performance-design.md`·`logical-components.md`, `inception/application-design/components.md`(엔티티·FK), `services.md`(FileStorageService), `functional-design/business-logic-model.md`(Flyway 시드)
> 전제: 로컬 Docker, 클라우드 서비스 미사용(Forbidden). U1이 공유 인프라 서비스를 확립.

## 1. 데이터베이스(PostgreSQL)

`components.md`(RDB·FK 정책)·`reliability-design.md`를 확정한다.
- **엔진/버전**: PostgreSQL 16, 단일 인스턴스(복제 없음 — 파일럿). 명명 볼륨 `pgdata` 영속.
- **스키마 관리**: Flyway 마이그레이션(전 유닛 공유 스키마 — U1이 마이그레이션 골격 확립). 관리자 시드 마이그레이션 포함(`business-logic-model.md` §5, 멱등·부팅 검증).
- **커넥션**: HikariCP pool 15(`performance-design.md` §3). db `max_connections` 기본(100) 이내 안전.
- **FK/삭제 정책**: `components.md` 관계표대로(코호트 하위 CASCADE, User RESTRICT). 파일럿은 하드 삭제 대신 상태 전이 우선.
- **읽기 복제본·샤딩**: 미도입(파일럿 소량 데이터, `scalability-design.md`).

## 2. 파일 스토리지(로컬 볼륨)

`services.md`(FileStorageService)·`security-design.md` §6:
- **저장 위치**: 명명 볼륨 `uploads`, **웹루트 밖** 마운트. api 컨테이너만 접근. 서버 생성 UUID 파일명.
- **제약**: 이미지(jpg/png)·pdf, ≤10MB(U4/U5 사용). 경로 이탈 방지(canonical path).
- **확장 교체점**: 다중 인스턴스 시 공유 스토리지(NFS/오브젝트 스토리지)로 교체 — FileStorageService 인터페이스 뒤로 격리(`scalability-design.md` §2). 파일럿은 로컬 볼륨.

## 3. 캐시 / 메시지 큐 / 검색

- **캐시(Redis 등)**: 미도입. 근거: <100명·BCrypt 지배 비용에서 캐시 이득 미미(`performance-design.md` §5). 확장 시 세션 스토어/지표 캐시로 도입 검토.
- **메시지 큐**: 미도입. 알림은 동일 트랜잭션 DB 레코드(`reliability-design.md`). 비동기 알림은 브로커 도입 확장 과제.
- **검색 서비스**: 미도입. 코호트 목록은 RDB 인덱스+페이지네이션으로 충분. 확장 시 전문 검색 검토.

## 4. DNS / 로드밸런서 / CDN

- **로드밸런서**: 미도입(단일 인스턴스). 확장(인스턴스 ≥2) 시 리버스 프록시/LB 도입.
- **CDN**: 미도입. 정적 자산은 web 컨테이너가 직접 서빙(파일럿 규모). 확장 시 CDN.
- **DNS**: 사내 로컬 호스트명/포트. 퍼블릭 도메인·TLS는 확장 후속.

## 5. 시크릿 관리

`security-design.md`(시크릿 환경변수화)·construction 가드레일:
- **주입 방식**: 환경변수 + `.env` 파일(git 커밋 금지, `.gitignore`). DB 비밀번호·관리자 시드 비밀번호(`ADMIN_EMAIL`/`ADMIN_PASSWORD`)·`security.bcrypt.cost` 등.
- **부팅 검증**: 필수 시크릿(관리자 계정) 미설정 시 부팅 중단(`business-logic-model.md` §5, R-U1-27).
- **로테이션**: 파일럿 수동. Vault/Secrets Manager는 클라우드 이관 확장 과제.
- **compliance**: PII 최소 수집(이메일·성명·닉네임), 데이터는 사내 로컬 서버에 잔류(외부 클라우드 전송 없음 — 데이터 잔류 통제 강점).

## 6. 서비스 디스커버리

- compose 내부 DNS(서비스명 `learnkk-db`, `learnkk-api`)로 컨테이너 간 통신. 별도 디스커버리 시스템 불필요(단일 스택).

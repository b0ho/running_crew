# Performance Design — U1 foundation (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 서포트 aws-platform(리소스·용량)
> 상위 입력: `nfr-requirements/performance-requirements.md`(응답시간·처리량 목표), `nfr-requirements/tech-stack-decisions.md`(Spring/JPA/HikariCP), `functional-design/business-logic-model.md`(로그인·가입·시드 흐름)
> 전제: <100명·로컬 단일 인스턴스. 고성능·저지연 요구 낮음 — "목표 달성에 필요한 최소 메커니즘"만 설계하고 고급 최적화는 명시적 스코프아웃.

## 1. 응답시간 예산(latency budget) 배분

`performance-requirements.md` §1의 목표 latency를 구간별로 분해하여 각 계층의 예산을 명시한다. BCrypt cost=10(NFR-SEC-1) 기준.

| 연산 | 총 목표 | 구간 배분(설계 예산) |
|---|---|---|
| 로그인 | ≤ 500ms | 라우팅/필터 ~20ms + `findByEmail`(인덱스 조회) ~10ms + **BCrypt matches ~200~350ms(cost=10 지배 요인)** + 세션 수립/응답 직렬화 ~30ms |
| 회원가입 | ≤ 600ms | 검증 ~10ms + 중복 조회 ~10ms + **BCrypt encode ~200~350ms** + insert ~20ms + 응답 ~30ms |
| 세션 확인(/me) | ≤ 150ms | 세션 조회(메모리) ~5ms + DTO 직렬화 ~20ms(해싱 없음) |
| 정적/헬스체크 | ≤ 100ms | 필터 + 즉시 응답, DB 미접근 |

**핵심 관찰**: 인증 경로의 지배적 비용은 BCrypt다. 따라서 성능 설계의 초점은 캐시가 아니라 **(a) BCrypt cost를 성능·보안 균형점(기본 10)으로 유지**, **(b) 인증 조회를 인덱스로 O(log n) 보장**, **(c) 커넥션 풀 병목 회피**다.

## 2. 데이터 접근 최적화

- **email UNIQUE 인덱스**: `users.email`에 UNIQUE 제약(=인덱스)을 두어 로그인/가입 중복조회를 O(log n)로 처리한다(`scalability-requirements.md` §1과 정합). email은 저장 전 소문자 정규화(business-logic-model §2)하여 인덱스 적중을 보장한다(대소문자 혼재로 인한 풀스캔 방지).
- **N+1 회피**: U1 도메인(User 단일 엔티티)은 연관 로딩이 없어 N+1 위험이 없다. 후속 유닛의 연관 조회는 각 유닛 nfr-design에서 다룬다.
- **읽기 전용 트랜잭션**: 로그인/`/me` 조회 경로는 `@Transactional(readOnly = true)`로 표시하여 flush 오버헤드를 제거한다.

## 3. 커넥션 풀(HikariCP) 설계

`tech-stack-decisions.md`의 HikariCP 소규모 설정을 구체화한다. `nfr-design-patterns.md`의 풀 사이징 공식(RPS × 평균 처리시간 × 1.5 버퍼) 적용:

- 인증 엔드포인트 목표 ≥ 20 req/s, 로그인 평균 처리시간 ~0.4s → 소요 커넥션 ≈ 20 × 0.4 × 1.5 ≈ 12.
- **설정값**: `maximum-pool-size = 15`, `minimum-idle = 5`, `connection-timeout = 5000ms`, `idle-timeout = 600000ms`, `max-lifetime = 1800000ms`.
- 로컬 단일 PostgreSQL의 `max_connections` 기본(100)을 넘지 않아 안전. 확장 시 인스턴스 수 × 풀 크기가 DB 한도를 넘지 않도록 재산정(§5 트리거).

## 4. 프론트엔드 성능(auth UI)

`business-logic-model.md` §8의 인증 UI 연동 계약 기준:
- React 번들은 Vite/CRA 프로덕션 빌드로 코드 스플리팅·minify. 파일럿 규모상 별도 CDN 미도입(로컬 배포), 정적 자산은 컨테이너/역방향 프록시가 직접 서빙.
- 중앙 API 클라이언트가 `credentials: include`로 세션 쿠키 전송. 로그인 응답 후 대시보드 라우팅은 클라이언트 사이드 네비게이션(전체 새로고침 회피).
- 인증 상태 확인(`/me`)은 앱 부팅 시 1회 호출 후 클라이언트 상태에 캐시(불필요한 반복 호출 억제).

## 5. 파일럿 스코프아웃 & 확장 트리거

- **미도입(파일럿)**: 애플리케이션 캐시 티어(Redis/Memcached), CDN 엣지 캐시, API 응답 캐시, 비동기 처리 큐. 근거: <100명·단일 인스턴스에서 캐시는 불필요한 복잡도이며 BCrypt가 지배 비용이라 캐시로 얻을 이득이 미미(`memory.md` Tradeoffs 정합).
- **확장 트리거**:
  - 활성 사용자 100명 초과 또는 인증 응답이 목표를 지속 미달 → 먼저 수직 확장(인스턴스 리소스·풀 크기 상향), 그다음 조회 캐시 검토.
  - 인스턴스 ≥ 2 → 커넥션 풀 총합이 DB 한도를 넘지 않도록 풀 크기 재산정 + 세션 외부화(`scalability-design.md`).
- **엄밀 성능 검증(통계적 p95/부하)** 은 `performance-requirements.md` §4대로 Operation 단계 performance-validation으로 이관. 본 파일럿은 목표 latency 대비 단건 스모크로 갈음.

## Review

**리뷰어:** aidlc-architecture-reviewer-agent

**판정: READY**

아키텍처 리뷰어의 적대적 검증(defect가 존재한다고 가정하고 반증 시도)을 통과했습니다. 다음 관점에서 검증했습니다:

### 검증 완료 항목

1. **상위 요구 → 설계 매핑 완전성**
   - performance-requirements의 4개 latency 목표 → BCrypt 지배 비용을 포함한 구간별 예산 배분으로 구체화됨 ✓
   - security-requirements NFR-SEC-1~4 → changeSessionId() 메커니즘, cost 외부화·하한, 401 동일 매핑으로 구체화됨 ✓
   - scalability-requirements의 "수직 확장만·세션 로컬" → 인스턴스 1·세션 메모리, 확장 시 세션 외부화 트리거 명시됨 ✓
   - reliability-requirements의 시드 실패 부팅 중단 → fail-fast 메커니즘으로 반영됨 ✓

2. **파일럿 스코프아웃의 명시성**
   - 캐시·CDN·비동기 큐 미도입 + 근거("<100명·BCrypt 지배 비용이라 캐시 이득 미미") + 확장 트리거(100명 초과·응답 미달) 명시됨 ✓
   - 보안(TLS·파일스캔·rate-limit 보류), 신뢰성(서킷브레이커·재시도 미도입·외부 서비스 없음), 확장(세션 결합점·Redis/JWT 전환 경로) 모두 명시됨 ✓

3. **보안 요구의 구체 메커니즘**
   - BCrypt cost 외부화(`security.bcrypt.cost`, 기본 10, 하한 8), 세션 고정 방지(`sessionFixation().changeSessionId()`), 사용자 열거 방지(InvalidCredentialsException → 401 동일 매핑), 경로 이탈 방지(canonical path·저장 루트 하위 검증) 모두 구체 메커니즘 명시됨 ✓

4. **성능 설계의 근거와 사이징**
   - HikariCP 풀 사이징: RPS × 처리시간 × 1.5 공식 적용(20 × 0.4 × 1.5 ≈ 12 → pool-size 15) 근거 있음 ✓
   - email UNIQUE 인덱스 O(log n) 보장, 소문자 정규화로 인덱스 적중 보장 ✓
   - latency 예산: BCrypt cost=10 지배 요인, "인덱스·풀·cost 균형" 우선순위 명시 ✓

5. **확장 제약과 교체점**
   - 세션 어피니티 의존, 파일 볼륨 로컬(다중 인스턴스 시 공유 스토리지 교체) 명시 ✓
   - FileStorageService·세션 스토어 격리 지점, 상위 유닛 코드 변경 없이 백엔드 교체 가능 명시 ✓
   - 확장 트리거 테이블(인스턴스 ≥ 2 → 세션 외부화/공유 스토리지/풀 재산정) 실행 가능하게 구체화됨 ✓

6. **논리 컴포넌트 경계와 blast radius**
   - logical-components 인벤토리(컴포넌트·책임·적용 NFR 매핑), 실패 도메인(PostgreSQL=Critical, 파일=Important, springdoc=Nice to have), 완화 조치(백업·재기동·예외 격리) 명시 ✓
   - 다이어그램 + fallback 텍스트, 경계 원칙(DTO·레이어·분리 저장소) 명시 ✓

7. **크로스유닛 계약 정합성**
   - U1이 확립하는 공유 골격(SecurityConfig·GlobalExceptionHandler·FileStorageService·Flyway 규약·DTO 규약) 후속 유닛 상속 명시 ✓
   - 관리자 인가 메서드 레벨 `@PreAuthorize`로 강제, U3/U6이 자기 메서드에 애너테이션만 붙이면 인가 걸림(U1 필터 충돌 없음) 명시 ✓
   - 파일 delete 멱등(U4/U5 보상용) 명시 ✓

8. **구현 공백 여부**
   - 커넥션 풀 설정값 구체(maximum-pool-size=15, connection-timeout=5000ms 등), BCrypt 빈·필터체인·@EnableMethodSecurity·changeSessionId() 설정·경로 이탈 방지 메커니즘, 헬스체크·부팅 검증·재배포 스모크 절차, 컴포넌트 인벤토리·경계·실패 도메인·공유 리소스·교체점 모두 명시 ✓
   - **개발자가 본 산출물만으로 U1을 구현 가능함이 확인됨** ✓

### 적대적 검증 시도 결과

순환 의존성, 상위 계약 위반, 구현 공백, 파일럿 스코프아웃 근거 부재, 확장 트리거 실행 불가능성, quality 목표 달성 불가능성, blast radius 격리 부재를 찾으려 시도했으나 **모두 발견에 실패**했습니다. READY는 이 반증 실패 후 도달한 판정입니다.

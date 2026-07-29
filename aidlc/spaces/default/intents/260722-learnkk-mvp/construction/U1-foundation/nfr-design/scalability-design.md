# Scalability Design — U1 foundation (LearnKK 파일럿)

> Construction · nfr-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect · 서포트 aws-platform(용량·확장 경로)
> 상위 입력: `nfr-requirements/scalability-requirements.md`(부하 전망·확장 전략·트리거), `nfr-requirements/tech-stack-decisions.md`(세션 인증·단일 인스턴스), `functional-design/business-logic-model.md`(세션 수립·가입 경쟁)
> 전제: 로컬 단일 인스턴스, <100명. 수평 확장·오토스케일은 확장 후속 과제.

## 1. 확장 아키텍처(파일럿)

`scalability-requirements.md` §2의 "수직 확장만" 전략을 구체화한다.

- **단일 인스턴스 수직 확장**: FE 컨테이너 1 + BE 컨테이너 1 + PostgreSQL 1. 부하 증가 시 컨테이너에 할당된 CPU/메모리, HikariCP 풀 크기(`performance-design.md` §3)를 상향한다.
- **상태 위치**: 인증 상태는 **서버 세션(인스턴스 로컬 메모리)** 에 보관. 단일 인스턴스이므로 세션 공유 스토어 불필요(`scalability-requirements.md` §2 정합).
- **로드밸런서·오토스케일 미도입**: 인스턴스가 1개이므로 분산·라우팅 계층 불필요.

## 2. 상태 관리(Statelessness) 경계 분석

향후 수평 확장의 핵심 제약을 명시한다:
- 현재 설계는 **세션 어피니티(sticky)에 의존** — 서버 세션이 인스턴스 로컬이라 인스턴스가 2개 이상이 되면 세션 불일치가 발생한다. 이것이 U1이 확장에 남기는 유일한 상태 결합점이다.
- 그 외 U1 로직(가입·시드·파일 저장)은 무상태이거나 DB/볼륨에 위임되어 인스턴스 로컬 상태를 두지 않는다.
- **파일 저장 볼륨**: 현재 로컬 볼륨은 단일 인스턴스 가정. 다중 인스턴스 시 공유 스토리지(네트워크 볼륨/오브젝트 스토어)로 교체 필요 — `logical-components.md`의 `FileStorageService` 추상화가 이 교체 지점을 격리한다.

## 3. 데이터 확장

- **User 테이블 규모**: 수백 행 수준. 파티셔닝·샤딩 불필요.
- **인증 조회**: `email` UNIQUE 인덱스로 O(log n)(`performance-design.md` §2). 규모 증가에도 인덱스가 선형 열화를 방지.
- **읽기 복제본(read replica)**: 파일럿 미도입. 인증은 쓰기 드물고 조회 경량이라 단일 DB로 충분.

## 4. 동시성 확장

- U1 자체 고동시성 대상은 아님. 유일한 경쟁은 **회원가입 email 중복 경쟁** → `email` UNIQUE 제약이 최종 방어선(business-logic-model §2, R-U1-17c). 애플리케이션 사전 조회 통과 후 삽입 경쟁도 제약 위반→DUPLICATE_EMAIL 매핑으로 경쟁 안전.
- 고동시성 설계(선착순 참여·정원 마감)는 U3-enrollment의 관심사이며 U1 범위 외.

## 5. 확장 트리거 & 전환 경로(구체)

`scalability-requirements.md` §4의 트리거를 설계 수준 전환 절차로 명세:

| 트리거 | 즉시 조치 | 전환 대상 |
|---|---|---|
| 인스턴스 ≥ 2로 확장 | 세션 스토어 외부화(Redis) **또는** 무상태 JWT 전환 | 세션 결합점 제거(§2) |
| 인스턴스 ≥ 2 (동시) | 공유 파일 스토리지로 교체 | `FileStorageService` 백엔드 교체 |
| 인스턴스 ≥ 2 (동시) | 커넥션 풀 총합 재산정 | DB `max_connections` 초과 방지 |
| 활성 사용자 100명 초과 / 응답 목표 지속 미달 | 수직 확장 우선(리소스·풀 상향) | 이후 캐시/복제 검토 |

- 세션 전환은 project.md ## Tech Stack("JWT 확장 시 재검토")와 정합.
- 클라우드 이관은 위 트리거와 별개의 조직 결정(확장 후속 과제, Forbidden: 초기 파일럿 클라우드 제외).

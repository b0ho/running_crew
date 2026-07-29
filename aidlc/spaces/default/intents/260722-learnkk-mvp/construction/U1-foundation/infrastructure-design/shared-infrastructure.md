# Shared Infrastructure — U1 foundation (LearnKK 파일럿)

> Construction · infrastructure-design 단계 산출물 · 유닛 U1-foundation
> 리드 aws-platform · 관점 devsecops·compliance
> 상위 입력: `nfr-design/logical-components.md`(공유 리소스), `nfr-design/scalability-design.md`·`reliability-design.md`·`security-design.md`, `inception/application-design/components.md`(공유 스키마), `services.md`, `functional-design/business-logic-model.md`
> 목적: U1이 foundation으로서 **전 유닛이 공유하는 인프라 리소스와 소유·접근 경계**를 정의한다(U2~U6은 이를 상속).

## 1. 공유 인프라 리소스 인벤토리

파일럿은 단일 배포단위(`learnkk-api`)·단일 스택이므로 인프라 리소스는 전 유닛이 공유한다.

| 공유 리소스 | 종류 | 공유 유닛 | 소유/확립 |
|---|---|---|---|
| PostgreSQL(`learnkk-db`) | 데이터스토어 | 전 유닛(공유 스키마) | U1 확립(Flyway), 각 유닛 테이블 추가 |
| 파일 볼륨(`uploads`) | 스토리지 | U4(증빙)·U5(보고서/증서) | U1 확립(FileStorageService) |
| `learnkk-api` 컨테이너 | 컴퓨트 | 전 유닛(모듈 공존) | U1 확립(Spring 앱 골격) |
| `learnkk-web` 컨테이너 | 컴퓨트 | 전 유닛 UI | U1 확립(React 앱 골격) |
| Docker compose 스택·네트워크 | 오케스트레이션 | 전 유닛 | U1 확립 |
| GitHub Actions 파이프라인 | CI/CD | 전 유닛(저장소 단위) | U1 확립(`cicd-pipeline.md`) |
| 시크릿(`.env`/Actions Secrets) | 설정 | 전 유닛 | U1 확립 |

## 2. 공유 스키마 소유·접근 경계

`logical-components.md`(유닛별 공유 계약)·`components.md`(엔티티):
- **단일 공유 스키마, 테이블 소유는 유닛별**: U1(User) / U2(Cohort·Session·Announcement) / U3(Enrollment·Notification) / U4(AttendanceEvidence) / U5(FinalReport·Certificate·SettlementStatus).
- **접근 경계(캡슐화)**: 유닛은 타 유닛 테이블을 리포지토리로 직접 접근하지 않고 **서비스 계약**으로 상호작용(예: U4→U2 `SessionService.markVerified`, U5→U3 `confirmedEnrollments`, U6은 읽기 전용 리포팅 모델). 계약은 `component-methods.md` 레지스트리에 등록(nfr-design 조율 반영).
- **읽기 전용 예외**: U6은 리포팅 읽기 모델로 공유 스키마를 조인·집계(쓰기 없음, INV-U6-1).
- **Flyway 마이그레이션 순서**: U1 스키마 골격 → 유닛별 테이블(의존 DAG U1→U2→(U3∥U4)→U5→U6 순). 단일 마이그레이션 이력으로 일관 버전 관리.

## 3. 공유 파일 볼륨 경계

- `uploads` 볼륨은 U4(증빙)·U5(보고서·수료증)가 공유하되, **모든 접근은 U1 FileStorageService(store/load/delete)** 경유(직접 파일시스템 접근 금지). 서버 UUID 파일명으로 충돌 없음.
- 볼륨은 백업 대상(`monitoring-design.md` §6, DB + uploads 일 1회 스냅샷).
- 확장 교체점: 공유 스토리지(오브젝트 스토리지)로 교체 시 FileStorageService 구현만 변경(전 유닛 무영향).

## 4. 공유 리소스 확장 시 분리 경로

- 다중 인스턴스: 세션 외부화(Redis/JWT) + 공유 파일 스토리지 + LB(`scalability-design.md` 트리거). PostgreSQL은 단일 공유 유지(필요 시 읽기 복제본).
- 서비스 분리(마이크로서비스화): 유닛별 서비스 계약이 이미 캡슐화되어 있어(§2), 분리 시 계약을 원격 API로 승격하는 경로가 자연스러움. 파일럿은 모듈러 모놀리스로 유지.

## 5. 데이터 잔류 & compliance

- 전 데이터(계정·파일·이력)는 **사내 로컬 서버에 잔류**(퍼블릭 클라우드 전송 없음 — Forbidden). 사내 도구·외부 판매 없음. PII 최소 수집(이메일·성명·닉네임). 클라우드 이관 시 데이터 잔류·규제 재평가(확장 후속).

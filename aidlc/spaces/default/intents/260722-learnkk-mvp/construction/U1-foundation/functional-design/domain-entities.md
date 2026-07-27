# Domain Entities — U1 foundation (LearnKK 파일럿)

> Construction · functional-design 단계 산출물 · 유닛 U1-foundation
> 리드 architect (서포트 developer 기술 타당성)
> 상위 입력: `units-generation/unit-of-work.md`(U1 책임), `unit-of-work-story-map.md`(US-0/1/2), `requirements-analysis/requirements.md`(FR-1), `application-design/components.md`(User 엔티티·FK), `component-methods.md`(AuthService), `services.md`(AuthService·FileStorageService)
> 범위: U1은 공통 인프라 + 인증 + RBAC 시드. 본 문서는 U1이 **소유**하는 도메인 엔티티만 상세화한다(상위 유닛 엔티티는 후속 유닛 functional-design에서 다룸).

## 1. U1 소유 엔티티 개요

U1은 `application-design/components.md`의 10개 엔티티 중 **User** 하나를 소유·구현하며, 여기에 RBAC를 표현하는 역할 개념(Role)과 인증 부수 개념을 더한다. 나머지 9개 엔티티(Cohort, Session, Enrollment, …)는 U1의 스키마 마이그레이션 골격에는 등장하지만 실제 컬럼/로직은 각 소유 유닛(U2~U6)에서 채운다.

| 엔티티 | 소유 유닛 | U1에서의 처리 |
|---|---|---|
| User | **U1** | 전체 필드·제약·생명주기 구현 |
| Role (RBAC) | **U1** | 역할 모델 + 관리자 시드 |
| Cohort~Notification (9종) | U2~U6 | U1은 스키마 마이그레이션 골격만 준비(FK 정책은 components.md 준수), 로직은 소유 유닛에서 |

## 2. User 엔티티

`components.md`의 User 정의(id, email(unique), name, nickname, passwordHash, isAdmin)를 파일럿 요구(FR-1, PII 최소 수집)에 맞게 상세화한다.

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | BIGINT (PK, auto) | NOT NULL | 대리 키 |
| email | VARCHAR(254) | NOT NULL, **UNIQUE** | 로그인 식별자. RFC 형식 검증 |
| name | VARCHAR(100) | NOT NULL | 성명(실명) |
| nickname | VARCHAR(50) | NOT NULL | 표시용 닉네임 |
| passwordHash | VARCHAR(60) | NOT NULL | **BCrypt** 해시(원문·가역암호 저장 금지) |
| isAdmin | BOOLEAN | NOT NULL, default false | 관리자 역할 플래그(RBAC) |
| createdAt | TIMESTAMP | NOT NULL, default now | 가입 시각 |

- **미수집 PII**: 사번·전화·부서 등은 수집하지 않는다(scope 결정 `cid:scope-definition:c4`, Mandated: 최소 수집).
- **경계 노출 규칙**: API 경계에서는 `passwordHash`를 절대 노출하지 않는다. 응답 DTO(`UserDto`)는 `id, email, name, nickname, isAdmin`만 포함(Mandated: DTO 사용, Entity 직접 노출 금지 — NFR-7).

### 2.1 표현 규칙(컨텍스트 역할 vs 명시 역할)

- **멘토/멘티는 컨텍스트 역할**이다(코호트 개설=멘토, 참여=멘티). User 엔티티에 mentor/mentee 플래그를 두지 않는다(`cid:user-stories:c1`). 같은 사용자가 한 코호트에서는 멘토, 다른 코호트에서는 멘티일 수 있다.
- **관리자만 명시 역할**이다. `isAdmin` 불리언으로 표현하며, 일반 회원가입으로는 절대 부여되지 않는다(부트스트랩 시드로만 부여).

## 3. Role (RBAC) 모델

파일럿 규모에서는 별도 Role/Permission 테이블을 두지 않고 **User.isAdmin 불리언 + 컨텍스트 역할**로 RBAC를 표현한다(YAGNI — 확장 시 정규화된 role 테이블 도입 검토).

| 권한 주체 | 판정 근거 | 예시 |
|---|---|---|
| ADMIN | `User.isAdmin == true` | 대기 승인/거절, 지표·이력 조회(U3/U6) |
| MENTOR(컨텍스트) | 해당 Cohort.mentorId == 요청자 id | 코호트 수정·종료, 증빙 업로드(U2/U4/U5) |
| MENTEE(컨텍스트) | 해당 Cohort에 확정 Enrollment 보유 | 진도·출석 조회(U3/U4) |
| USER(인증됨) | 로그인 완료 | 코호트 탐색·참여 신청(U3) |

- U1은 **인증(authentication)**과 **관리자 권한 판정(isAdmin 기반 authorization)**의 골격만 제공한다. 컨텍스트 역할 판정 로직은 각 소유 유닛에서 소유 데이터로 수행한다.
- Spring Security 권한 표현: 인증 사용자에게 `ROLE_USER`, `isAdmin==true`이면 추가로 `ROLE_ADMIN` 부여.

## 4. User 생명주기 (상태 전이)

User는 상태 컬럼을 두지 않는 단순 생명주기다(파일럿). 논리적 상태만 기술한다.

```
[미가입] --signup--> [활성(Active)]
[활성] --(파일럿 범위 외: 비활성화/탈퇴 없음)--> [활성]
```

<!-- Text fallback: 미가입 사용자가 signup으로 활성 상태가 된다. 파일럿에서는 비활성화·탈퇴·삭제 흐름을 두지 않는다. -->

- 파일럿 범위 외: 계정 비활성화, 탈퇴, 이메일 변경, 관리자 승격 UI. (확장 후속 과제)
- 삭제 정책: `components.md`대로 User는 참여/멘토 이력이 있으면 **ON DELETE RESTRICT**. U1에서 하드 삭제 기능은 제공하지 않는다.

## 5. 인증 부수 개념 (엔티티 아님)

세션/토큰은 영속 엔티티로 두지 않는다(파일럿, 상태 저장 세션 우선 — memory Tradeoff 참조).

| 개념 | 성격 | 설명 |
|---|---|---|
| 인증 세션 | 서버 세션(HttpOnly 쿠키) | Spring Security 기본 세션. JWT 무상태 토큰은 확장 시 재검토 |
| 비밀번호 해시 | 값 객체(값 규칙) | BCrypt(cost 기본 10). business-rules.md 참조 |

## 6. 시드 데이터(RBAC 부트스트랩)

`services.md` 시드 절, `cid:application-design:c1`에 따라 **최초 관리자 계정을 Flyway 마이그레이션 시드로 부트스트랩**한다.

| 시드 항목 | 값(예시, 환경변수 주입) | 규칙 |
|---|---|---|
| 관리자 계정 | email=`admin@learnkk.local`, isAdmin=true | 비밀번호는 환경변수에서 주입해 BCrypt 해싱 후 시드(평문 하드코딩 금지) |

- 시드는 멱등이어야 한다: 판정 쿼리 `SELECT 1 FROM users WHERE email = :adminEmail LIMIT 1` 결과 존재 시 재삽입하지 않는다(business-rules R-U1-25).
- 시드 비밀번호를 마이그레이션 파일에 평문/커밋하지 않는다(business-rules R-U1-26 — Forbidden: 평문 저장, secret 하드코딩 금지).
- 시드 환경변수 미설정 시 조용히 스킵하지 않고 명시적 실패로 부팅 중단(business-rules R-U1-27).

## 7. 다른 유닛과의 관계(경계)

- U1의 User는 U2~U6 전 유닛이 참조하는 **추이적 선행 의존**이다(mentorId, menteeId, authorId, userId 등 FK 대상).
- U1은 **FileStorageService**(공통 인프라)도 제공하나 이는 서비스이지 엔티티가 아니다. 저장 파일 메타(AttendanceEvidence/Certificate 등)는 소유 유닛이 관리한다.

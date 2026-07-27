# External Dependency Map — LearnKK (파일럿)

> Inception · delivery-planning 단계 산출물
> 상위 입력: `application-design/decisions.md`, `practices-discovery/team-practices.md`
> 근거: delivery-planning-questions.md (Q4=A 외부 의존 없음)

## 외부 의존성

- **외부 SaaS/API 연동 없음**(Q4=A). 로컬 서버·자체 계정 인증·오픈소스 라이브러리만 사용.

## 사용 오픈소스/도구 (라이브러리 수준)

| 항목 | 용도 | 비고 |
|---|---|---|
| React + Tailwind | 프론트엔드 | 오픈소스 |
| Spring Boot + Spring Security | 백엔드·인증 | BCrypt |
| RDB (예: PostgreSQL) + Flyway | 데이터·마이그레이션·시드 | 컨테이너 |
| springdoc-openapi | API 문서 | |
| Testcontainers, JUnit5, Jest/RTL | 테스트 | |
| Docker, GitHub Actions | 배포·CI | |

## 리스크

- 오픈소스 버전 고정(lockfile)로 재현성 확보(team-practices). 외부 연동 부재로 통합 리스크 낮음.
- 외부 미팅 링크는 멘토가 공지에 붙이는 URL일 뿐, 플랫폼 연동 아님(내장 화상 없음).

# LearnKK 파일럿 — U1 워킹 스켈레톤

사내 멘토링 과정 통합 관리 플랫폼(파일럿). 본 저장소는 워킹 스켈레톤(U1 foundation) — 실행 가능한 최소 앱 골격 + 인증 + RBAC 시드를 포함한다.

## 구성

| 디렉터리 | 내용 |
|---|---|
| `learnkk-api/` | Spring Boot 3.x (Java 17) 백엔드 — 인증·RBAC·에러·파일 골격 |
| `learnkk-web/` | React + Vite + TypeScript 프론트 — 인증 UI + 공통 셸 |
| `docker-compose.yml` | 로컬 스택(learnkk-db + learnkk-api + learnkk-web) |
| `.env.example` | 환경변수 템플릿(복사해 `.env` 작성) |

## 로컬 기동 (docker compose)

```bash
cp .env.example .env       # 값 채우기 (특히 DB_PASSWORD·ADMIN_EMAIL·ADMIN_PASSWORD)
docker compose up -d --build
```

- 프론트: http://localhost:8081
- API: http://localhost:8080 (Swagger UI: http://localhost:8080/swagger-ui.html)
- DB 포트는 호스트에 노출하지 않는다(내부 네트워크만).

> `ADMIN_EMAIL`/`ADMIN_PASSWORD` 가 비어 있으면 API 는 부팅을 중단한다(R-U1-27, 관리자 없는 상태 기동 방지).

## 워킹 스켈레톤 관통 경로

```
가입 → 로그인(세션 쿠키) → GET /api/auth/me 200 → 관리자 로그인 → GET /api/admin/ping 200
```

수동 확인(curl):

```bash
BASE=http://localhost:8080
# 1) 가입
curl -s -X POST $BASE/api/auth/signup -H 'Content-Type: application/json' \
  -d '{"email":"alice@learnkk.local","name":"앨리스","nickname":"al","password":"password123"}'
# 2) 로그인(쿠키 저장)
curl -s -c cookies.txt -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@learnkk.local","password":"password123"}'
# 3) /me (세션)
curl -s -b cookies.txt $BASE/api/auth/me
# 4) 관리자 로그인 + 스텁
curl -s -c admin.txt -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@learnkk.local","password":"<ADMIN_PASSWORD>"}'
curl -s -b admin.txt $BASE/api/admin/ping
```

## 저장소 규약 (team.md)

- FE/BE 분리(별도 파이프라인), OpenAPI 계약으로 동기화(springdoc).
- 트렁크 기반 개발, squash-merge, git SHA 버저닝, Docker recreate.
- CI 는 머지 전 린트+테스트 그린까지 블럭.

각 하위 저장소 상세는 `learnkk-api/README.md`, `learnkk-web/README.md` 참조.

# NFR Requirements — 관찰 일지 (memory)

> Construction · nfr-requirements 단계 진행 일지. 유닛별 반복.
> 표준 4개 H2: Interpretations / Deviations / Tradeoffs / Open questions.

## Interpretations

- 2026-07-27T03:30:00Z — NFR은 상위 requirements.md의 NFR-1~7과 team/project 관행(테스트 80%·로컬서버·BCrypt·보안 위생 보류·<100명)에서 대부분 pin됨. 유닛별 NFR은 공유 베이스라인 + 유닛 고유 델타(U3 동시성 성능, U4/U5 파일 보안/크기)로 구성.
- 2026-07-27T03:30:00Z — 파일럿 규모(<100명)·로컬 단일 서버 전제이므로 성능/확장/가용성 목표는 보수적으로 설정하고 고급 전략(HA·오토스케일·멀티리전)은 확장 후속으로 명시.

## Deviations

- 2026-07-27T03:30:00Z — Construction 질문 라운드 생략(project.md ## Corrections 학습 c2 적용): NFR이 requirements+관행에 pin되어 genuine gap 없음. 미결 발생 시 질문 파일 생성.

## Tradeoffs

- 2026-07-27T03:30:00Z — 보안 위생(파일 스캔·TLS·SCA)을 파일럿에서 보류(project.md Scope Overrides `cid:practices-discovery:c3`)하되 BCrypt·세션 쿠키·입력 검증·최소 PII는 하드 요구로 유지. 잔여 리스크는 확장 전 재검토로 명시.

## Open questions

- 2026-07-27T03:30:00Z — 파일럿 로컬 서버의 백업 주기·복구 목표(RPO/RTO) 구체 수치 미정. 운영 단계(observability/incident-response)에서 확정 예정. 현재는 정성 목표로 둠.

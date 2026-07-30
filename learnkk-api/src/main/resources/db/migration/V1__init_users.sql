-- V1 — users 테이블 (domain-entities.md §2, R-U1-02 email UNIQUE)
-- 시드 관리자는 Flyway 이후 실행되는 Spring ApplicationRunner(AdminSeeder)가 삽입한다.
-- 평문/해시 비밀번호를 마이그레이션에 커밋하지 않는다(R-U1-26).
CREATE TABLE users (
    id            BIGSERIAL     PRIMARY KEY,
    email         VARCHAR(254)  NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    nickname      VARCHAR(50)   NOT NULL,
    password_hash VARCHAR(60)   NOT NULL,
    is_admin      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT now()
);

-- email 은 대소문자 무시 유일. 애플리케이션이 저장 전 소문자 정규화(R-U1-02)하므로
-- 정규화된 값에 대한 표준 UNIQUE 인덱스로 충분하다.
CREATE UNIQUE INDEX ux_users_email ON users (email);

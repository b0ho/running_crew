package com.learnkk.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** User 리포지토리 — email 은 정규화된 소문자 값으로 조회한다(R-U1-02). */
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}

package com.learnkk.completion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Certificate 리포지토리 (performance-design.md §3, INV-U5-1). */
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

  /** 멘티별 수료증 조회 — 사전조회 멱등(재종료 skip) + 다운로드 스코프(certificateOf). */
  Optional<Certificate> findByCohortIdAndMenteeId(Long cohortId, Long menteeId);

  /** 발급 증서 수(종료 요약 issuedCertificateCount·U6 집계). */
  long countByCohortId(Long cohortId);
}

package com.learnkk.completion;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** SettlementStatus 리포지토리 (performance-design.md §3, INV-U5-2). */
public interface SettlementStatusRepository extends JpaRepository<SettlementStatus, Long> {

  /** 코호트별 정산 상태 1건 조회 — upsert 판정(존재 시 update, 없으면 insert). */
  Optional<SettlementStatus> findByCohortId(Long cohortId);
}

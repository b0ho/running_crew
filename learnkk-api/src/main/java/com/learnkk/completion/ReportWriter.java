package com.learnkk.completion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최종 보고서 저장 트랜잭션 빈 (business-logic-model.md §3, R-U5-16/17).
 *
 * <p>{@link ReportService} 와 <b>별도의 @Transactional 빈</b>으로 분리한 이유는 U4 {@code
 * AttendanceEvidenceWriter} 와 동일하다: {@code submit} 은 비트랜잭션 파일 I/O 와 보상 로직을 포함한 오케스트레이션이라 트랜잭션 경계를
 * 갖지 않아야 하며, self-invocation 이면 @Transactional 프록시가 적용되지 않는다. 따라서 순수 DB insert 만 이 빈으로 떼어낸다.
 */
@Component
public class ReportWriter {

  private final FinalReportRepository finalReportRepository;

  public ReportWriter(FinalReportRepository finalReportRepository) {
    this.finalReportRepository = finalReportRepository;
  }

  /** 보고서 1건 저장(첨부 경로는 store 성공 후에만 전달) (R-U5-16). */
  @Transactional
  public FinalReport persist(Long cohortId, Long authorId, String body, String filePath) {
    return finalReportRepository.save(FinalReport.of(cohortId, authorId, body, filePath));
  }
}

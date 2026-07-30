package com.learnkk.completion.dto;

import com.learnkk.completion.FinalReport;
import java.time.Instant;

/**
 * 최종 보고서 응답 DTO (INV-U1 Mandated, R-U5-15/18).
 *
 * <p>Entity 를 직접 노출하지 않는다. 첨부 파일 경로(내부 저장명)는 노출하지 않고 존재 여부({@code hasAttachment})만 노출하며, 실제 다운로드는
 * 별도 엔드포인트로 스트리밍한다(security-design.md §3).
 */
public record ReportDto(
    Long id,
    Long cohortId,
    Long authorId,
    String body,
    boolean hasAttachment,
    Instant submittedAt) {

  public static ReportDto from(FinalReport report) {
    return new ReportDto(
        report.getId(),
        report.getCohortId(),
        report.getAuthorId(),
        report.getBody(),
        report.hasAttachment(),
        report.getSubmittedAt());
  }
}

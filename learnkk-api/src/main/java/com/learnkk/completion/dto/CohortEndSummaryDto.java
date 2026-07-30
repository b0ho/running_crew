package com.learnkk.completion.dto;

/**
 * 코호트 종료 요약 응답 DTO (business-logic-model.md §2 8단계, US-4 종료).
 *
 * <p>종료 오케스트레이션 결과를 멘토에게 반환한다: 수료자 수·미수료 수·전체 확정 멘티 수·정산 조건 충족 여부·발급 증서 수. 서버측 판정 결과만 담으며 클라이언트가
 * 판정을 조작할 수 없다(security-design.md §2).
 */
public record CohortEndSummaryDto(
    int certifiedCount,
    int notCertifiedCount,
    int totalConfirmed,
    boolean settlementSatisfied,
    long issuedCertificateCount) {

  public static CohortEndSummaryDto of(
      int certifiedCount,
      int notCertifiedCount,
      int totalConfirmed,
      boolean settlementSatisfied,
      long issuedCertificateCount) {
    return new CohortEndSummaryDto(
        certifiedCount,
        notCertifiedCount,
        totalConfirmed,
        settlementSatisfied,
        issuedCertificateCount);
  }
}

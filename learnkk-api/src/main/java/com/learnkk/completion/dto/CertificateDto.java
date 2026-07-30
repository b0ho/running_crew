package com.learnkk.completion.dto;

import com.learnkk.completion.Certificate;
import java.time.Instant;

/**
 * 수료증 메타 응답 DTO (INV-U1 Mandated, US-12).
 *
 * <p>이미지 저장 경로(내부 저장명)는 노출하지 않는다. 실제 이미지는 별도 다운로드 엔드포인트로 스트리밍한다(security-design.md §3).
 */
public record CertificateDto(Long id, Long cohortId, Long menteeId, Instant issuedAt) {

  public static CertificateDto from(Certificate certificate) {
    return new CertificateDto(
        certificate.getId(),
        certificate.getCohortId(),
        certificate.getMenteeId(),
        certificate.getIssuedAt());
  }
}

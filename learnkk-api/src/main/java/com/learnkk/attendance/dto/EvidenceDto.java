package com.learnkk.attendance.dto;

import com.learnkk.attendance.AttendanceEvidence;
import java.time.Instant;

/**
 * 증빙 응답 DTO (INV-U4-1, security-design.md §4).
 *
 * <p>저장 원경로({@code filePath})는 노출하지 않는다. 파일 다운로드는 {@code id}(evidenceId)를 경유해 인증된 API 로만 접근한다(직접
 * URL 없음). Entity 는 API 경계에서 노출하지 않는다(ArchUnit DTO 경계).
 */
public record EvidenceDto(
    Long id, Long sessionId, String mimeType, long size, Long uploadedBy, Instant createdAt) {

  public static EvidenceDto from(AttendanceEvidence evidence) {
    return new EvidenceDto(
        evidence.getId(),
        evidence.getSessionId(),
        evidence.getMimeType(),
        evidence.getSize(),
        evidence.getUploadedBy(),
        evidence.getCreatedAt());
  }
}

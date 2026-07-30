package com.learnkk.metrics.dto;

import java.time.Instant;

/**
 * 증빙 이력 1건 응답 DTO (domain-entities.md §3.2, US-15 / R-U6-09).
 *
 * <p>관리자 증빙 이력 뷰에 노출되는 행이다. U4 AttendanceEvidence 를 Session·Cohort·User(업로더 성명)와 조인해 조립한다(N+1 회피 —
 * 생성자 표현식 JPQL). 파일 원경로(filePath)는 노출하지 않으며, 실제 다운로드는 U1 {@code FileStorageService.load} 를 경유하는 기존
 * 스트리밍 엔드포인트({@code GET /api/sessions/{sessionId}/evidence/{evidenceId}})로 수행한다(R-U6-11). 이를 위해
 * {@code sessionId} 를 함께 노출한다(다운로드 링크 조립용).
 *
 * @param evidenceId 증빙 id (다운로드 링크 조립용)
 * @param sessionId 회차 id (다운로드 링크 조립용 — R-U6-11)
 * @param cohortTitle 코호트 제목
 * @param sessionSeq 회차 순번
 * @param mimeType 파일 형식(MIME)
 * @param size 파일 크기(bytes)
 * @param uploadedBy 업로더 성명
 * @param createdAt 업로드 일시
 */
public record EvidenceHistoryItemDto(
    Long evidenceId,
    Long sessionId,
    String cohortTitle,
    int sessionSeq,
    String mimeType,
    long size,
    String uploadedBy,
    Instant createdAt) {}

package com.learnkk.metrics.dto;

import java.time.Instant;

/**
 * 보고서 이력 1건 응답 DTO (domain-entities.md §3.3, US-15 / R-U6-10).
 *
 * <p>관리자 보고서 이력 뷰에 노출되는 행이다. U5 FinalReport 를 Cohort·User(작성자 성명)와 조인하고 첨부 유무(filePath != null)를
 * 계산해 조립한다(N+1 회피 — 생성자 표현식 JPQL). 첨부 원경로는 노출하지 않고 존재 여부({@code hasAttachment})만
 * 노출한다(security-design.md §3). 증빙 이력과 별도 뷰로 조회한다(R-U6-08, FR-10 분리 조회).
 *
 * @param reportId 보고서 id
 * @param cohortId 코호트 id
 * @param cohortTitle 코호트 제목
 * @param authorName 작성자 성명
 * @param hasAttachment 첨부 유무
 * @param submittedAt 제출 일시
 */
public record ReportHistoryItemDto(
    Long reportId,
    Long cohortId,
    String cohortTitle,
    String authorName,
    boolean hasAttachment,
    Instant submittedAt) {}

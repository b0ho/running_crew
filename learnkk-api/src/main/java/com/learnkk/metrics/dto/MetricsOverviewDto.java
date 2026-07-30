package com.learnkk.metrics.dto;

/**
 * 운영 지표 개요 응답 DTO (domain-entities.md §3.1, US-14 / FR-11).
 *
 * <p>관리자 지표 탭에 노출되는 4개 집계 지표 + 집계 범위 라벨을 담는 읽기 모델이다. Entity 를 직접 노출하지 않는다(INV-U1 Mandated).
 * 출석률·수료율은 백분율(0~100) 값이며 표시 단계에서 소수 1자리로 반올림된 값이다(R-U6-04/05, 표시 전용). 집계 범위는 종료됨(CLOSED) 코호트
 * 기준이다(INV-U6-4).
 *
 * @param completedCohortCount 완주(종료됨) 코호트 수 (R-U6-06)
 * @param attendanceRate 전체 출석률(%) — Σ인증 회차 / Σ전체 회차, 분모 0 → 0 (R-U6-04, INV-U6-3)
 * @param completionRate 수료율(%) — 발급 증서 수 / 종료됨 코호트 확정 멘티 수, 분모 0 → 0 (R-U6-05, INV-U6-3)
 * @param certificateCount 발급 증서 수 — 전체 증서 count (R-U6-07)
 * @param scopeLabel 집계 범위 라벨 (예: "종료된 코호트 3건 기준")
 */
public record MetricsOverviewDto(
    int completedCohortCount,
    double attendanceRate,
    double completionRate,
    long certificateCount,
    String scopeLabel) {}

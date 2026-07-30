package com.learnkk.completion;

import com.learnkk.cohort.CohortService;
import com.learnkk.enrollment.NotificationService;
import com.learnkk.enrollment.NotificationType;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코호트 종료 원자적 커밋 (business-logic-model.md §2 4~7단계, INV-U5-3).
 *
 * <p>{@link CompletionService} 와 <b>별도의 @Transactional 빈</b>으로 분리한 이유는 U4 {@code
 * AttendanceEvidenceWriter} 와 동일하다: {@code endCohort} 는 비트랜잭션 이미지 I/O 와 보상 로직을 포함한 오케스트레이션이라 트랜잭션
 * 경계를 갖지 않아야 하며, self-invocation 이면 @Transactional 프록시가 적용되지 않는다.
 *
 * <p>{@link #finalizeEnd} 는 (1) 수료증 insert(사전조회 멱등 — 재종료 skip, INV-U5-1) + (2) 정산 상태
 * upsert(INV-U5-2) + (3) U2 {@code CohortService.closeByCompletion} 상태 전이(INV-U5-4) + (4) 확정 멘티 수료
 * 결과 알림(R-U5-20)을 한 트랜잭션에서 수행한다. 모두 함께 커밋되거나 함께 롤백된다(INV-U5-3). 알림 생성은 {@code
 * NotificationService.notify}(전파 REQUIRED)로 동일 트랜잭션에 참여한다.
 */
@Component
public class CompletionWriter {

  private final CertificateRepository certificateRepository;
  private final SettlementStatusRepository settlementStatusRepository;
  private final CohortService cohortService;
  private final NotificationService notificationService;

  public CompletionWriter(
      CertificateRepository certificateRepository,
      SettlementStatusRepository settlementStatusRepository,
      CohortService cohortService,
      NotificationService notificationService) {
    this.certificateRepository = certificateRepository;
    this.settlementStatusRepository = settlementStatusRepository;
    this.cohortService = cohortService;
    this.notificationService = notificationService;
  }

  /**
   * 종료 판정 결과를 원자적으로 반영한다.
   *
   * @param cohortId 대상 코호트
   * @param mentorId 코호트 멘토(정산 대상)
   * @param cohortTitle 알림 메시지용 코호트명
   * @param completionMet 수료 기준 충족 여부(확정 멘티 전원 공통, R-U5-07)
   * @param issuances 발급할 수료증 목록(미충족이면 빈 목록)
   * @param settlementSatisfied 정산 조건 충족 여부
   * @param confirmedMenteeIds 확정 멘티 목록(결과 알림 대상)
   */
  @Transactional
  public void finalizeEnd(
      Long cohortId,
      Long mentorId,
      String cohortTitle,
      boolean completionMet,
      List<CertificateIssuance> issuances,
      boolean settlementSatisfied,
      List<Long> confirmedMenteeIds) {

    // 1) 수료증 insert — 사전조회 후 없을 때만(재종료 멱등, INV-U5-1). UNIQUE 제약이 최종 방어선.
    for (CertificateIssuance issuance : issuances) {
      boolean exists =
          certificateRepository
              .findByCohortIdAndMenteeId(cohortId, issuance.menteeId())
              .isPresent();
      if (!exists) {
        certificateRepository.save(
            Certificate.issue(cohortId, issuance.menteeId(), issuance.imagePath()));
      }
    }

    // 2) 정산 상태 upsert — 코호트당 1건(INV-U5-2).
    settlementStatusRepository
        .findByCohortId(cohortId)
        .ifPresentOrElse(
            existing -> {
              existing.updateSatisfied(settlementSatisfied);
              settlementStatusRepository.save(existing);
            },
            () ->
                settlementStatusRepository.save(
                    SettlementStatus.of(cohortId, mentorId, settlementSatisfied)));

    // 3) 상태 전이 진행중→종료됨 — U2 가드 UPDATE 경로로만 수행(INV-U5-4).
    cohortService.closeByCompletion(cohortId);

    // 4) 확정 멘티 수료 결과 알림(R-U5-20). 동일 트랜잭션 참여.
    String message =
        completionMet
            ? "'" + cohortTitle + "' 코호트를 수료하셨습니다. 축하합니다!"
            : "'" + cohortTitle + "' 코호트 수료 기준(출석률 80%)에 도달하지 못해 미수료 처리되었습니다.";
    for (Long menteeId : confirmedMenteeIds) {
      notificationService.notify(menteeId, NotificationType.COMPLETION_RESULT, message);
    }
  }
}

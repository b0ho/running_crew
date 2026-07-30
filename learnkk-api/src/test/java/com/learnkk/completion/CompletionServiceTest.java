package com.learnkk.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.cohort.Session;
import com.learnkk.cohort.SessionRepository;
import com.learnkk.cohort.SessionStatus;
import com.learnkk.common.exception.DataIntegrityException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.completion.dto.CohortEndSummaryDto;
import com.learnkk.enrollment.EnrollmentService;
import com.learnkk.enrollment.EnrollmentStatus;
import com.learnkk.enrollment.dto.EnrollmentDto;
import com.learnkk.file.FileStorageService;
import com.learnkk.user.User;
import com.learnkk.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * CompletionService 단위 테스트 (Mockito) — 종료 사전검증·수료 80% 경계·정산 판정·이미지 보상(business-logic-model §2,
 * R-U5-01~11).
 *
 * <p>사전검증(404·403·409)·정합 오류(회차 0)·수료 경계(정수 비교)·정산 조건·수료증 이미지 생성/누적·트랜잭션 롤백 시 N건 보상 삭제를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CompletionServiceTest {

  @Mock private CohortRepository cohortRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private EnrollmentService enrollmentService;
  @Mock private UserRepository userRepository;
  @Mock private CertificateRenderer certificateRenderer;
  @Mock private FileStorageService fileStorageService;
  @Mock private CertificateRepository certificateRepository;
  @Mock private ReportService reportService;
  @Mock private CompletionWriter completionWriter;

  @InjectMocks private CompletionService completionService;

  private static final Long MENTOR_ID = 10L;
  private static final Long COHORT_ID = 1L;

  private Cohort cohort(Long id, Long mentorId, CohortStatus status) {
    Cohort c =
        Cohort.open(
            mentorId, "코호트", null, 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 5);
    ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "status", status);
    return c;
  }

  private List<Session> sessions(int total, int verified) {
    List<Session> list = new ArrayList<>();
    for (int seq = 1; seq <= total; seq++) {
      Session s = Session.scheduled(COHORT_ID, seq);
      ReflectionTestUtils.setField(s, "id", (long) seq);
      if (seq <= verified) {
        ReflectionTestUtils.setField(s, "status", SessionStatus.VERIFIED);
      }
      list.add(s);
    }
    return list;
  }

  private EnrollmentDto confirmedMentee(Long menteeId) {
    return new EnrollmentDto(
        menteeId,
        COHORT_ID,
        "코호트",
        menteeId,
        EnrollmentStatus.CONFIRMED,
        EnrollmentStatus.CONFIRMED.getDisplayName(),
        Instant.now(),
        Instant.now());
  }

  private User user(Long id, String name) {
    User u = User.newMember(name + "@learnkk.local", name, name, "$2a$10$dummyhashvalue000000");
    ReflectionTestUtils.setField(u, "id", id);
    return u;
  }

  @Test
  void endCohort_코호트_미존재면_404() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> completionService.endCohort(MENTOR_ID, COHORT_ID))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void endCohort_비소유_멘토면_403() {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, 999L, CohortStatus.ONGOING)));

    assertThatThrownBy(() -> completionService.endCohort(MENTOR_ID, COHORT_ID))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void endCohort_진행중_아니면_409() {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.CLOSED)));

    assertThatThrownBy(() -> completionService.endCohort(MENTOR_ID, COHORT_ID))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void endCohort_회차가_0이면_500_정합오류() {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    when(sessionRepository.findByCohortIdOrderBySeqAsc(COHORT_ID)).thenReturn(List.of());

    assertThatThrownBy(() -> completionService.endCohort(MENTOR_ID, COHORT_ID))
        .isInstanceOf(DataIntegrityException.class);
    verify(completionWriter, never())
        .finalizeEnd(
            anyLong(), anyLong(), anyString(), anyBoolean(), anyList(), anyBoolean(), anyList());
  }

  @Test
  void endCohort_수료경계_total5_verified4면_수료() {
    stubOngoing(sessions(5, 4), List.of(confirmedMentee(100L)));
    when(userRepository.findAllById(List.of(100L))).thenReturn(List.of(user(100L, "멘티A")));
    when(certificateRenderer.render(anyString(), anyString(), any())).thenReturn(new byte[] {1, 2});
    when(fileStorageService.store(any())).thenReturn("cert-100.png");
    // verified(4) != total(5) 이므로 정산 판정에서 mentorReportExists 는 단락 평가로 호출되지 않는다(스텁 불필요).
    when(certificateRepository.countByCohortId(COHORT_ID)).thenReturn(1L);

    CohortEndSummaryDto summary = completionService.endCohort(MENTOR_ID, COHORT_ID);

    assertThat(summary.certifiedCount()).isEqualTo(1);
    assertThat(summary.notCertifiedCount()).isZero();
    assertThat(summary.totalConfirmed()).isEqualTo(1);
    assertThat(summary.issuedCertificateCount()).isEqualTo(1L);
    // 수료 → 이미지 1건 생성/저장, writer 에 발급 목록 전달
    verify(fileStorageService).store(any());
    verify(certificateRenderer).render(eq("멘티A"), anyString(), any());
  }

  @Test
  void endCohort_수료경계_total5_verified3면_미수료_수료증없음() {
    stubOngoing(sessions(5, 3), List.of(confirmedMentee(100L)));
    when(certificateRepository.countByCohortId(COHORT_ID)).thenReturn(0L);

    CohortEndSummaryDto summary = completionService.endCohort(MENTOR_ID, COHORT_ID);

    assertThat(summary.certifiedCount()).isZero();
    assertThat(summary.notCertifiedCount()).isEqualTo(1);
    assertThat(summary.issuedCertificateCount()).isZero();
    // 미수료 → 이미지 생성/저장 없음
    verify(fileStorageService, never()).store(any());
    verify(certificateRenderer, never()).render(anyString(), anyString(), any());
  }

  @Test
  void endCohort_수료경계_total10_verified8이면_수료_verified7이면_미수료() {
    // 8/10 = 80% → 수료
    stubOngoing(sessions(10, 8), List.of(confirmedMentee(100L)));
    when(userRepository.findAllById(List.of(100L))).thenReturn(List.of(user(100L, "멘티A")));
    when(certificateRenderer.render(anyString(), anyString(), any())).thenReturn(new byte[] {1});
    when(fileStorageService.store(any())).thenReturn("cert.png");
    // verified(8) != total(10) → 정산 단락 평가로 mentorReportExists 미호출.
    when(certificateRepository.countByCohortId(COHORT_ID)).thenReturn(1L);

    assertThat(completionService.endCohort(MENTOR_ID, COHORT_ID).certifiedCount()).isEqualTo(1);
  }

  @Test
  void endCohort_수료경계_total10_verified7이면_미수료() {
    stubOngoing(sessions(10, 7), List.of(confirmedMentee(100L)));
    when(certificateRepository.countByCohortId(COHORT_ID)).thenReturn(0L);

    assertThat(completionService.endCohort(MENTOR_ID, COHORT_ID).certifiedCount()).isZero();
  }

  @Test
  void endCohort_정산조건_전회차인증_and_멘토보고서면_충족() {
    stubOngoing(sessions(5, 5), List.of(confirmedMentee(100L)));
    when(userRepository.findAllById(List.of(100L))).thenReturn(List.of(user(100L, "멘티A")));
    when(certificateRenderer.render(anyString(), anyString(), any())).thenReturn(new byte[] {1});
    when(fileStorageService.store(any())).thenReturn("cert.png");
    when(reportService.mentorReportExists(COHORT_ID, MENTOR_ID)).thenReturn(true);
    when(certificateRepository.countByCohortId(COHORT_ID)).thenReturn(1L);

    assertThat(completionService.endCohort(MENTOR_ID, COHORT_ID).settlementSatisfied()).isTrue();
  }

  @Test
  void endCohort_정산조건_전회차인증했으나_멘토보고서없으면_미충족() {
    stubOngoing(sessions(5, 5), List.of(confirmedMentee(100L)));
    when(userRepository.findAllById(List.of(100L))).thenReturn(List.of(user(100L, "멘티A")));
    when(certificateRenderer.render(anyString(), anyString(), any())).thenReturn(new byte[] {1});
    when(fileStorageService.store(any())).thenReturn("cert.png");
    when(reportService.mentorReportExists(COHORT_ID, MENTOR_ID)).thenReturn(false);
    when(certificateRepository.countByCohortId(COHORT_ID)).thenReturn(1L);

    assertThat(completionService.endCohort(MENTOR_ID, COHORT_ID).settlementSatisfied()).isFalse();
  }

  @Test
  void endCohort_트랜잭션_실패시_누적한_모든_수료증_이미지를_보상_삭제한다() {
    // 확정 멘티 3명 수료 → 이미지 3건 저장 후 writer 실패 → 3건 모두 delete
    stubOngoing(
        sessions(5, 5), List.of(confirmedMentee(1L), confirmedMentee(2L), confirmedMentee(3L)));
    when(userRepository.findAllById(anyList()))
        .thenReturn(List.of(user(1L, "A"), user(2L, "B"), user(3L, "C")));
    when(certificateRenderer.render(anyString(), anyString(), any())).thenReturn(new byte[] {1});
    when(fileStorageService.store(any())).thenReturn("cert-1.png", "cert-2.png", "cert-3.png");
    when(reportService.mentorReportExists(COHORT_ID, MENTOR_ID)).thenReturn(true);
    doThrow(new RuntimeException("DB 롤백"))
        .when(completionWriter)
        .finalizeEnd(
            anyLong(), anyLong(), anyString(), anyBoolean(), anyList(), anyBoolean(), anyList());

    assertThatThrownBy(() -> completionService.endCohort(MENTOR_ID, COHORT_ID))
        .isInstanceOf(IllegalStateException.class);

    verify(fileStorageService).delete("cert-1.png");
    verify(fileStorageService).delete("cert-2.png");
    verify(fileStorageService).delete("cert-3.png");
  }

  private void stubOngoing(List<Session> sessions, List<EnrollmentDto> confirmed) {
    when(cohortRepository.findById(COHORT_ID))
        .thenReturn(Optional.of(cohort(COHORT_ID, MENTOR_ID, CohortStatus.ONGOING)));
    when(sessionRepository.findByCohortIdOrderBySeqAsc(COHORT_ID)).thenReturn(sessions);
    when(enrollmentService.confirmedEnrollments(COHORT_ID)).thenReturn(confirmed);
  }
}

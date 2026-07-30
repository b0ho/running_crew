package com.learnkk.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.cohort.Cohort;
import com.learnkk.cohort.CohortRepository;
import com.learnkk.cohort.CohortStatus;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.ValidationException;
import com.learnkk.completion.dto.ReportDto;
import com.learnkk.completion.dto.ReportSubmitRequest;
import com.learnkk.enrollment.Enrollment;
import com.learnkk.enrollment.EnrollmentRepository;
import com.learnkk.enrollment.EnrollmentStatus;
import com.learnkk.file.FileStorageService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * ReportService 단위 테스트 (Mockito) — 본문 필수·참여자 인가·첨부 보상·mentorReportExists(business-logic-model §3,
 * R-U5-15~19).
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

  @Mock private FileStorageService fileStorageService;
  @Mock private ReportWriter reportWriter;
  @Mock private FinalReportRepository finalReportRepository;
  @Mock private CohortRepository cohortRepository;
  @Mock private EnrollmentRepository enrollmentRepository;

  @InjectMocks private ReportService reportService;

  private static final Long MENTOR_ID = 10L;
  private static final Long MENTEE_ID = 20L;
  private static final Long OUTSIDER_ID = 99L;
  private static final Long COHORT_ID = 1L;

  private Cohort cohort() {
    Cohort c =
        Cohort.open(
            MENTOR_ID, "코호트", null, 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 5);
    ReflectionTestUtils.setField(c, "id", COHORT_ID);
    ReflectionTestUtils.setField(c, "status", CohortStatus.ONGOING);
    return c;
  }

  private FinalReport report(Long id, Long authorId, String path) {
    FinalReport r = FinalReport.of(COHORT_ID, authorId, "본문", path);
    ReflectionTestUtils.setField(r, "id", id);
    ReflectionTestUtils.setField(r, "submittedAt", Instant.now());
    return r;
  }

  private MockMultipartFile pdf() {
    return new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[] {1, 2, 3});
  }

  @Test
  void submit_코호트_미존재면_404() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> reportService.submit(MENTOR_ID, COHORT_ID, new ReportSubmitRequest("본문"), null))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void submit_참여자가_아니면_403() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.of(cohort()));
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT_ID, OUTSIDER_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> reportService.submit(OUTSIDER_ID, COHORT_ID, new ReportSubmitRequest("본문"), null))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void submit_본문이_비면_400() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.of(cohort()));

    assertThatThrownBy(
            () -> reportService.submit(MENTOR_ID, COHORT_ID, new ReportSubmitRequest("  "), null))
        .isInstanceOf(ValidationException.class);
    verify(reportWriter, never()).persist(anyLong(), anyLong(), anyString(), any());
  }

  @Test
  void submit_멘토_첨부없음이면_순수_DB_트랜잭션() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.of(cohort()));
    when(reportWriter.persist(eq(COHORT_ID), eq(MENTOR_ID), eq("본문"), isNull()))
        .thenReturn(report(5L, MENTOR_ID, null));

    ReportDto dto = reportService.submit(MENTOR_ID, COHORT_ID, new ReportSubmitRequest("본문"), null);

    assertThat(dto.id()).isEqualTo(5L);
    assertThat(dto.hasAttachment()).isFalse();
    verify(fileStorageService, never()).store(any());
  }

  @Test
  void submit_확정멘티_첨부있음이면_store후_저장() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.of(cohort()));
    Enrollment confirmed = Enrollment.confirmed(COHORT_ID, MENTEE_ID);
    ReflectionTestUtils.setField(confirmed, "status", EnrollmentStatus.CONFIRMED);
    when(enrollmentRepository.findByCohortIdAndMenteeId(COHORT_ID, MENTEE_ID))
        .thenReturn(Optional.of(confirmed));
    when(fileStorageService.store(any())).thenReturn("stored.pdf");
    when(reportWriter.persist(eq(COHORT_ID), eq(MENTEE_ID), eq("본문"), eq("stored.pdf")))
        .thenReturn(report(6L, MENTEE_ID, "stored.pdf"));

    ReportDto dto =
        reportService.submit(MENTEE_ID, COHORT_ID, new ReportSubmitRequest("본문"), pdf());

    assertThat(dto.id()).isEqualTo(6L);
    assertThat(dto.hasAttachment()).isTrue();
    verify(fileStorageService).store(any());
  }

  @Test
  void submit_첨부저장후_트랜잭션_실패면_파일을_보상삭제한다() {
    when(cohortRepository.findById(COHORT_ID)).thenReturn(Optional.of(cohort()));
    when(fileStorageService.store(any())).thenReturn("orphan.pdf");
    doThrow(new RuntimeException("DB 롤백"))
        .when(reportWriter)
        .persist(anyLong(), anyLong(), anyString(), anyString());

    assertThatThrownBy(
            () -> reportService.submit(MENTOR_ID, COHORT_ID, new ReportSubmitRequest("본문"), pdf()))
        .isInstanceOf(IllegalStateException.class);
    verify(fileStorageService).delete("orphan.pdf");
  }

  @Test
  void mentorReportExists_리포지토리_위임() {
    when(finalReportRepository.existsByCohortIdAndAuthorId(COHORT_ID, MENTOR_ID)).thenReturn(true);

    assertThat(reportService.mentorReportExists(COHORT_ID, MENTOR_ID)).isTrue();
  }
}

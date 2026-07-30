package com.learnkk.cohort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.cohort.dto.CohortCreateRequest;
import com.learnkk.cohort.dto.CohortDetailDto;
import com.learnkk.cohort.dto.CohortDto;
import com.learnkk.cohort.dto.CohortUpdateRequest;
import com.learnkk.cohort.port.ConfirmedEnrollmentQuery;
import com.learnkk.common.exception.CapacityBelowConfirmedException;
import com.learnkk.common.exception.CohortClosedException;
import com.learnkk.common.exception.EntityNotFoundException;
import com.learnkk.common.exception.InvalidStateTransitionException;
import com.learnkk.common.exception.SessionVerifiedLockException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/** CohortService 단위 테스트 — 소유권·상태 전이·정원/회차 규칙(business-rules R-U2-07~11). */
@ExtendWith(MockitoExtension.class)
class CohortServiceTest {

  @Mock private CohortRepository cohortRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private AnnouncementRepository announcementRepository;
  @Mock private ConfirmedEnrollmentQuery confirmedEnrollmentQuery;
  @InjectMocks private CohortService cohortService;

  private static final Long MENTOR = 10L;
  private static final Long OTHER = 99L;

  private Cohort cohort(
      Long id, Long mentorId, CohortStatus status, int capacity, int sessionCount) {
    Cohort c =
        Cohort.open(
            mentorId,
            "코호트",
            "설명",
            capacity,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 1),
            sessionCount);
    ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "status", status);
    return c;
  }

  private CohortCreateRequest createReq(int capacity, int sessionCount) {
    return new CohortCreateRequest(
        "자바 멘토링", "설명", capacity, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), sessionCount);
  }

  private CohortUpdateRequest updateReq(int capacity, int sessionCount) {
    return new CohortUpdateRequest(
        "수정 제목", "설명", capacity, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), sessionCount);
  }

  @Test
  void create_정상이면_RECRUITING_코호트와_회차_N건_생성() {
    when(cohortRepository.save(any(Cohort.class))).thenAnswer(inv -> inv.getArgument(0));

    CohortDto dto = cohortService.create(MENTOR, createReq(20, 5));

    assertThat(dto.status()).isEqualTo(CohortStatus.RECRUITING);
    assertThat(dto.mentorId()).isEqualTo(MENTOR);
    assertThat(dto.sessionCount()).isEqualTo(5);

    ArgumentCaptor<List<Session>> captor = ArgumentCaptor.forClass(List.class);
    verify(sessionRepository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(5);
    assertThat(captor.getValue()).extracting(Session::getSeq).containsExactly(1, 2, 3, 4, 5);
  }

  @Test
  void update_존재하지_않으면_EntityNotFound() {
    when(cohortRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cohortService.update(MENTOR, 1L, updateReq(20, 5)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void update_소유자가_아니면_403_AccessDenied() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, OTHER, CohortStatus.RECRUITING, 20, 5)));

    assertThatThrownBy(() -> cohortService.update(MENTOR, 1L, updateReq(20, 5)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void update_종료됨_코호트면_409_CohortClosed() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.CLOSED, 20, 5)));

    assertThatThrownBy(() -> cohortService.update(MENTOR, 1L, updateReq(20, 5)))
        .isInstanceOf(CohortClosedException.class);
  }

  @Test
  void update_정원을_확정인원_미만으로_축소하면_409() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.RECRUITING, 20, 5)));
    when(confirmedEnrollmentQuery.confirmedCount(1L)).thenReturn(8);

    // 새 정원 5 < 확정 8
    assertThatThrownBy(() -> cohortService.update(MENTOR, 1L, updateReq(5, 5)))
        .isInstanceOf(CapacityBelowConfirmedException.class);
    verify(cohortRepository, never()).save(any());
  }

  @Test
  void update_정원_축소가_확정인원_이상이면_경고와_함께_허용() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.RECRUITING, 20, 5)));
    when(confirmedEnrollmentQuery.confirmedCount(1L)).thenReturn(3);
    when(cohortRepository.save(any(Cohort.class))).thenAnswer(inv -> inv.getArgument(0));

    // 회차 수 동일(5) → 회차 조정 없음
    CohortDto dto = cohortService.update(MENTOR, 1L, updateReq(10, 5));

    assertThat(dto.capacity()).isEqualTo(10);
    assertThat(dto.warnings()).isNotEmpty();
  }

  @Test
  void update_인증회차를_절단하는_회차수_축소면_409() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.RECRUITING, 20, 5)));
    // seq > 3 구간에 인증 회차 존재
    when(sessionRepository.countByCohortIdAndSeqGreaterThanAndStatus(1L, 3, SessionStatus.VERIFIED))
        .thenReturn(1L);

    assertThatThrownBy(() -> cohortService.update(MENTOR, 1L, updateReq(20, 3)))
        .isInstanceOf(SessionVerifiedLockException.class);
    verify(sessionRepository, never()).deleteByCohortIdAndSeqGreaterThan(anyLong(), eq(3));
  }

  @Test
  void update_회차수_증가면_추가_회차_생성() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.RECRUITING, 20, 3)));
    when(cohortRepository.save(any(Cohort.class))).thenAnswer(inv -> inv.getArgument(0));

    cohortService.update(MENTOR, 1L, updateReq(20, 6));

    ArgumentCaptor<List<Session>> captor = ArgumentCaptor.forClass(List.class);
    verify(sessionRepository).saveAll(captor.capture());
    // seq 4,5,6 추가
    assertThat(captor.getValue()).extracting(Session::getSeq).containsExactly(4, 5, 6);
  }

  @Test
  void start_모집중이면_진행중으로_전이() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.RECRUITING, 20, 5)))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.ONGOING, 20, 5)));
    when(cohortRepository.updateStatusGuarded(1L, CohortStatus.RECRUITING, CohortStatus.ONGOING))
        .thenReturn(1);

    CohortDto dto = cohortService.start(MENTOR, 1L);

    assertThat(dto.status()).isEqualTo(CohortStatus.ONGOING);
  }

  @Test
  void start_가드_UPDATE_영향행_0이면_409_InvalidStateTransition() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.ONGOING, 20, 5)));
    when(cohortRepository.updateStatusGuarded(1L, CohortStatus.RECRUITING, CohortStatus.ONGOING))
        .thenReturn(0);

    assertThatThrownBy(() -> cohortService.start(MENTOR, 1L))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void start_소유자가_아니면_403() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, OTHER, CohortStatus.RECRUITING, 20, 5)));

    assertThatThrownBy(() -> cohortService.start(MENTOR, 1L))
        .isInstanceOf(AccessDeniedException.class);
    verify(cohortRepository, never()).updateStatusGuarded(anyLong(), any(), any());
  }

  @Test
  void closeByCompletion_진행중이_아니면_409() {
    when(cohortRepository.updateStatusGuarded(1L, CohortStatus.ONGOING, CohortStatus.CLOSED))
        .thenReturn(0);

    assertThatThrownBy(() -> cohortService.closeByCompletion(1L))
        .isInstanceOf(InvalidStateTransitionException.class);
  }

  @Test
  void get_종료됨_코호트는_비소유_비관리자면_403() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.CLOSED, 20, 5)));

    assertThatThrownBy(() -> cohortService.get(1L, OTHER, false))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void get_진행중_코호트는_아무_인증사용자나_상세_조회() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.ONGOING, 20, 2)));
    when(sessionRepository.findByCohortIdOrderBySeqAsc(1L))
        .thenReturn(List.of(Session.scheduled(1L, 1), Session.scheduled(1L, 2)));
    when(announcementRepository.findTop5ByCohortIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

    CohortDetailDto detail = cohortService.get(1L, OTHER, false);

    assertThat(detail.sessions()).hasSize(2);
    assertThat(detail.recentAnnouncements()).isEmpty();
  }
}

package com.learnkk.cohort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.cohort.dto.AnnouncementCreateRequest;
import com.learnkk.cohort.dto.AnnouncementDto;
import com.learnkk.common.exception.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/** AnnouncementService 단위 테스트 — 소유권 작성·조회 권한(R-U2-15/18). */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

  @Mock private AnnouncementRepository announcementRepository;
  @Mock private CohortRepository cohortRepository;
  @InjectMocks private AnnouncementService announcementService;

  private static final Long MENTOR = 10L;
  private static final Long OTHER = 99L;

  private Cohort cohort(Long id, Long mentorId, CohortStatus status) {
    Cohort c =
        Cohort.open(
            mentorId, "코호트", "설명", 20, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), 5);
    ReflectionTestUtils.setField(c, "id", id);
    ReflectionTestUtils.setField(c, "status", status);
    return c;
  }

  @Test
  void create_소유_멘토면_공지_저장() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.ONGOING)));
    when(announcementRepository.save(any(Announcement.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    AnnouncementDto dto =
        announcementService.create(
            MENTOR, 1L, new AnnouncementCreateRequest("공지 본문", "https://meet.example.com/room"));

    assertThat(dto.body()).isEqualTo("공지 본문");
    assertThat(dto.externalLink()).isEqualTo("https://meet.example.com/room");
  }

  @Test
  void create_소유자가_아니면_403() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, OTHER, CohortStatus.ONGOING)));

    assertThatThrownBy(
            () -> announcementService.create(MENTOR, 1L, new AnnouncementCreateRequest("공지", null)))
        .isInstanceOf(AccessDeniedException.class);
    verify(announcementRepository, never()).save(any());
  }

  @Test
  void create_코호트_미존재면_404() {
    when(cohortRepository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> announcementService.create(MENTOR, 1L, new AnnouncementCreateRequest("공지", null)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void list_종료됨_코호트는_비소유_비관리자면_403() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.CLOSED)));

    assertThatThrownBy(() -> announcementService.list(1L, OTHER, false, PageRequest.of(0, 20)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void list_진행중_코호트는_인증사용자에게_최신순_반환() {
    when(cohortRepository.findById(1L))
        .thenReturn(Optional.of(cohort(1L, MENTOR, CohortStatus.ONGOING)));
    Announcement a = Announcement.create(1L, "공지", null);
    Page<Announcement> page = new PageImpl<>(java.util.List.of(a));
    when(announcementRepository.findByCohortIdOrderByCreatedAtDesc(
            org.mockito.ArgumentMatchers.eq(1L), any()))
        .thenReturn(page);

    Page<AnnouncementDto> result =
        announcementService.list(1L, OTHER, false, PageRequest.of(0, 20));

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).body()).isEqualTo("공지");
  }
}

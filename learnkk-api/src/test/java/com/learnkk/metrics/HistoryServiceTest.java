package com.learnkk.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.learnkk.metrics.dto.EvidenceHistoryItemDto;
import com.learnkk.metrics.dto.ReportHistoryItemDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * HistoryService 단위 테스트 (Mockito) — 페이지네이션 기본 20·파라미터 정규화·cohortId 필터 전달·hasAttachment 전달
 * (R-U6-08~10).
 *
 * <p>리포지토리를 mock 하여 (1) size 미지정(≤0)이면 기본 20, 음수 page 는 0 으로 정규화되는지, (2) cohortId 필터가 그대로
 * 전달되는지(null 포함), (3) 보고서 이력의 첨부 유무가 DTO 로 전달되는지 검증한다. 최신순 정렬·조인은 쿼리 ORDER BY 가 담당하므로 통합 테스트에서 실 DB
 * 로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

  @Mock private HistoryRepository historyRepository;

  @InjectMocks private HistoryService historyService;

  private EvidenceHistoryItemDto evidence(long id) {
    return new EvidenceHistoryItemDto(
        id, 5L, "코호트", 1, "image/png", 1024L, "업로더", Instant.parse("2026-01-01T00:00:00Z"));
  }

  private ReportHistoryItemDto report(long id, boolean hasAttachment) {
    return new ReportHistoryItemDto(
        id, 5L, "코호트", "작성자", hasAttachment, Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void evidenceHistory_size미지정이면_기본20_음수page는_0으로_정규화() {
    when(historyRepository.findEvidenceHistory(isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(evidence(1L))));

    historyService.evidenceHistory(null, -3, 0);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(historyRepository).findEvidenceHistory(isNull(), captor.capture());
    assertThat(captor.getValue().getPageNumber()).isZero();
    assertThat(captor.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  void evidenceHistory_cohortId필터가_그대로_전달된다() {
    when(historyRepository.findEvidenceHistory(eq(42L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(evidence(1L))));

    Page<EvidenceHistoryItemDto> page = historyService.evidenceHistory(42L, 0, 20);

    assertThat(page.getContent()).hasSize(1);
    org.mockito.Mockito.verify(historyRepository).findEvidenceHistory(eq(42L), any(Pageable.class));
  }

  @Test
  void evidenceHistory_지정한_page_size가_전달된다() {
    when(historyRepository.findEvidenceHistory(isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    historyService.evidenceHistory(null, 2, 10);

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(historyRepository).findEvidenceHistory(isNull(), captor.capture());
    assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
    assertThat(captor.getValue().getPageSize()).isEqualTo(10);
  }

  @Test
  void reportHistory_기본20_및_cohortId필터전달_및_hasAttachment전달() {
    when(historyRepository.findReportHistory(eq(7L), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(report(1L, true), report(2L, false))));

    Page<ReportHistoryItemDto> page = historyService.reportHistory(7L, 0, 0);

    assertThat(page.getContent())
        .extracting(ReportHistoryItemDto::hasAttachment)
        .containsExactly(true, false);
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    org.mockito.Mockito.verify(historyRepository).findReportHistory(eq(7L), captor.capture());
    assertThat(captor.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  void reportHistory_cohortId_null이면_전체조회로_위임한다() {
    when(historyRepository.findReportHistory(isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(report(1L, false))));

    Page<ReportHistoryItemDto> page = historyService.reportHistory(null, 0, 20);

    assertThat(page.getContent()).hasSize(1);
    org.mockito.Mockito.verify(historyRepository).findReportHistory(isNull(), any(Pageable.class));
  }
}

package com.learnkk.completion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 최종 보고서 도메인 엔티티 (domain-entities.md §2, US-11 / FR-7).
 *
 * <p>멘토·멘티가 제출하는 자유 서식 본문 + 선택 첨부(U1 FileStorage 경로). private 생성자 + static 팩토리({@link #of})로 생성하며
 * 세터를 두지 않는다. API 경계에는 절대 노출하지 않고 {@code ReportDto}로만 전달한다(INV-U1 Mandated). {@code authorId}가 코호트
 * 멘토 id 와 같은지로 멘토 보고서 여부를 판정한다(정산 판정 R-U5-11).
 */
@Entity
@Table(name = "final_report")
public class FinalReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cohort_id", nullable = false)
  private Long cohortId;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  @Column(nullable = false, columnDefinition = "text")
  private String body;

  @Column(name = "file_path", length = 512)
  private String filePath;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  protected FinalReport() {
    // JPA 전용 기본 생성자
  }

  private FinalReport(Long cohortId, Long authorId, String body, String filePath) {
    this.cohortId = cohortId;
    this.authorId = authorId;
    this.body = body;
    this.filePath = filePath;
  }

  /** 최종 보고서 제출 — 첨부는 선택(null 허용) (W-U5-2, R-U5-15). */
  public static FinalReport of(Long cohortId, Long authorId, String body, String filePath) {
    return new FinalReport(cohortId, authorId, body, filePath);
  }

  @PrePersist
  void prePersist() {
    if (this.submittedAt == null) {
      this.submittedAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getCohortId() {
    return cohortId;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public String getBody() {
    return body;
  }

  public String getFilePath() {
    return filePath;
  }

  /** 첨부 존재 여부 — DTO 노출 시 첨부 다운로드 가능 여부 표시에 사용. */
  public boolean hasAttachment() {
    return filePath != null && !filePath.isBlank();
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}

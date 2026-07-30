package com.learnkk.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 회차 증빙 도메인 엔티티 (domain-entities.md §2, INV-U4-1).
 *
 * <p>멘토가 회차(Session) 단위로 업로드한 증빙 파일의 메타 이력이다. 실제 파일 본체는 U1 {@code FileStorageService} 가 웹루트 밖에
 * 저장하며(INV-U4-2), 본 엔티티는 저장 경로({@link #filePath})와 검증된 MIME·크기·업로더만 보관한다. 재업로드 시 이전 증빙도 이력으로 보존한다(한
 * 회차에 복수 행 허용, R-U4-06).
 *
 * <p>private 생성자 + static 팩토리({@link #of}) 로 생성하며 getter 만 노출한다. API 경계에서는 절대 노출하지 않고 DTO 로만
 * 전달한다(INV-U4-1 유지, DTO 경계).
 */
@Entity
@Table(name = "attendance_evidence")
public class AttendanceEvidence {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "file_path", nullable = false, length = 512)
  private String filePath;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(nullable = false)
  private long size;

  @Column(name = "uploaded_by", nullable = false)
  private Long uploadedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AttendanceEvidence() {
    // JPA 전용 기본 생성자
  }

  private AttendanceEvidence(
      Long sessionId, String filePath, String mimeType, long size, Long uploadedBy) {
    this.sessionId = sessionId;
    this.filePath = filePath;
    this.mimeType = mimeType;
    this.size = size;
    this.uploadedBy = uploadedBy;
  }

  /** 증빙 이력 생성 — createdAt 은 @PrePersist 에서 채워진다(R-U4-06). */
  public static AttendanceEvidence of(
      Long sessionId, String filePath, String mimeType, long size, Long uploadedBy) {
    return new AttendanceEvidence(sessionId, filePath, mimeType, size, uploadedBy);
  }

  @PrePersist
  void prePersist() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public String getFilePath() {
    return filePath;
  }

  public String getMimeType() {
    return mimeType;
  }

  public long getSize() {
    return size;
  }

  public Long getUploadedBy() {
    return uploadedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

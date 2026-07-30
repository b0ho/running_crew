package com.learnkk.attendance;

import com.learnkk.cohort.SessionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 증빙 저장 + 회차 인증 원자적 커밋 (business-logic-model.md §2 3단계, INV-U4-1).
 *
 * <p>{@link AttendanceService} 와 <b>별도의 @Transactional 빈</b>으로 분리한 이유: {@code uploadEvidence} 는 파일
 * I/O(비트랜잭션)와 보상 로직을 포함한 오케스트레이션이라 트랜잭션 경계를 갖지 않아야 한다. 만약 같은 클래스의 메서드로 두면 self-invocation 이
 * 되어 @Transactional 프록시가 적용되지 않는다. 따라서 원자적 DB 작업만 이 빈으로 떼어낸다.
 *
 * <p>{@link #persistAndVerify} 는 증빙 이력 저장과 {@code SessionService.markVerified}(전파 REQUIRED — 동일
 * 트랜잭션 참여)를 한 트랜잭션에서 수행한다. 둘은 함께 커밋되거나 함께 롤백되므로 "인증됐는데 증빙 이력 없음"(INV-U4-1 위반)은 구조적으로 발생하지 않는다.
 */
@Component
public class AttendanceEvidenceWriter {

  private final AttendanceEvidenceRepository evidenceRepository;
  private final SessionService sessionService;

  public AttendanceEvidenceWriter(
      AttendanceEvidenceRepository evidenceRepository, SessionService sessionService) {
    this.evidenceRepository = evidenceRepository;
    this.sessionService = sessionService;
  }

  /**
   * 증빙 이력 1건 저장 + 회차 예정→인증 전이(원자적, R-U4-05/06).
   *
   * @return 저장된 증빙 엔티티
   */
  @Transactional
  public AttendanceEvidence persistAndVerify(
      Long sessionId, String filePath, String mimeType, long size, Long uploadedBy) {
    AttendanceEvidence saved =
        evidenceRepository.save(
            AttendanceEvidence.of(sessionId, filePath, mimeType, size, uploadedBy));
    // 회차 인증 전이는 U2 계약으로만 수행한다(INV-U4-4). 동일 트랜잭션(REQUIRED)에 참여한다.
    sessionService.markVerified(sessionId);
    return saved;
  }
}

package com.learnkk.cohort;

import com.learnkk.cohort.dto.SessionDto;
import com.learnkk.common.exception.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회차 서비스 (domain-entities.md §3, business-logic-model.md §8).
 *
 * <p>U2 는 회차 조회와 예정→인증 전이 세터를 제공한다. {@link #markVerified} 는 U4(attendance)가 증빙 업로드 시 호출하는 계약이며,
 * 리포지토리 직접 접근 대신 이 메서드를 통해 전이를 캡슐화한다.
 */
@Service
public class SessionService {

  private final SessionRepository sessionRepository;

  public SessionService(SessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  /** 코호트의 회차 목록(seq 오름차순). */
  @Transactional(readOnly = true)
  public List<SessionDto> listByCohort(Long cohortId) {
    return sessionRepository.findByCohortIdOrderBySeqAsc(cohortId).stream()
        .map(SessionDto::from)
        .toList();
  }

  /**
   * 예정→인증 전이 (U4 호출용). 이미 인증 상태면 멱등적으로 무시한다.
   *
   * @return 이 호출로 인증 전이가 발생했으면 true, 이미 인증이었으면 false
   */
  @Transactional
  public boolean markVerified(Long sessionId) {
    Session session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new EntityNotFoundException("회차를 찾을 수 없습니다"));
    boolean changed = session.markVerified();
    if (changed) {
      sessionRepository.save(session);
    }
    return changed;
  }
}

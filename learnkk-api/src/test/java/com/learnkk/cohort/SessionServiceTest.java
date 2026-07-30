package com.learnkk.cohort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.common.exception.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** SessionService 단위 테스트 — markVerified 전이·멱등(business-logic-model §8). */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

  @Mock private SessionRepository sessionRepository;
  @InjectMocks private SessionService sessionService;

  private Session session(Long id, SessionStatus status) {
    Session s = Session.scheduled(1L, 1);
    ReflectionTestUtils.setField(s, "id", id);
    ReflectionTestUtils.setField(s, "status", status);
    return s;
  }

  @Test
  void markVerified_예정_회차면_인증으로_전이하고_저장() {
    Session s = session(5L, SessionStatus.SCHEDULED);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(s));

    boolean changed = sessionService.markVerified(5L);

    assertThat(changed).isTrue();
    assertThat(s.getStatus()).isEqualTo(SessionStatus.VERIFIED);
    verify(sessionRepository).save(s);
  }

  @Test
  void markVerified_이미_인증이면_멱등적으로_무시() {
    Session s = session(5L, SessionStatus.VERIFIED);
    when(sessionRepository.findById(5L)).thenReturn(Optional.of(s));

    boolean changed = sessionService.markVerified(5L);

    assertThat(changed).isFalse();
    verify(sessionRepository, never()).save(any());
  }

  @Test
  void markVerified_회차_미존재면_EntityNotFound() {
    when(sessionRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> sessionService.markVerified(404L))
        .isInstanceOf(EntityNotFoundException.class);
  }
}

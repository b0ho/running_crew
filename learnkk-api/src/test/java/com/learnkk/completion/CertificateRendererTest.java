package com.learnkk.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * CertificateRenderer 단위 테스트 — PNG 바이트 생성(비어있지 않음)·한글 렌더 예외 없음(performance-design §2, FR-8).
 *
 * <p>헤드리스 Java2D 로 유효한 PNG 를 생성하는지, 한글 성명·코호트명을 렌더링해도 예외가 없는지 검증한다(폰트 폴백 경로 포함).
 */
class CertificateRendererTest {

  private final CertificateRenderer renderer = new CertificateRenderer();

  @Test
  void 한글_수료증을_렌더링해도_예외가_없고_비어있지_않은_PNG를_생성한다() throws IOException {
    byte[] png = renderer.render("김멘티", "백엔드 마스터 코호트", LocalDate.of(2026, 3, 1));

    assertThat(png).isNotEmpty();
    // 유효한 PNG 로 디코딩되는지 확인(폭/높이 > 0).
    var image = ImageIO.read(new ByteArrayInputStream(png));
    assertThat(image).isNotNull();
    assertThat(image.getWidth()).isPositive();
    assertThat(image.getHeight()).isPositive();
  }

  @Test
  void 성명이나_코호트명이_null이어도_예외없이_렌더링한다() {
    assertThatCode(() -> renderer.render(null, null, null)).doesNotThrowAnyException();
  }
}

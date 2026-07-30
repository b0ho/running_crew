package com.learnkk.completion;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 수료증 이미지 렌더러 (performance-design.md §2, security-design.md §3, FR-8).
 *
 * <p>외부 이미지 라이브러리 의존 없이 순수 Java2D({@link BufferedImage} + {@link Graphics2D})로 수료증 PNG 를 생성하고
 * {@link ImageIO}로 인코딩한다. 헤드리스 환경에서 동작한다({@code java.awt.headless=true}는 {@code
 * LearnkkApiApplication} 부팅 시 설정). 멘티 성명·코호트명·발급일만 임베드해 PII 를 최소화한다(security-design.md §3).
 *
 * <p><b>한글 폰트</b>: {@code resources/fonts/NotoSansKR-Regular.ttf}(OFL 라이선스)가 번들되어 있으면 {@link
 * Font#createFont}로 로드해 한글 글리프를 정확히 렌더링한다. 바이너리 폰트가 없는 환경에서도 실패 없이 동작하도록 논리 폰트({@code
 * Font.SANS_SERIF})로 폴백한다(폴백 시 실행 환경의 물리 폰트에 따라 한글 표시가 달라질 수 있으나 예외는 발생하지 않는다). 폰트는 최초 1회 로드 후
 * 재사용한다.
 */
@Component
public class CertificateRenderer {

  private static final Logger log = LoggerFactory.getLogger(CertificateRenderer.class);

  private static final String FONT_RESOURCE = "/fonts/NotoSansKR-Regular.ttf";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년 M월 d일");

  private static final int WIDTH = 1000;
  private static final int HEIGHT = 700;

  /** 최초 1회 로드 후 재사용하는 기본 폰트(파생 크기는 deriveFont 로 생성). */
  private final Font baseFont;

  public CertificateRenderer() {
    this.baseFont = loadBaseFont();
  }

  /**
   * 수료증 PNG 바이트를 생성한다. 멘티별 순차 호출·참조 해제로 힙 누적을 방지한다(performance-design.md §2).
   *
   * @param menteeName 수료 멘티 성명
   * @param cohortTitle 코호트명
   * @param issuedDate 발급일
   * @return PNG 인코딩된 이미지 바이트(비어있지 않음)
   */
  public byte[] render(String menteeName, String cohortTitle, LocalDate issuedDate) {
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      // 배경
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, WIDTH, HEIGHT);

      // 테두리
      g.setColor(new Color(0x1F3A5F));
      g.drawRect(24, 24, WIDTH - 49, HEIGHT - 49);
      g.drawRect(34, 34, WIDTH - 69, HEIGHT - 69);

      // 제목
      drawCentered(g, "수 료 증", deriveFont(Font.BOLD, 56f), new Color(0x1F3A5F), 180);

      // 멘티 성명
      drawCentered(g, safe(menteeName), deriveFont(Font.BOLD, 40f), Color.BLACK, 320);

      // 본문
      drawCentered(
          g,
          "위 사람은 '" + safe(cohortTitle) + "' 코호트 과정을",
          deriveFont(Font.PLAIN, 26f),
          new Color(0x333333),
          400);
      drawCentered(
          g, "성실히 이수하여 수료하였음을 증명합니다.", deriveFont(Font.PLAIN, 26f), new Color(0x333333), 445);

      // 발급일
      LocalDate date = issuedDate == null ? LocalDate.now() : issuedDate;
      drawCentered(
          g, date.format(DATE_FORMAT), deriveFont(Font.PLAIN, 24f), new Color(0x555555), 560);
      drawCentered(g, "LearnKK", deriveFont(Font.BOLD, 28f), new Color(0x1F3A5F), 610);

      return encodePng(image);
    } finally {
      g.dispose();
    }
  }

  private byte[] encodePng(BufferedImage image) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      boolean written = ImageIO.write(image, "png", out);
      if (!written) {
        throw new IllegalStateException("PNG 인코더를 찾을 수 없습니다");
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("수료증 이미지 인코딩에 실패했습니다", e);
    }
  }

  private void drawCentered(Graphics2D g, String text, Font font, Color color, int y) {
    g.setFont(font);
    g.setColor(color);
    int textWidth = g.getFontMetrics().stringWidth(text);
    int x = (WIDTH - textWidth) / 2;
    g.drawString(text, x, y);
  }

  private Font deriveFont(int style, float size) {
    return baseFont.deriveFont(style, size);
  }

  private static String safe(String value) {
    return value == null || value.isBlank() ? "-" : value.trim();
  }

  private static Font loadBaseFont() {
    try (InputStream in = CertificateRenderer.class.getResourceAsStream(FONT_RESOURCE)) {
      if (in != null) {
        return Font.createFont(Font.TRUETYPE_FONT, in);
      }
      log.warn(
          "번들 한글 폰트({})가 없어 논리 폰트(SANS_SERIF)로 폴백합니다. 한글 글리프 렌더링을 보장하려면 OFL 폰트를 추가하세요.",
          FONT_RESOURCE);
    } catch (IOException | FontFormatException e) {
      log.warn("한글 폰트 로드 실패 — 논리 폰트(SANS_SERIF)로 폴백합니다", e);
    }
    return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  }
}

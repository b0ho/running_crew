package com.learnkk.cohort.dto;

import com.learnkk.common.validation.SafeExternalUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 공지 작성 요청 (W-U2-6, R-U2-16/17).
 *
 * <p>body 필수. externalLink 는 선택이며 {@link SafeExternalUrl} 로 http/https 스킴만 허용(security-design.md
 * §3). VARCHAR(2048) 컬럼 한도에 맞춰 길이를 제한한다.
 */
public record AnnouncementCreateRequest(
    @NotBlank(message = "공지 본문은 필수입니다") String body,
    @Size(max = 2048, message = "외부 링크는 최대 2048자입니다") @SafeExternalUrl String externalLink) {}

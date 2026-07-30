package com.learnkk.auth.dto;

import com.learnkk.user.User;

/**
 * 사용자 응답 DTO (domain-entities.md §2, INV-1).
 *
 * <p>passwordHash 를 절대 포함하지 않는다. API 경계에서 JPA Entity 대신 이 DTO 만 노출한다(NFR-7).
 */
public record UserDto(Long id, String email, String name, String nickname, boolean isAdmin) {

  public static UserDto from(User user) {
    return new UserDto(
        user.getId(), user.getEmail(), user.getName(), user.getNickname(), user.isAdmin());
  }
}

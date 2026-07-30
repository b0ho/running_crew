package com.learnkk.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO (business-rules R-U1-01~07).
 *
 * <p>수집 필드는 email·name·nickname·password 4개로 제한한다(R-U1-07). <b>isAdmin 필드를 두지 않는다</b>(R-U1-06 —
 * 클라이언트가 권한을 실을 여지 자체를 제거).
 */
public record SignupRequest(
    @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        @Size(max = 254, message = "이메일은 최대 254자입니다")
        String email,
    @NotBlank(message = "이름은 필수입니다") @Size(max = 100, message = "이름은 최대 100자입니다") String name,
    @NotBlank(message = "닉네임은 필수입니다") @Size(max = 50, message = "닉네임은 최대 50자입니다") String nickname,
    @NotBlank(message = "비밀번호는 필수입니다") @Size(min = 8, message = "비밀번호는 최소 8자입니다")
        String password) {}

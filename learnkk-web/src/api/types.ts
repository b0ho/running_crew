// 백엔드 계약 DTO 타입 (OpenAPI 스펙과 동기화 — learnkk-api/springdoc)

export interface UserDto {
  id: number;
  email: string;
  name: string;
  nickname: string;
  isAdmin: boolean;
}

export interface SignupRequest {
  email: string;
  name: string;
  nickname: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// 공통 에러 응답 DTO (R-U1-17)
export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  path: string;
}

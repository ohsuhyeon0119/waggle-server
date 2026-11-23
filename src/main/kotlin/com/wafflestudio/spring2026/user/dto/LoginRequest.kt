package com.wafflestudio.spring2026.user.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "이메일은 공백일 수 없습니다.")
    val email: String,
    @field:NotBlank(message = "비밀번호는 공백일 수 없습니다.")
    val password: String,
)

data class LoginResponse(
    val accessToken: String,
)

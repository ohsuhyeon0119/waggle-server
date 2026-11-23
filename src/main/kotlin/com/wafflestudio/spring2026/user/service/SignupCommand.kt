package com.wafflestudio.spring2026.user.service

import com.wafflestudio.spring2026.user.model.Role

data class SignupCommand(
    val email: String,
    val password: String,
    val name: String,
    val githubUsername: String,
    val role: Role,
    /** 운영진이 담당할 세미나. 루키는 비어 있다. */
    val seminarId: Long?,
)

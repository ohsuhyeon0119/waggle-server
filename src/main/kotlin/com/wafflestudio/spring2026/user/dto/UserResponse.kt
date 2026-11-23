package com.wafflestudio.spring2026.user.dto

import com.wafflestudio.spring2026.common.web.toServiceTime
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import com.wafflestudio.spring2026.user.model.UserStatus
import java.time.OffsetDateTime

/** 비밀번호는 어떤 응답에도 담지 않는다. */
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val githubUsername: String,
    val role: Role,
    val status: UserStatus,
    val seminarId: Long?,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(
                id = user.requireId(),
                email = user.email,
                name = user.name,
                githubUsername = user.githubUsername,
                role = user.role,
                status = user.status,
                seminarId = user.seminarId?.id,
                createdAt = user.createdAt.toServiceTime(),
            )
    }
}

/** 가입 신청 응답. 나머지 정보는 단건 조회에서 받는다. */
data class SignupResponse(
    val id: Long,
    val status: UserStatus,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(user: User): SignupResponse =
            SignupResponse(
                id = user.requireId(),
                status = user.status,
                createdAt = user.createdAt.toServiceTime(),
            )
    }
}

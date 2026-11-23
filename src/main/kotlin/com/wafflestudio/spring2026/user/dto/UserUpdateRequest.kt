package com.wafflestudio.spring2026.user.dto

import com.wafflestudio.spring2026.common.error.InvalidRequestException
import io.swagger.v3.oas.annotations.media.Schema

/** 보낸 항목만 바꾼다. 둘 다 `null` 을 허용하지 않으므로 세 가지 상태가 필요 없다. */
data class UserUpdateRequest(
    @field:Schema(description = "이름. 보내지 않으면 그대로 둔다.")
    val name: String? = null,
    @field:Schema(description = "GitHub 사용자명. 보내지 않으면 그대로 둔다.")
    val githubUsername: String? = null,
) {
    fun validated(): UserUpdateRequest =
        also {
            if (name != null && name.isBlank()) throw InvalidRequestException("name", "이름은 공백일 수 없습니다.")
            if (githubUsername != null && githubUsername.isBlank()) {
                throw InvalidRequestException("githubUsername", "GitHub 사용자명은 공백일 수 없습니다.")
            }
        }
}

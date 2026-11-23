package com.wafflestudio.spring2026.user.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.service.SignupCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SignupRequest(
    @field:NotBlank(message = "이메일은 공백일 수 없습니다.")
    @field:Email(message = "이메일 형식이 아닙니다.")
    val email: String,
    @field:NotBlank(message = "비밀번호는 공백일 수 없습니다.")
    val password: String,
    @field:NotBlank(message = "이름은 공백일 수 없습니다.")
    val name: String,
    @field:NotBlank(message = "GitHub 사용자명은 공백일 수 없습니다.")
    val githubUsername: String,
    @field:Schema(description = "가입할 역할. 와장(ADMIN)으로는 가입할 수 없다.")
    val role: SignupRole,
    @field:Schema(description = "운영진이 담당할 세미나. 루키는 보내지 않는다.")
    val seminarId: Long? = null,
) {
    @get:AssertTrue(message = "운영진은 담당 세미나를 하나 골라야 하고, 루키는 고를 수 없습니다.")
    @get:JsonIgnore
    @get:Schema(hidden = true)
    val seminarSelectionValid: Boolean
        get() =
            when (role) {
                SignupRole.STAFF -> seminarId != null
                SignupRole.ROOKIE -> seminarId == null
            }

    fun toCommand(): SignupCommand =
        SignupCommand(
            email = email,
            password = password,
            name = name,
            githubUsername = githubUsername,
            role = role.toRole(),
            seminarId = seminarId,
        )
}

/** 가입으로 만들 수 있는 역할. [Role] 과 달리 `ADMIN` 이 없어서 잘못된 요청이 역직렬화 단계에서 걸러진다. */
enum class SignupRole {
    STAFF,
    ROOKIE,
    ;

    fun toRole(): Role =
        when (this) {
            STAFF -> Role.STAFF
            ROOKIE -> Role.ROOKIE
        }
}

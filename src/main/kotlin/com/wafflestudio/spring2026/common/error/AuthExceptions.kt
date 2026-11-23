package com.wafflestudio.spring2026.common.error

import org.springframework.http.HttpStatus

/** 누구인지는 알지만 이 기능을 쓸 자격이 없다. */
class ForbiddenException(
    message: String,
    code: String = "FORBIDDEN",
) : ApiException(
        status = HttpStatus.FORBIDDEN,
        code = code,
        message = message,
    )

class InvalidCredentialsException :
    ApiException(
        status = HttpStatus.UNAUTHORIZED,
        code = "INVALID_CREDENTIALS",
        message = "이메일 또는 비밀번호가 맞지 않습니다.",
    )

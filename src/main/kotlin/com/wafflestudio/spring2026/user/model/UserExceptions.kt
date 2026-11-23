package com.wafflestudio.spring2026.user.model

import com.wafflestudio.spring2026.common.error.ApiException
import org.springframework.http.HttpStatus

class UserNotFoundException(
    userId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "USER_NOT_FOUND",
        message = "ID가 ${userId}인 사용자를 찾을 수 없습니다.",
    )

class EmailAlreadyExistsException(
    email: String,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "EMAIL_ALREADY_EXISTS",
        message = "이미 ${email}로 가입한 사용자가 있습니다.",
    )

class UserNotPendingException(
    userId: Long,
    status: UserStatus,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "USER_NOT_PENDING",
        message = "ID가 ${userId}인 사용자는 이미 심사가 끝났습니다. (현재 상태: $status)",
    )

class UserNotApprovedException(
    userId: Long,
    status: UserStatus,
) : ApiException(
        status = HttpStatus.FORBIDDEN,
        code = "USER_NOT_APPROVED",
        message = "ID가 ${userId}인 사용자는 가입 승인을 받지 못했습니다. (현재 상태: $status)",
    )

class NotRookieException(
    userId: Long,
    role: Role,
) : ApiException(
        status = HttpStatus.BAD_REQUEST,
        code = "INVALID_REQUEST",
        message = "ID가 ${userId}인 사용자는 루키가 아닙니다. (현재 역할: $role)",
    )

class AdminCannotWithdrawException :
    ApiException(
        status = HttpStatus.CONFLICT,
        code = "ADMIN_CANNOT_WITHDRAW",
        message = "와장은 탈퇴할 수 없습니다.",
    )

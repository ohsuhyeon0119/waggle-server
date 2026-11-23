package com.wafflestudio.spring2026.common.auth

import com.wafflestudio.spring2026.common.error.ForbiddenException
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import com.wafflestudio.spring2026.user.model.UserStatus

/**
 * 역할과 심사 상태로 갈리는 인가 규칙. docs/권한.md 를 코드로 옮긴 것이다.
 * 자원별 규칙(담당 세미나인가, 수강 중인가)은 각 서비스가 이어서 확인한다.
 */

/** 승인되지 않은 사용자는 자기 정보를 보는 것 말고는 아무것도 할 수 없다. */
fun User.requireApproved(): User =
    also {
        if (status != UserStatus.APPROVED) {
            throw ForbiddenException(
                code = "USER_NOT_APPROVED",
                message = "가입 승인을 받아야 쓸 수 있는 기능입니다. (현재 상태: $status)",
            )
        }
    }

fun User.requireRole(vararg allowed: Role): User =
    requireApproved().also {
        if (role !in allowed) {
            throw ForbiddenException("이 기능을 쓸 수 있는 역할이 아닙니다. (현재 역할: $role)")
        }
    }

/** 운영진이라면 담당 세미나인지까지 확인한다. 와장은 모든 세미나를 다룰 수 있다. */
fun User.requireManages(seminarId: Long): User =
    requireRole(Role.ADMIN, Role.STAFF).also {
        if (role == Role.STAFF && this.seminarId?.id != seminarId) {
            throw ForbiddenException("담당 세미나가 아닙니다.")
        }
    }

package com.wafflestudio.spring2026.session.model

import com.wafflestudio.spring2026.common.error.ApiException
import org.springframework.http.HttpStatus

class SessionNotFoundException(
    sessionId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "SESSION_NOT_FOUND",
        message = "ID가 ${sessionId}인 회차를 찾을 수 없습니다.",
    )

class NotLastSessionException(
    sessionId: Long,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "NOT_LAST_SESSION",
        message = "ID가 ${sessionId}인 회차는 마지막 회차가 아닙니다. 마지막 회차부터 지워야 합니다.",
    )

class SessionHasRecordsException(
    sessionId: Long,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "SESSION_HAS_RECORDS",
        message = "ID가 ${sessionId}인 회차에는 출석이나 과제 기록이 있어 지울 수 없습니다.",
    )

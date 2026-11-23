package com.wafflestudio.spring2026.dashboard.model

import com.wafflestudio.spring2026.common.error.ApiException
import org.springframework.http.HttpStatus

/** 회차와 수강 신청이 서로 다른 세미나의 것이면 그 회차의 표에는 없는 수강생이다. */
class RecordOutsideSeminarException(
    sessionId: Long,
    enrollmentId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "ENROLLMENT_NOT_FOUND",
        message = "회차 ${sessionId}의 세미나에 ID가 ${enrollmentId}인 수강 신청이 없습니다.",
    )

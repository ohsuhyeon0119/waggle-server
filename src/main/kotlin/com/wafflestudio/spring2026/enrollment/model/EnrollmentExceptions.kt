package com.wafflestudio.spring2026.enrollment.model

import com.wafflestudio.spring2026.common.error.ApiException
import com.wafflestudio.spring2026.common.error.FieldErrorResponse
import org.springframework.http.HttpStatus

class EnrollmentNotFoundException(
    enrollmentId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "ENROLLMENT_NOT_FOUND",
        message = "ID가 ${enrollmentId}인 수강 신청을 찾을 수 없습니다.",
    )

class NotEnrolledException(
    seminarId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "ENROLLMENT_NOT_FOUND",
        message = "ID가 ${seminarId}인 세미나에 신청한 기록이 없습니다.",
    )

class AlreadyEnrolledException(
    seminarId: Long,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "ALREADY_ENROLLED",
        message = "ID가 ${seminarId}인 세미나에 이미 신청했습니다.",
    )

class SeminarFullException(
    seminarId: Long,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "SEMINAR_FULL",
        message = "ID가 ${seminarId}인 세미나는 정원이 찼습니다.",
    )

class SeminarNotOpenException(
    seminarId: Long,
) : ApiException(
        status = HttpStatus.CONFLICT,
        code = "SEMINAR_NOT_OPEN",
        message = "ID가 ${seminarId}인 세미나는 지금 신청하거나 취소할 수 없습니다.",
    )

class GraceDaysOutOfRangeException(
    requested: Int,
    totalGraceDays: Int,
) : ApiException(
        status = HttpStatus.BAD_REQUEST,
        code = "INVALID_REQUEST",
        message = "요청값이 올바르지 않습니다.",
        fieldErrors =
            listOf(
                FieldErrorResponse("graceDaysRemaining", "0에서 $totalGraceDays 사이여야 합니다. (받은 값: $requested)"),
            ),
    )

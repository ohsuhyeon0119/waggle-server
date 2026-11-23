package com.wafflestudio.spring2026.seminar.model

import com.wafflestudio.spring2026.common.error.ApiException
import org.springframework.http.HttpStatus

class SeminarNotFoundException(
    seminarId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "SEMINAR_NOT_FOUND",
        message = "ID가 ${seminarId}인 세미나를 찾을 수 없습니다.",
    )

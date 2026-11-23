package com.wafflestudio.spring2026.meeting

import com.wafflestudio.spring2026.common.error.ApiException
import org.springframework.http.HttpStatus

class MeetingNotFoundException(
    val meetingId: Long,
) : ApiException(
        status = HttpStatus.NOT_FOUND,
        code = "MEETING_NOT_FOUND",
        message = "ID가 ${meetingId}인 모임을 찾을 수 없습니다.",
    )

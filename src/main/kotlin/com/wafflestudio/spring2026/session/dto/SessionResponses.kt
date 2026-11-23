package com.wafflestudio.spring2026.session.dto

import com.wafflestudio.spring2026.common.web.toServiceTime
import com.wafflestudio.spring2026.session.model.RoundedSession
import java.time.OffsetDateTime

/** 목록용. 본문이 길 수 있는 수업·과제 설명은 담지 않는다. */
data class SessionSummaryResponse(
    val id: Long,
    val seminarId: Long,
    val round: Int,
    val title: String,
    val startsAt: OffsetDateTime,
    val location: String,
    val assignmentTitle: String,
) {
    companion object {
        fun from(rounded: RoundedSession): SessionSummaryResponse =
            with(rounded.session) {
                SessionSummaryResponse(
                    id = requireId(),
                    seminarId = requireSeminarId(),
                    round = rounded.round,
                    title = title,
                    startsAt = startsAt.toServiceTime(),
                    location = location,
                    assignmentTitle = assignmentTitle,
                )
            }
    }
}

/** 단건 조회용. 목록 항목에 수업·과제 설명을 더한 형태다. */
data class SessionDetailResponse(
    val id: Long,
    val seminarId: Long,
    val round: Int,
    val title: String,
    val startsAt: OffsetDateTime,
    val location: String,
    val assignmentTitle: String,
    val lectureContent: String?,
    val assignmentContent: String?,
) {
    companion object {
        fun from(rounded: RoundedSession): SessionDetailResponse =
            with(rounded.session) {
                SessionDetailResponse(
                    id = requireId(),
                    seminarId = requireSeminarId(),
                    round = rounded.round,
                    title = title,
                    startsAt = startsAt.toServiceTime(),
                    location = location,
                    assignmentTitle = assignmentTitle,
                    lectureContent = lectureContent,
                    assignmentContent = assignmentContent,
                )
            }
    }
}

/** 회차 등록 응답. 나머지 정보는 단건 조회에서 받는다. */
data class SessionCreateResponse(
    val id: Long,
    val seminarId: Long,
    val round: Int,
) {
    companion object {
        fun from(rounded: RoundedSession): SessionCreateResponse =
            SessionCreateResponse(
                id = rounded.session.requireId(),
                seminarId = rounded.session.requireSeminarId(),
                round = rounded.round,
            )
    }
}

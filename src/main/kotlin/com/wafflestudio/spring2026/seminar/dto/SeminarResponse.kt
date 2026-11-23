package com.wafflestudio.spring2026.seminar.dto

import com.wafflestudio.spring2026.common.web.toServiceTime
import com.wafflestudio.spring2026.seminar.model.SeminarDetail
import com.wafflestudio.spring2026.seminar.model.SeminarStatus
import java.time.OffsetDateTime

data class SeminarResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val capacity: Int,
    val enrolledCount: Int,
    val applyStartAt: OffsetDateTime,
    val applyEndAt: OffsetDateTime,
    val totalGraceDays: Int,
    val status: SeminarStatus,
    val sessionCount: Int,
) {
    companion object {
        fun from(detail: SeminarDetail): SeminarResponse {
            val seminar = detail.seminar

            return SeminarResponse(
                id = seminar.requireId(),
                title = seminar.title,
                description = seminar.description,
                capacity = seminar.capacity,
                enrolledCount = detail.enrolledCount,
                applyStartAt = seminar.applyStartAt.toServiceTime(),
                applyEndAt = seminar.applyEndAt.toServiceTime(),
                totalGraceDays = seminar.totalGraceDays,
                status = detail.status,
                sessionCount = detail.sessionCount,
            )
        }
    }
}

/** 세미나 개설 응답. 나머지 정보는 단건 조회에서 받는다. */
data class SeminarCreateResponse(
    val id: Long,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(detail: SeminarDetail): SeminarCreateResponse =
            SeminarCreateResponse(
                id = detail.seminar.requireId(),
                createdAt = detail.seminar.createdAt.toServiceTime(),
            )
    }
}

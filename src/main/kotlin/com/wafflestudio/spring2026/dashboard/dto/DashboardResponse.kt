package com.wafflestudio.spring2026.dashboard.dto

import com.wafflestudio.spring2026.common.web.toServiceTime
import com.wafflestudio.spring2026.dashboard.model.AttendanceStatus
import com.wafflestudio.spring2026.dashboard.model.Dashboard
import com.wafflestudio.spring2026.dashboard.model.DashboardRow
import com.wafflestudio.spring2026.dashboard.model.SubmissionStatus
import com.wafflestudio.spring2026.session.model.RoundedSession
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

/** 4주차 과제 문서의 `GET /seminars/{seminarId}/dashboard` 응답. 페이지 정보는 `content` 에만 해당한다. */
data class DashboardResponse(
    val seminarId: Long,
    @field:Schema(description = "세미나의 전체 회차. 페이지와 관계없이 같다.")
    val sessions: List<DashboardSessionResponse>,
    @field:Schema(description = "이번 페이지의 수강생. 수강 신청 ID 오름차순.")
    val content: List<DashboardRowResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(dashboard: Dashboard): DashboardResponse =
            DashboardResponse(
                seminarId = dashboard.seminarId,
                sessions = dashboard.sessions.map(DashboardSessionResponse::from),
                content = dashboard.rows.content.map(DashboardRowResponse::from),
                page = dashboard.rows.page,
                size = dashboard.rows.size,
                totalElements = dashboard.rows.totalElements,
                totalPages = dashboard.rows.totalPages,
            )
    }
}

data class DashboardSessionResponse(
    val id: Long,
    val round: Int,
    val title: String,
    val startsAt: OffsetDateTime,
    val assignmentTitle: String,
) {
    companion object {
        fun from(rounded: RoundedSession): DashboardSessionResponse =
            DashboardSessionResponse(
                id = rounded.session.requireId(),
                round = rounded.round,
                title = rounded.session.title,
                startsAt = rounded.session.startsAt.toServiceTime(),
                assignmentTitle = rounded.session.assignmentTitle,
            )
    }
}

data class DashboardRowResponse(
    @field:Schema(description = "수강 신청 ID. 사용자 ID 와 다르다.")
    val id: Long,
    val user: DashboardUserResponse,
    val graceDaysRemaining: Int,
    val dropped: Boolean,
    val createdAt: OffsetDateTime,
    val records: List<RecordCellResponse>,
) {
    companion object {
        fun from(row: DashboardRow): DashboardRowResponse =
            DashboardRowResponse(
                id = row.enrollment.requireId(),
                user =
                    DashboardUserResponse(
                        id = row.rookie.requireId(),
                        email = row.rookie.email,
                        name = row.rookie.name,
                        githubUsername = row.rookie.githubUsername,
                    ),
                graceDaysRemaining = row.enrollment.graceDaysRemaining,
                dropped = row.enrollment.dropped,
                createdAt = row.enrollment.createdAt.toServiceTime(),
                records = row.records.map { RecordCellResponse(it.sessionId, it.attendance, it.submission) },
            )
    }
}

data class DashboardUserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val githubUsername: String,
)

/** 아직 기록하지 않은 칸은 `null` 이다. 결석·미제출과 구분하며, 프로퍼티를 생략하지 않는다. */
data class RecordCellResponse(
    val sessionId: Long,
    @field:Schema(nullable = true)
    val attendance: AttendanceStatus?,
    @field:Schema(nullable = true)
    val assignmentStatus: SubmissionStatus?,
)

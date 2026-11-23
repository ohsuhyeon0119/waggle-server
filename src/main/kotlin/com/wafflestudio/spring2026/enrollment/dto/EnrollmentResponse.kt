package com.wafflestudio.spring2026.enrollment.dto

import com.wafflestudio.spring2026.common.web.toServiceTime
import com.wafflestudio.spring2026.enrollment.model.Enrollment
import com.wafflestudio.spring2026.enrollment.service.MyEnrollment
import com.wafflestudio.spring2026.seminar.model.SeminarStatus
import java.time.OffsetDateTime

/** 수강 신청 응답. `graceDaysRemaining` 은 세미나의 `totalGraceDays` 에서 시작한다. */
data class EnrollmentCreateResponse(
    val id: Long,
    val graceDaysRemaining: Int,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(enrollment: Enrollment): EnrollmentCreateResponse =
            EnrollmentCreateResponse(
                id = enrollment.requireId(),
                graceDaysRemaining = enrollment.graceDaysRemaining,
                createdAt = enrollment.createdAt.toServiceTime(),
            )
    }
}

/** 수강 정보 수정 응답. 신청 ID 와 신청 일시는 바뀌지 않는다. */
data class EnrollmentUpdateResponse(
    val id: Long,
    val graceDaysRemaining: Int,
    val dropped: Boolean,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(enrollment: Enrollment): EnrollmentUpdateResponse =
            EnrollmentUpdateResponse(
                id = enrollment.requireId(),
                graceDaysRemaining = enrollment.graceDaysRemaining,
                dropped = enrollment.dropped,
                createdAt = enrollment.createdAt.toServiceTime(),
            )
    }
}

/** 내 수강 목록의 한 줄. 세미나 요약이 함께 붙는다. */
data class MyEnrollmentResponse(
    val id: Long,
    val seminar: EnrolledSeminarResponse,
    val graceDaysRemaining: Int,
    val dropped: Boolean,
    val createdAt: OffsetDateTime,
) {
    companion object {
        fun from(mine: MyEnrollment): MyEnrollmentResponse =
            MyEnrollmentResponse(
                id = mine.enrollment.requireId(),
                seminar =
                    EnrolledSeminarResponse(
                        id = mine.seminar.seminar.requireId(),
                        title = mine.seminar.seminar.title,
                        description = mine.seminar.seminar.description,
                        status = mine.seminar.status,
                        capacity = mine.seminar.seminar.capacity,
                        enrolledCount = mine.seminar.enrolledCount,
                        applyStartAt = mine.seminar.seminar.applyStartAt.toServiceTime(),
                        applyEndAt = mine.seminar.seminar.applyEndAt.toServiceTime(),
                    ),
                graceDaysRemaining = mine.enrollment.graceDaysRemaining,
                dropped = mine.enrollment.dropped,
                createdAt = mine.enrollment.createdAt.toServiceTime(),
            )
    }
}

/**
 * 신청 기간을 함께 내려 준다. 취소가 가능한지는 `status` 가 아니라 기간으로 판단하기 때문이다.
 * 정원이 차서 `CLOSED` 인 세미나도 기간 안이라면 취소할 수 있다.
 */
data class EnrolledSeminarResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val status: SeminarStatus,
    val capacity: Int,
    val enrolledCount: Int,
    val applyStartAt: OffsetDateTime,
    val applyEndAt: OffsetDateTime,
)

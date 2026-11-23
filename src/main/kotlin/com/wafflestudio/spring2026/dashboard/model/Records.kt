package com.wafflestudio.spring2026.dashboard.model

import com.wafflestudio.spring2026.enrollment.model.Enrollment
import com.wafflestudio.spring2026.session.model.SeminarSession
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
}

enum class SubmissionStatus {
    PASSED,
    SUBMITTED_BUT_FAILED,
    NOT_SUBMITTED,
}

/**
 * "어떤 회차의, 어떤 수강생"에 대한 출석 기록. 한 칸에 하나뿐이다.
 * 아직 기록하지 않은 칸은 행이 없는 상태로 남는다. 결석으로 간주하지 않는다.
 */
@Table("attendances")
data class AttendanceRecord(
    @Id
    val id: Long? = null,
    val sessionId: AggregateReference<SeminarSession, Long>,
    val enrollmentId: AggregateReference<Enrollment, Long>,
    val status: AttendanceStatus,
    @CreatedDate
    val createdAt: Instant = Instant.EPOCH,
    @LastModifiedDate
    val updatedAt: Instant = Instant.EPOCH,
)

/** 출석 기록과 같은 구조의 과제 제출 기록. */
@Table("assignment_submissions")
data class SubmissionRecord(
    @Id
    val id: Long? = null,
    val sessionId: AggregateReference<SeminarSession, Long>,
    val enrollmentId: AggregateReference<Enrollment, Long>,
    val status: SubmissionStatus,
    @CreatedDate
    val createdAt: Instant = Instant.EPOCH,
    @LastModifiedDate
    val updatedAt: Instant = Instant.EPOCH,
)

/** 대시보드 한 칸. 두 기록을 회차 기준으로 합쳐 놓은 읽기 전용 값이다. */
data class RecordCell(
    val sessionId: Long,
    val attendance: AttendanceStatus?,
    val submission: SubmissionStatus?,
)

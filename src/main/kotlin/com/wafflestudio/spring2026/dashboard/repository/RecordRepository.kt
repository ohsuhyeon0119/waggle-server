package com.wafflestudio.spring2026.dashboard.repository

import com.wafflestudio.spring2026.dashboard.model.AttendanceRecord
import com.wafflestudio.spring2026.dashboard.model.SubmissionRecord
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component

interface AttendanceRepository : CrudRepository<AttendanceRecord, Long> {
    @Query("SELECT * FROM attendances WHERE enrollment_id IN (:enrollmentIds)")
    fun findAllByEnrollmentIds(
        @Param("enrollmentIds") enrollmentIds: Collection<Long>,
    ): List<AttendanceRecord>

    @Query("SELECT * FROM attendances WHERE session_id = :sessionId AND enrollment_id IN (:enrollmentIds)")
    fun findAllBySessionIdAndEnrollmentIds(
        @Param("sessionId") sessionId: Long,
        @Param("enrollmentIds") enrollmentIds: Collection<Long>,
    ): List<AttendanceRecord>

    @Query("SELECT COUNT(*) > 0 FROM attendances WHERE session_id = :sessionId")
    fun existsBySessionId(
        @Param("sessionId") sessionId: Long,
    ): Boolean

    @Modifying
    @Query("DELETE FROM attendances WHERE enrollment_id = :enrollmentId")
    fun deleteAllByEnrollmentId(
        @Param("enrollmentId") enrollmentId: Long,
    ): Int
}

interface SubmissionRepository : CrudRepository<SubmissionRecord, Long> {
    @Query("SELECT * FROM assignment_submissions WHERE enrollment_id IN (:enrollmentIds)")
    fun findAllByEnrollmentIds(
        @Param("enrollmentIds") enrollmentIds: Collection<Long>,
    ): List<SubmissionRecord>

    @Query("SELECT * FROM assignment_submissions WHERE session_id = :sessionId AND enrollment_id IN (:enrollmentIds)")
    fun findAllBySessionIdAndEnrollmentIds(
        @Param("sessionId") sessionId: Long,
        @Param("enrollmentIds") enrollmentIds: Collection<Long>,
    ): List<SubmissionRecord>

    @Query("SELECT COUNT(*) > 0 FROM assignment_submissions WHERE session_id = :sessionId")
    fun existsBySessionId(
        @Param("sessionId") sessionId: Long,
    ): Boolean

    @Modifying
    @Query("DELETE FROM assignment_submissions WHERE enrollment_id = :enrollmentId")
    fun deleteAllByEnrollmentId(
        @Param("enrollmentId") enrollmentId: Long,
    ): Int
}

/** 출석과 과제 기록을 함께 봐야 하는 자리(회차 삭제, 수강 취소)를 위한 얇은 묶음. */
@Component
class RecordRepository(
    private val attendances: AttendanceRepository,
    private val submissions: SubmissionRepository,
) {
    fun hasAnyRecord(sessionId: Long): Boolean =
        attendances.existsBySessionId(sessionId) || submissions.existsBySessionId(sessionId)

    /** 수강 신청 하나에 딸린 출석·과제 기록을 모두 지운다. 다른 수강생의 기록과 회차는 그대로다. */
    fun deleteAllByEnrollmentId(enrollmentId: Long) {
        attendances.deleteAllByEnrollmentId(enrollmentId)
        submissions.deleteAllByEnrollmentId(enrollmentId)
    }
}

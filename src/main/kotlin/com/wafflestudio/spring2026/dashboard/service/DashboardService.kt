package com.wafflestudio.spring2026.dashboard.service

import com.wafflestudio.spring2026.common.auth.SeminarViewerPolicy
import com.wafflestudio.spring2026.common.auth.requireManages
import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.common.web.PatchField
import com.wafflestudio.spring2026.dashboard.model.AttendanceRecord
import com.wafflestudio.spring2026.dashboard.model.AttendanceStatus
import com.wafflestudio.spring2026.dashboard.model.Dashboard
import com.wafflestudio.spring2026.dashboard.model.DashboardRow
import com.wafflestudio.spring2026.dashboard.model.RecordCell
import com.wafflestudio.spring2026.dashboard.model.RecordOutsideSeminarException
import com.wafflestudio.spring2026.dashboard.model.SubmissionRecord
import com.wafflestudio.spring2026.dashboard.model.SubmissionStatus
import com.wafflestudio.spring2026.dashboard.repository.AttendanceRepository
import com.wafflestudio.spring2026.dashboard.repository.SubmissionRepository
import com.wafflestudio.spring2026.enrollment.model.Enrollment
import com.wafflestudio.spring2026.enrollment.model.EnrollmentNotFoundException
import com.wafflestudio.spring2026.enrollment.repository.EnrollmentRepository
import com.wafflestudio.spring2026.seminar.service.SeminarService
import com.wafflestudio.spring2026.session.model.SessionNotFoundException
import com.wafflestudio.spring2026.session.repository.SeminarSessionRepository
import com.wafflestudio.spring2026.session.service.SeminarSessionService
import com.wafflestudio.spring2026.user.model.User
import com.wafflestudio.spring2026.user.repository.UserRepository
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/** 일괄 기록 요청의 한 항목. 생략한 칸은 [PatchField.Absent], `null` 을 보낸 칸은 [PatchField.Cleared] 다. */
data class RecordChange(
    val enrollmentId: Long,
    val attendance: PatchField<AttendanceStatus>,
    val assignmentStatus: PatchField<SubmissionStatus>,
)

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val enrollmentRepository: EnrollmentRepository,
    private val userRepository: UserRepository,
    private val sessionRepository: SeminarSessionRepository,
    private val sessionService: SeminarSessionService,
    private val seminarService: SeminarService,
    private val attendanceRepository: AttendanceRepository,
    private val submissionRepository: SubmissionRepository,
    private val seminarViewerPolicy: SeminarViewerPolicy,
) {
    /**
     * 현황 조회. 세미나의 수강생 목록을 겸한다.
     * 와장은 모든 세미나, 운영진은 담당 세미나, 루키는 수강 중인 세미나를 볼 수 있다.
     *
     * 읽기 전용 트랜잭션 하나에서 회차, 행, 전체 수를 읽으므로 같은 시점의 값이 함께 나간다.
     */
    fun getDashboard(
        viewer: User,
        seminarId: Long,
        page: PageRequest,
    ): Dashboard {
        seminarService.getSeminar(seminarId)
        seminarViewerPolicy.requireCanView(viewer, seminarId)

        val sessions = sessionService.listBySeminar(seminarId)
        val enrollments = enrollmentRepository.findPageBySeminarId(seminarId, page.size, page.offset)
        val rookies =
            userRepository
                .findAllById(enrollments.map { it.requireRookieId() })
                .associateBy { it.requireId() }
        val cells = cellsOf(enrollments, sessions.map { it.session.requireId() })

        val rows =
            enrollments.map { enrollment ->
                DashboardRow(
                    enrollment = enrollment,
                    rookie = rookies.getValue(enrollment.requireRookieId()),
                    records = cells.getValue(enrollment.requireId()),
                )
            }

        return Dashboard(
            seminarId = seminarId,
            sessions = sessions,
            rows = PageResponse.of(rows, page, enrollmentRepository.countBySeminarId(seminarId).toLong()),
        )
    }

    /**
     * 한 회차에서 여러 수강생의 출석·과제를 한 번에 고친다. 와장과 담당 운영진만 할 수 있다.
     *
     * 요청 하나가 하나의 작업이다. 항목 하나라도 실패하면 앞서 쓴 기록까지 모두 되돌린다.
     * 업무 예외([com.wafflestudio.spring2026.common.error.ApiException])와 DB 예외는 모두 RuntimeException 이라
     * Spring 의 기본 규칙대로 롤백된다.
     *
     * 잠금 순서: 회차(읽기 잠금) → 수강 신청(ID 오름차순 쓰기 잠금).
     * - 회차 읽기 잠금: 기록하는 동안 회차가 지워지지 않는다. 같은 회차의 기록끼리는 서로 막지 않는다.
     * - 수강 신청 쓰기 잠금: 같은 수강생의 기록을 동시에 고치거나, 취소가 기록을 지우는 중에 끼어들지 않는다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun updateRecords(
        recorder: User,
        sessionId: Long,
        changes: List<RecordChange>,
    ) {
        val session = sessionRepository.findByIdForShare(sessionId) ?: throw SessionNotFoundException(sessionId)
        val seminarId = session.requireSeminarId()
        recorder.requireManages(seminarId)

        val requestedIds = changes.map { it.enrollmentId }
        val locked = enrollmentRepository.findAllByIdsForUpdate(requestedIds).associateBy { it.requireId() }
        for (enrollmentId in requestedIds) {
            val enrollment = locked[enrollmentId] ?: throw EnrollmentNotFoundException(enrollmentId)
            if (enrollment.requireSeminarId() != seminarId) {
                throw RecordOutsideSeminarException(sessionId, enrollmentId)
            }
        }

        val attendances =
            attendanceRepository
                .findAllBySessionIdAndEnrollmentIds(sessionId, requestedIds)
                .associateBy { it.enrollmentId.id }
        val submissions =
            submissionRepository
                .findAllBySessionIdAndEnrollmentIds(sessionId, requestedIds)
                .associateBy { it.enrollmentId.id }

        for (change in changes.sortedBy { it.enrollmentId }) {
            writeAttendance(sessionId, change.enrollmentId, change.attendance, attendances[change.enrollmentId])
            writeSubmission(sessionId, change.enrollmentId, change.assignmentStatus, submissions[change.enrollmentId])
        }
    }

    /** 처음이면 저장하고, 있으면 고치고, `null` 이면 지운다. 같은 값이면 아무것도 하지 않는다. */
    private fun writeAttendance(
        sessionId: Long,
        enrollmentId: Long,
        change: PatchField<AttendanceStatus>,
        existing: AttendanceRecord?,
    ) {
        when (change) {
            PatchField.Absent -> Unit
            PatchField.Cleared -> existing?.let(attendanceRepository::delete)
            is PatchField.Updated ->
                when {
                    existing == null ->
                        attendanceRepository.save(
                            AttendanceRecord(
                                sessionId = AggregateReference.to(sessionId),
                                enrollmentId = AggregateReference.to(enrollmentId),
                                status = change.value,
                            ),
                        )
                    existing.status != change.value -> attendanceRepository.save(existing.copy(status = change.value))
                    else -> Unit
                }
        }
    }

    private fun writeSubmission(
        sessionId: Long,
        enrollmentId: Long,
        change: PatchField<SubmissionStatus>,
        existing: SubmissionRecord?,
    ) {
        when (change) {
            PatchField.Absent -> Unit
            PatchField.Cleared -> existing?.let(submissionRepository::delete)
            is PatchField.Updated ->
                when {
                    existing == null ->
                        submissionRepository.save(
                            SubmissionRecord(
                                sessionId = AggregateReference.to(sessionId),
                                enrollmentId = AggregateReference.to(enrollmentId),
                                status = change.value,
                            ),
                        )
                    existing.status != change.value -> submissionRepository.save(existing.copy(status = change.value))
                    else -> Unit
                }
        }
    }

    /** 수강생마다 회차 수만큼 칸이 생긴다. 기록 두 종류를 한 번씩만 읽어 와 맞춘다. */
    private fun cellsOf(
        enrollments: List<Enrollment>,
        sessionIds: List<Long>,
    ): Map<Long, List<RecordCell>> {
        if (enrollments.isEmpty()) return emptyMap()

        val enrollmentIds = enrollments.map { it.requireId() }
        val attendances =
            attendanceRepository
                .findAllByEnrollmentIds(enrollmentIds)
                .associate { (it.enrollmentId.id to it.sessionId.id) to it.status }
        val submissions =
            submissionRepository
                .findAllByEnrollmentIds(enrollmentIds)
                .associate { (it.enrollmentId.id to it.sessionId.id) to it.status }

        return enrollmentIds.associateWith { enrollmentId ->
            sessionIds.map { sessionId ->
                RecordCell(
                    sessionId = sessionId,
                    attendance = attendances[enrollmentId to sessionId],
                    submission = submissions[enrollmentId to sessionId],
                )
            }
        }
    }
}

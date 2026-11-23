package com.wafflestudio.spring2026.enrollment.service

import com.wafflestudio.spring2026.common.auth.requireManages
import com.wafflestudio.spring2026.common.auth.requireRole
import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.dashboard.repository.RecordRepository
import com.wafflestudio.spring2026.enrollment.model.AlreadyEnrolledException
import com.wafflestudio.spring2026.enrollment.model.Enrollment
import com.wafflestudio.spring2026.enrollment.model.EnrollmentNotFoundException
import com.wafflestudio.spring2026.enrollment.model.NotEnrolledException
import com.wafflestudio.spring2026.enrollment.model.SeminarFullException
import com.wafflestudio.spring2026.enrollment.model.SeminarNotOpenException
import com.wafflestudio.spring2026.enrollment.repository.EnrollmentRepository
import com.wafflestudio.spring2026.seminar.model.Seminar
import com.wafflestudio.spring2026.seminar.model.SeminarNotFoundException
import com.wafflestudio.spring2026.seminar.model.SeminarStatus
import com.wafflestudio.spring2026.seminar.repository.SeminarRepository
import com.wafflestudio.spring2026.seminar.service.SeminarService
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import java.time.Clock
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

/**
 * 수강 신청·취소와 수강 정보 수정.
 *
 * 동시성은 비관적 잠금으로 다룬다. (docs/4주차 설계.md)
 * - 한 세미나의 신청과 취소는 세미나 행을 잠그고 하나씩 실행한다. 정원 확인과 저장 사이에 다른 요청이 끼어들 수 없다.
 * - 수강 신청 행 하나를 고치거나 지우는 작업(수정, 출석·과제 기록, 취소)은 그 행을 잠근다.
 *
 * 잠그는 메서드는 READ COMMITTED 로 실행한다. MySQL 기본값인 REPEATABLE READ 에서는 잠금을 기다린 뒤에도
 * 일반 SELECT 가 트랜잭션 첫 조회 시점의 스냅샷을 볼 수 있고, 범위 조건의 DELETE 가 gap lock 을 걸어
 * 교착 상태가 생길 여지가 커진다. 순서는 잠금이 정하므로 각 조회는 그때까지 커밋된 값을 보면 된다.
 */
@Service
@Transactional(readOnly = true)
class EnrollmentService(
    private val enrollmentRepository: EnrollmentRepository,
    private val seminarRepository: SeminarRepository,
    private val seminarService: SeminarService,
    private val recordRepository: RecordRepository,
    private val clock: Clock,
) {
    /**
     * 선착순이므로 별도 승인 없이 즉시 확정된다. 신청할 수 있는 세미나 개수에는 제한이 없다.
     *
     * 세미나 행을 잠근 뒤 "이미 신청했는가, 자리가 남았는가"를 보고 저장한다. 잠금이 없으면 두 요청이 같은
     * 인원 수를 읽고 둘 다 저장해 정원을 넘길 수 있다. `UNIQUE(seminar_id, rookie_id)` 는 마지막 방어선이다.
     * 이 경로로는 위반되지 않으므로, 위반된다면 버그로 보고 409 로 감추지 않는다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun enroll(
        rookie: User,
        seminarId: Long,
    ): Enrollment {
        rookie.requireRole(Role.ROOKIE)

        val seminar = lockSeminar(seminarId)
        val rookieId = rookie.requireId()

        if (enrollmentRepository.existsBySeminarIdAndRookieId(seminarId, rookieId)) {
            throw AlreadyEnrolledException(seminarId)
        }

        val enrolledCount = enrollmentRepository.countBySeminarId(seminarId)
        when (seminar.statusAt(clock.instant(), enrolledCount)) {
            SeminarStatus.OPEN -> Unit
            SeminarStatus.BEFORE -> throw SeminarNotOpenException(seminarId)
            // 기간 안인데 CLOSED 라면 이유는 정원 마감뿐이다.
            SeminarStatus.CLOSED ->
                if (seminar.isApplyPeriod(clock.instant())) {
                    throw SeminarFullException(seminarId)
                } else {
                    throw SeminarNotOpenException(seminarId)
                }
        }

        return enrollmentRepository.save(
            Enrollment.of(seminarId = seminarId, rookieId = rookieId, totalGraceDays = seminar.totalGraceDays),
        )
    }

    /**
     * 신청 기간 안에서만 취소할 수 있다.
     * 정원이 차서 `CLOSED` 인 세미나라도 기간 안이라면 취소가 가능하므로 상태가 아니라 기간을 본다.
     *
     * 신청과 같은 세미나 잠금을 잡으므로, 같은 신청을 동시에 여러 번 취소하면 처음 것만 지우고 나머지는 404 다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun cancelMine(
        rookie: User,
        seminarId: Long,
    ) {
        rookie.requireRole(Role.ROOKIE)

        val seminar = lockSeminar(seminarId)
        val enrollment =
            enrollmentRepository.findBySeminarIdAndRookieIdForUpdate(seminarId, rookie.requireId())
                ?: throw NotEnrolledException(seminarId)

        if (!seminar.isApplyPeriod(clock.instant())) {
            throw SeminarNotOpenException(seminarId)
        }

        removeWithRecords(enrollment)
    }

    /**
     * 탈퇴하는 루키의 신청 중 아직 신청 기간인 세미나의 것을 취소해 자리를 돌려준다.
     * 이미 신청이 끝난 세미나의 수강 기록은 남긴다. 호출자([com.wafflestudio.spring2026.user.service.UserService.withdraw])의
     * 트랜잭션에 참여한다.
     *
     * 세미나는 ID 순으로 잠근다. 사용자 행은 호출자가 이 작업 뒤에 고친다. 신청이 세미나 → 사용자 순으로
     * 잠그므로 순서를 맞춰야 서로 기다리며 멈추지 않는다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun cancelOpenEnrollmentsOf(rookieId: Long) {
        val now = clock.instant()

        enrollmentRepository
            .findAllByRookieId(rookieId)
            .map { it.requireSeminarId() }
            .sorted()
            .forEach { seminarId ->
                val seminar = seminarRepository.findByIdForUpdate(seminarId) ?: return@forEach
                if (!seminar.isApplyPeriod(now)) return@forEach

                enrollmentRepository.findBySeminarIdAndRookieIdForUpdate(seminarId, rookieId)?.let(::removeWithRecords)
            }
    }

    fun listMine(
        rookie: User,
        page: PageRequest,
    ): PageResponse<MyEnrollment> {
        rookie.requireRole(Role.ROOKIE)

        val all =
            enrollmentRepository
                .findAllByRookieId(rookie.requireId())
                .map { MyEnrollment(it, seminarService.getDetail(it.requireSeminarId())) }

        return PageResponse.slice(all, page)
    }

    /**
     * 대시보드의 Grace Day·탈락 칸 편집. 담당 운영진과 와장만 할 수 있다. 보낸 항목만 바꾼다.
     *
     * 수강 신청 행을 잠근 뒤 최신 값에 보낸 항목만 덮어쓴다. 두 운영진이 같은 수강생의 Grace Day 와
     * 탈락 여부를 동시에 고쳐도 한쪽 변경이 다른 쪽에 지워지지 않는다. 같은 항목이면 나중 요청이 남는다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun edit(
        editor: User,
        seminarId: Long,
        enrollmentId: Long,
        graceDaysRemaining: Int?,
        dropped: Boolean?,
    ): Enrollment {
        val seminar = findSeminar(seminarId)
        editor.requireManages(seminarId)

        val enrollment =
            enrollmentRepository.findByIdAndSeminarIdForUpdate(enrollmentId, seminarId)
                ?: throw EnrollmentNotFoundException(enrollmentId)

        return enrollmentRepository.save(enrollment.edit(graceDaysRemaining, dropped, seminar.totalGraceDays))
    }

    /** 신청을 지우기 전에 딸린 출석·과제 기록을 먼저 지운다. 어느 단계에서 실패해도 트랜잭션 전체가 되돌아간다. */
    private fun removeWithRecords(enrollment: Enrollment) {
        recordRepository.deleteAllByEnrollmentId(enrollment.requireId())
        enrollmentRepository.delete(enrollment)
    }

    /** 확인보다 먼저 잡는다. 잠금을 얻은 뒤의 조회는 앞서 끝난 신청·취소가 커밋한 결과를 모두 본다. */
    private fun lockSeminar(seminarId: Long): Seminar =
        seminarRepository.findByIdForUpdate(seminarId) ?: throw SeminarNotFoundException(seminarId)

    private fun findSeminar(seminarId: Long): Seminar =
        seminarRepository.findByIdOrNull(seminarId) ?: throw SeminarNotFoundException(seminarId)
}

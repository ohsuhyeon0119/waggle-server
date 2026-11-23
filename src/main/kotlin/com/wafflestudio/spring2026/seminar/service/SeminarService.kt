package com.wafflestudio.spring2026.seminar.service

import com.wafflestudio.spring2026.common.auth.requireManages
import com.wafflestudio.spring2026.common.auth.requireRole
import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.common.web.orKeep
import com.wafflestudio.spring2026.enrollment.repository.EnrollmentRepository
import com.wafflestudio.spring2026.seminar.model.Seminar
import com.wafflestudio.spring2026.seminar.model.SeminarDetail
import com.wafflestudio.spring2026.seminar.model.SeminarNotFoundException
import com.wafflestudio.spring2026.seminar.model.SeminarStatus
import com.wafflestudio.spring2026.seminar.repository.SeminarRepository
import com.wafflestudio.spring2026.session.repository.SeminarSessionRepository
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import java.time.Clock
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SeminarService(
    private val seminarRepository: SeminarRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val sessionRepository: SeminarSessionRepository,
    private val clock: Clock,
) {
    /** 세미나 개설은 와장만 할 수 있다. */
    @Transactional
    fun create(
        creator: User,
        command: SeminarCreateCommand,
    ): SeminarDetail {
        creator.requireRole(Role.ADMIN)

        return detailOf(
            seminarRepository.save(
                Seminar(
                    title = command.title,
                    description = command.description,
                    capacity = command.capacity,
                    applyStartAt = command.applyStartAt,
                    applyEndAt = command.applyEndAt,
                    totalGraceDays = command.totalGraceDays,
                ),
            ),
        )
    }

    fun getSeminar(seminarId: Long): Seminar =
        seminarRepository.findByIdOrNull(seminarId) ?: throw SeminarNotFoundException(seminarId)

    fun getDetail(seminarId: Long): SeminarDetail = detailOf(getSeminar(seminarId))

    /** 목록과 검색은 로그인 전에도 열려 있다. 가입 화면에서 담당 세미나를 골라야 하기 때문이다. */
    fun search(
        keyword: String?,
        status: SeminarStatus?,
        page: PageRequest,
    ): PageResponse<SeminarDetail> {
        val all =
            seminarRepository
                .findAllByKeyword(keyword?.trim()?.takeIf { it.isNotEmpty() })
                .map(::detailOf)
                .filter { status == null || it.status == status }

        return PageResponse.slice(all, page)
    }

    /** 개설 뒤 고칠 수 있는 것은 제목과 설명뿐이다. 와장과 그 세미나의 담당 운영진이 고친다. */
    @Transactional
    fun update(
        editor: User,
        seminarId: Long,
        command: SeminarUpdateCommand,
    ): SeminarDetail {
        val seminar = getSeminar(seminarId)
        editor.requireManages(seminarId)

        val updated =
            seminar.edit(
                title = command.title ?: seminar.title,
                description = command.description.orKeep(seminar.description),
            )

        return detailOf(seminarRepository.save(updated))
    }

    /**
     * 신청 인원과 회차 수는 저장하지 않고 조회할 때마다 센다.
     * 신청과 취소가 수시로 일어나므로 세미나 행에 들고 있으면 어긋나기 쉽다.
     */
    private fun detailOf(seminar: Seminar): SeminarDetail {
        val enrolledCount = enrollmentRepository.countBySeminarId(seminar.requireId())

        return SeminarDetail(
            seminar = seminar,
            enrolledCount = enrolledCount,
            sessionCount = sessionRepository.countBySeminarId(seminar.requireId()),
            status = seminar.statusAt(clock.instant(), enrolledCount),
        )
    }
}

package com.wafflestudio.spring2026.common.auth

import com.wafflestudio.spring2026.common.error.ForbiddenException
import com.wafflestudio.spring2026.enrollment.repository.EnrollmentRepository
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import org.springframework.stereotype.Component

/**
 * 세미나 안쪽(회차 안내, 현황 대시보드)을 볼 수 있는 사람.
 * 와장, 그 세미나의 담당 운영진, 그 세미나를 수강 중인 루키다. 모두 승인된 상태여야 한다.
 */
@Component
class SeminarViewerPolicy(
    private val enrollmentRepository: EnrollmentRepository,
) {
    fun requireCanView(
        viewer: User,
        seminarId: Long,
    ) {
        viewer.requireApproved()

        val allowed =
            when (viewer.role) {
                Role.ADMIN -> true
                Role.STAFF -> viewer.seminarId?.id == seminarId
                Role.ROOKIE -> enrollmentRepository.existsBySeminarIdAndRookieId(seminarId, viewer.requireId())
            }

        if (!allowed) {
            throw ForbiddenException("이 세미나의 정보를 볼 수 없습니다.")
        }
    }
}

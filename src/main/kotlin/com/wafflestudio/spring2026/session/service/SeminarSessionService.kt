package com.wafflestudio.spring2026.session.service

import com.wafflestudio.spring2026.common.auth.SeminarViewerPolicy
import com.wafflestudio.spring2026.common.auth.requireManages
import com.wafflestudio.spring2026.common.web.orKeep
import com.wafflestudio.spring2026.dashboard.repository.RecordRepository
import com.wafflestudio.spring2026.seminar.model.SeminarNotFoundException
import com.wafflestudio.spring2026.seminar.repository.SeminarRepository
import com.wafflestudio.spring2026.session.model.NotLastSessionException
import com.wafflestudio.spring2026.session.model.RoundedSession
import com.wafflestudio.spring2026.session.model.SeminarSession
import com.wafflestudio.spring2026.session.model.SessionHasRecordsException
import com.wafflestudio.spring2026.session.model.SessionNotFoundException
import com.wafflestudio.spring2026.session.repository.SeminarSessionRepository
import com.wafflestudio.spring2026.user.model.User
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SeminarSessionService(
    private val seminarRepository: SeminarRepository,
    private val sessionRepository: SeminarSessionRepository,
    private val recordRepository: RecordRepository,
    private val seminarViewerPolicy: SeminarViewerPolicy,
) {
    @Transactional
    fun create(
        author: User,
        seminarId: Long,
        command: SessionCreateCommand,
    ): RoundedSession {
        requireSeminarExists(seminarId)
        author.requireManages(seminarId)

        val saved =
            sessionRepository.save(
                SeminarSession(
                    seminarId = AggregateReference.to(seminarId),
                    title = command.title,
                    startsAt = command.startsAt,
                    location = command.location,
                    assignmentTitle = command.assignmentTitle,
                    lectureContent = command.lectureContent,
                    assignmentContent = command.assignmentContent,
                ),
            )

        return roundedSessionsOf(seminarId).first { it.session.id == saved.id }
    }

    /** 회차 안내는 와장, 담당 운영진, 수강 중인 루키가 본다. */
    fun listBySeminar(
        viewer: User,
        seminarId: Long,
    ): List<RoundedSession> {
        requireSeminarExists(seminarId)
        seminarViewerPolicy.requireCanView(viewer, seminarId)

        return roundedSessionsOf(seminarId)
    }

    /** 권한 확인을 이미 마친 호출자(대시보드)를 위한 목록. */
    fun listBySeminar(seminarId: Long): List<RoundedSession> {
        requireSeminarExists(seminarId)

        return roundedSessionsOf(seminarId)
    }

    fun getSession(
        viewer: User,
        sessionId: Long,
    ): RoundedSession {
        val session = findSession(sessionId)
        seminarViewerPolicy.requireCanView(viewer, session.requireSeminarId())

        return roundedSessionsOf(session.requireSeminarId()).first { it.session.id == session.id }
    }

    @Transactional
    fun update(
        author: User,
        sessionId: Long,
        command: SessionUpdateCommand,
    ): RoundedSession {
        val session = findSession(sessionId)
        author.requireManages(session.requireSeminarId())

        val updated =
            sessionRepository.save(
                session.copy(
                    title = command.title ?: session.title,
                    startsAt = command.startsAt ?: session.startsAt,
                    location = command.location ?: session.location,
                    assignmentTitle = command.assignmentTitle ?: session.assignmentTitle,
                    lectureContent = command.lectureContent.orKeep(session.lectureContent),
                    assignmentContent = command.assignmentContent.orKeep(session.assignmentContent),
                ),
            )

        return roundedSessionsOf(updated.requireSeminarId()).first { it.session.id == updated.id }
    }

    /**
     * 마지막 회차만, 그리고 출석·과제 기록이 없을 때만 지울 수 있다.
     * 회차 행을 먼저 잠근다. 기록 중인 요청은 회차에 읽기 잠금을 걸므로, 그 요청이 끝난 뒤 기록이 남았는지 본다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun delete(
        author: User,
        sessionId: Long,
    ) {
        val session = sessionRepository.findByIdForUpdate(sessionId) ?: throw SessionNotFoundException(sessionId)
        author.requireManages(session.requireSeminarId())

        if (roundedSessionsOf(session.requireSeminarId()).last().session.id != sessionId) {
            throw NotLastSessionException(sessionId)
        }
        if (recordRepository.hasAnyRecord(sessionId)) {
            throw SessionHasRecordsException(sessionId)
        }

        sessionRepository.delete(session)
    }

    private fun findSession(sessionId: Long): SeminarSession =
        sessionRepository.findByIdOrNull(sessionId) ?: throw SessionNotFoundException(sessionId)

    private fun requireSeminarExists(seminarId: Long) {
        if (!seminarRepository.existsById(seminarId)) {
            throw SeminarNotFoundException(seminarId)
        }
    }

    /** 회차 번호는 세미나의 회차 전체를 정렬한 뒤의 순서다. 한 세미나의 회차 수는 많지 않다. */
    private fun roundedSessionsOf(seminarId: Long): List<RoundedSession> =
        sessionRepository
            .findAllBySeminarIdInRoundOrder(seminarId)
            .mapIndexed { index, session -> RoundedSession(session, round = index + 1) }
}

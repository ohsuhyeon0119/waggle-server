package com.wafflestudio.spring2026.session.model

import com.wafflestudio.spring2026.seminar.model.Seminar
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * 세미나의 수업 회차. 회차마다 과제가 정확히 하나 있으므로 과제 정보도 같은 행에 둔다.
 *
 * 회차 번호(`round`)는 저장하지 않는다. `startsAt` 오름차순으로 정렬했을 때의 순서일 뿐이라,
 * 더 이른 회차가 나중에 등록되면 기존 회차의 번호가 함께 바뀌어야 하기 때문이다.
 */
@Table("seminar_sessions")
data class SeminarSession(
    @Id
    val id: Long? = null,
    val seminarId: AggregateReference<Seminar, Long>,
    val title: String,
    val startsAt: Instant,
    val location: String,
    val assignmentTitle: String,
    val lectureContent: String?,
    val assignmentContent: String?,
    @CreatedDate
    val createdAt: Instant = Instant.EPOCH,
    @LastModifiedDate
    val updatedAt: Instant = Instant.EPOCH,
) {
    fun requireId(): Long = requireNotNull(id) { "아직 저장되지 않은 회차입니다." }

    fun requireSeminarId(): Long = requireNotNull(seminarId.id) { "회차는 반드시 세미나에 속한다." }
}

/** 조회 시점에 계산한 회차 번호를 붙인 회차. */
data class RoundedSession(
    val session: SeminarSession,
    val round: Int,
)

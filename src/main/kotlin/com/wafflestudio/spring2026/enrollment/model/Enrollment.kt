package com.wafflestudio.spring2026.enrollment.model

import com.wafflestudio.spring2026.seminar.model.Seminar
import com.wafflestudio.spring2026.user.model.User
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * 루키 한 명이 세미나 하나를 수강하는 일.
 *
 * 잔여 Grace Day 와 탈락 여부는 루키가 아니라 "이 루키가 이 세미나를 수강하는 것"에 대한 값이므로
 * 여기에 둔다. 같은 루키라도 세미나마다 값이 다르다.
 */
@Table("enrollments")
data class Enrollment(
    @Id
    val id: Long? = null,
    val seminarId: AggregateReference<Seminar, Long>,
    val rookieId: AggregateReference<User, Long>,
    val graceDaysRemaining: Int,
    val dropped: Boolean = false,
    @CreatedDate
    val createdAt: Instant = Instant.EPOCH,
    @LastModifiedDate
    val updatedAt: Instant = Instant.EPOCH,
) {
    fun requireId(): Long = requireNotNull(id) { "아직 저장되지 않은 수강 신청입니다." }

    fun requireSeminarId(): Long = requireNotNull(seminarId.id)

    fun requireRookieId(): Long = requireNotNull(rookieId.id)

    /**
     * 보낸 항목만 바꾼다. Grace Day 는 0 미만이거나 세미나의 총 Grace Day 보다 클 수 없다.
     * 범위를 먼저 확인하므로, 하나라도 잘못되면 어느 항목도 바뀌지 않는다.
     */
    fun edit(
        graceDaysRemaining: Int?,
        dropped: Boolean?,
        totalGraceDays: Int,
    ): Enrollment {
        graceDaysRemaining?.let {
            if (it !in 0..totalGraceDays) {
                throw GraceDaysOutOfRangeException(it, totalGraceDays)
            }
        }

        return copy(
            graceDaysRemaining = graceDaysRemaining ?: this.graceDaysRemaining,
            dropped = dropped ?: this.dropped,
        )
    }

    companion object {
        /** 새 신청은 세미나의 총 Grace Day 와 탈락하지 않은 상태로 시작한다. 취소 후 다시 신청해도 같다. */
        fun of(
            seminarId: Long,
            rookieId: Long,
            totalGraceDays: Int,
        ): Enrollment =
            Enrollment(
                seminarId = AggregateReference.to(seminarId),
                rookieId = AggregateReference.to(rookieId),
                graceDaysRemaining = totalGraceDays,
            )
    }
}

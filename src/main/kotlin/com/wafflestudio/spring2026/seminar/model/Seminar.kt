package com.wafflestudio.spring2026.seminar.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("seminars")
data class Seminar(
    @Id
    val id: Long? = null,
    val title: String,
    val description: String?,
    val capacity: Int,
    val applyStartAt: Instant,
    val applyEndAt: Instant,
    val totalGraceDays: Int,
    @CreatedDate
    val createdAt: Instant = Instant.EPOCH,
    @LastModifiedDate
    val updatedAt: Instant = Instant.EPOCH,
) {
    fun requireId(): Long = requireNotNull(id) { "아직 저장되지 않은 세미나입니다." }

    /**
     * 신청 기간과 정원 소진 여부로 정해지는 신청 상태.
     * 기간 종료와 정원 마감은 이유가 다르지만 표시값은 모두 [SeminarStatus.CLOSED] 다.
     */
    fun statusAt(
        now: Instant,
        enrolledCount: Int,
    ): SeminarStatus =
        when {
            !now.isBefore(applyEndAt) || enrolledCount >= capacity -> SeminarStatus.CLOSED
            now.isBefore(applyStartAt) -> SeminarStatus.BEFORE
            else -> SeminarStatus.OPEN
        }

    /** 신청과 취소가 열려 있는 기간인지. 정원 소진 여부와는 무관하다. */
    fun isApplyPeriod(now: Instant): Boolean = !now.isBefore(applyStartAt) && now.isBefore(applyEndAt)

    /** 개설 뒤 고칠 수 있는 것은 제목과 설명뿐이다. */
    fun edit(
        title: String,
        description: String?,
    ): Seminar = copy(title = title, description = description)
}

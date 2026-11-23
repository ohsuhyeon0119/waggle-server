package com.wafflestudio.spring2026.enrollment.repository

import com.wafflestudio.spring2026.enrollment.model.Enrollment
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface EnrollmentRepository : CrudRepository<Enrollment, Long> {
    @Query("SELECT COUNT(*) FROM enrollments WHERE seminar_id = :seminarId")
    fun countBySeminarId(
        @Param("seminarId") seminarId: Long,
    ): Int

    @Query("SELECT COUNT(*) > 0 FROM enrollments WHERE seminar_id = :seminarId AND rookie_id = :rookieId")
    fun existsBySeminarIdAndRookieId(
        @Param("seminarId") seminarId: Long,
        @Param("rookieId") rookieId: Long,
    ): Boolean

    @Query("SELECT * FROM enrollments WHERE rookie_id = :rookieId ORDER BY created_at DESC, id DESC")
    fun findAllByRookieId(
        @Param("rookieId") rookieId: Long,
    ): List<Enrollment>

    /** 대시보드의 행 순서. 수강 신청 ID 오름차순이다. */
    @Query("SELECT * FROM enrollments WHERE seminar_id = :seminarId ORDER BY id LIMIT :size OFFSET :offset")
    fun findPageBySeminarId(
        @Param("seminarId") seminarId: Long,
        @Param("size") size: Int,
        @Param("offset") offset: Long,
    ): List<Enrollment>

    // 아래 잠금 조회는 수강 신청 행 하나를 고치거나 지우는 작업끼리 순서를 정한다.
    // 수강 정보 수정, 출석·과제 기록, 취소가 같은 행을 잡으므로, 취소가 기록을 지우는 사이에
    // 새 기록이 끼어들지 못한다.

    @Query("SELECT * FROM enrollments WHERE seminar_id = :seminarId AND rookie_id = :rookieId FOR UPDATE")
    fun findBySeminarIdAndRookieIdForUpdate(
        @Param("seminarId") seminarId: Long,
        @Param("rookieId") rookieId: Long,
    ): Enrollment?

    @Query("SELECT * FROM enrollments WHERE id = :enrollmentId AND seminar_id = :seminarId FOR UPDATE")
    fun findByIdAndSeminarIdForUpdate(
        @Param("enrollmentId") enrollmentId: Long,
        @Param("seminarId") seminarId: Long,
    ): Enrollment?

    /**
     * 여러 행을 한 번에 잠근다. ID 오름차순으로 잠그므로, 겹치는 수강생을 기록하는 두 요청이
     * 서로가 잡은 행을 기다리며 멈추는 일(교착 상태)이 생기지 않는다.
     */
    @Query("SELECT * FROM enrollments WHERE id IN (:enrollmentIds) ORDER BY id FOR UPDATE")
    fun findAllByIdsForUpdate(
        @Param("enrollmentIds") enrollmentIds: Collection<Long>,
    ): List<Enrollment>
}

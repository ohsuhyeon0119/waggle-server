package com.wafflestudio.spring2026.user.repository

import com.wafflestudio.spring2026.user.model.User
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface UserRepository : CrudRepository<User, Long> {
    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): User?

    /**
     * 가입 심사 목록. 와장 계정과 탈퇴한 사용자는 심사 대상이 아니라 빼고,
     * `:role` 과 `:status` 는 값을 주지 않으면 전체를 뜻한다.
     */
    @Query(
        """
        SELECT * FROM users
        WHERE role <> 'ADMIN' AND status <> 'WITHDRAWN'
          AND (:role IS NULL OR role = :role)
          AND (:status IS NULL OR status = :status)
        ORDER BY created_at DESC, id DESC
        LIMIT :size OFFSET :offset
        """,
    )
    fun findApplicants(
        @Param("role") role: String?,
        @Param("status") status: String?,
        @Param("size") size: Int,
        @Param("offset") offset: Long,
    ): List<User>

    @Query(
        """
        SELECT COUNT(*) FROM users
        WHERE role <> 'ADMIN' AND status <> 'WITHDRAWN'
          AND (:role IS NULL OR role = :role)
          AND (:status IS NULL OR status = :status)
        """,
    )
    fun countApplicants(
        @Param("role") role: String?,
        @Param("status") status: String?,
    ): Long
}

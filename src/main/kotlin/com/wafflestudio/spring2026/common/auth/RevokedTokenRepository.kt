package com.wafflestudio.spring2026.common.auth

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RevokedTokenRepository : CrudRepository<RevokedToken, String> {
    /**
     * ID 를 애플리케이션이 정하는 엔티티는 `save` 가 UPDATE 로 판단하므로 INSERT 를 직접 쓴다.
     * 이미 무효화된 토큰이면 PK 충돌로 DuplicateKeyException 이 난다.
     */
    @Modifying
    @Query(
        """
        INSERT INTO revoked_tokens (token_id, user_id, expires_at, created_at)
        VALUES (:tokenId, :userId, :expiresAt, :createdAt)
        """,
    )
    fun insert(
        @Param("tokenId") tokenId: String,
        @Param("userId") userId: Long,
        @Param("expiresAt") expiresAt: Instant,
        @Param("createdAt") createdAt: Instant,
    ): Int
}

package com.wafflestudio.spring2026.common.auth

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/** 로그아웃으로 무효화된 Access Token. 토큰 원문이 아니라 `jti` 만 기억한다. */
@Table("revoked_tokens")
data class RevokedToken(
    @Id
    val tokenId: String,
    val userId: Long,
    val expiresAt: Instant,
    val createdAt: Instant,
)

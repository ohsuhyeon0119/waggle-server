package com.wafflestudio.spring2026.common.auth

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/** 서명과 만료를 통과한 Access Token 에서 꺼낸 값. 로그아웃은 [tokenId] 로 이 토큰 하나만 무효화한다. */
data class AccessToken(
    val userId: Long,
    val tokenId: String,
    val expiresAt: Instant,
)

/**
 * HS256 JWT Access Token 을 발급하고 검증한다. (3주차 과제 문서의 "JWT Access Token")
 *
 * 토큰에는 사용자 ID(`sub`), 토큰 ID(`jti`), 발급·만료 시각(`iat`, `exp`)만 담는다. 역할과 가입 상태는
 * 심사로 바뀔 수 있으므로 토큰에 넣지 않고, [JwtAuthenticationFilter] 가 요청마다 DB 에서 읽는다.
 */
@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    private val clock: Clock,
) {
    // HS256 은 256비트(32바이트) 이상의 키를 요구한다. 짧으면 WeakKeyException 으로 기동이 실패한다.
    // 3주차 테스트가 같은 키로 토큰을 직접 만들므로, 설정값의 UTF-8 바이트를 가공하지 않고 그대로 쓴다.
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(UTF_8))

    /**
     * `iat`, `exp` 는 초 단위로 직렬화되므로 같은 시각에서 계산해야 `exp = iat + 3600` 이 정확히 맞는다.
     * `jti` 가 있어야 같은 사용자가 같은 초에 두 번 로그인해도 서로 다른 토큰이 되어, 하나만 로그아웃할 수 있다.
     */
    fun issue(userId: Long): String {
        val now = clock.instant()

        return Jwts
            .builder()
            .subject(userId.toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(TTL)))
            .signWith(key, Jwts.SIG.HS256)
            .compact()
    }

    /**
     * 유효하면 토큰의 값, 아니면 null.
     * 형식이 틀렸든, 서명이 맞지 않든, 만료되었든 모두 "누구인지 확인할 수 없음" 으로 같게 다룬다.
     * 로그아웃으로 무효화되었는지는 DB 를 봐야 하므로 여기서 판단하지 않는다. ([JwtAuthenticationFilter])
     */
    fun verify(token: String): AccessToken? =
        try {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(key)
                    .clock { Date.from(clock.instant()) }
                    .build()
                    .parseSignedClaims(token)
                    .payload

            val userId = claims.subject?.toLongOrNull()
            val tokenId = claims.id
            val expiresAt = claims.expiration?.toInstant()

            // 이 서버가 발급한 토큰에는 셋 다 있다. 하나라도 없으면 무효화할 방법이 없으므로 받지 않는다.
            if (userId != null && tokenId != null && expiresAt != null) AccessToken(userId, tokenId, expiresAt) else null
        } catch (exception: JwtException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }

    private companion object {
        /** 발급 후 1시간. (3주차 과제 문서: `exp = iat + 3600`) */
        val TTL: Duration = Duration.ofHours(1)
    }
}

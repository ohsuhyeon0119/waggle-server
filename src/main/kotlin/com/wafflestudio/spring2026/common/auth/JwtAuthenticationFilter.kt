package com.wafflestudio.spring2026.common.auth

import com.wafflestudio.spring2026.user.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * `Authorization: Bearer <token>` 을 검증해 토큰의 주인을 [SecurityContextHolder] 에 넣는다.
 *
 * Principal 은 DB 에서 방금 읽은 사용자다. 심사가 끝나거나 탈퇴해도 이미 발급된 토큰이
 * 옛 역할·상태로 남지 않게 하기 위해서다. Credentials 는 검증을 마친 [AccessToken] 이다.
 * 로그아웃이 이 값으로 요청에 쓴 토큰만 무효화한다.
 *
 * 토큰이 없거나, 유효하지 않거나, 로그아웃으로 무효화되었으면 아무것도 넣지 않고 넘긴다. 로그인 없이 쓰는 경로는 그대로 통과하고,
 * 로그인이 필요한 경로는 [SecurityConfig] 의 `AuthenticationEntryPoint` 가 401 로 응답한다.
 *
 * 빈으로 등록하면 서블릿 필터로도 한 번 더 등록되므로, [SecurityConfig] 에서 직접 만들어 체인에 넣는다.
 */
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token =
            request
                .getHeader(HttpHeaders.AUTHORIZATION)
                ?.takeIf { it.startsWith(BEARER) }
                ?.removePrefix(BEARER)
                ?.let(jwtProvider::verify)
                ?.takeUnless { revokedTokenRepository.existsById(it.tokenId) }
        val user = token?.let { userRepository.findByIdOrNull(it.userId) }

        if (user != null) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    user,
                    token,
                    listOf(SimpleGrantedAuthority("ROLE_${user.role}")),
                )
        }

        filterChain.doFilter(request, response)
    }

    private companion object {
        const val BEARER = "Bearer "
    }
}

package com.wafflestudio.spring2026.common.auth

import com.wafflestudio.spring2026.common.error.ApiErrorResponse
import com.wafflestudio.spring2026.user.repository.UserRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets.UTF_8

/**
 * 인증과 큰 단위의 인가를 맡는다.
 *
 * - 로그인 없이 쓰는 경로만 열고, 나머지는 모두 인증을 요구한다.
 * - 역할만으로 정해지는 규칙(세미나 개설은 와장만)은 여기서 막는다.
 * - 심사 상태, 담당 세미나, 수강 여부처럼 DB 를 봐야 아는 규칙은 각 서비스가 [AccessRules] 로 확인한다.
 *
 * 필터에서 거절한 요청은 컨트롤러에 닿지 않으므로 `GlobalExceptionHandler` 대신
 * 아래 핸들러가 `ApiErrorResponse` 형식으로 직접 응답한다.
 */
@Configuration
class SecurityConfig(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val revokedTokenRepository: RevokedTokenRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${waggle.cors.allowed-origins}") private val corsAllowedOrigins: List<String>,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()
                    // 운영진 지원자가 계정이 생기기 전에 담당 세미나를 골라야 한다.
                    .requestMatchers(HttpMethod.GET, "/seminars").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    // 처리되지 않은 오류가 넘어오는 곳. 막으면 400·500 이 401 로 바뀐다.
                    .requestMatchers("/error").permitAll()
                    // 1주차 실습 코드
                    .requestMatchers("/hello", "/meetings", "/meetings/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/seminars").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }.exceptionHandling {
                it
                    .authenticationEntryPoint { _, response, _ ->
                        response.writeError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다.")
                    }.accessDeniedHandler { _, response, _ ->
                        response.writeError(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.")
                    }
            }.addFilterBefore(
                JwtAuthenticationFilter(jwtProvider, userRepository, revokedTokenRepository),
                UsernamePasswordAuthenticationFilter::class.java,
            ).build()

    /** 프론트는 다른 출처(CloudFront)에서 뜬다. 인증은 쿠키가 아니라 `Authorization` 헤더라 credentials 는 쓰지 않는다. */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    // apply 블록 안의 `allowedOrigins` 는 CorsConfiguration 자신의 속성을 가리키므로 이름을 달리한다.
                    allowedOriginPatterns = corsAllowedOrigins
                    allowedMethods = listOf("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                    allowedHeaders = listOf("*")
                    maxAge = 3600
                },
            )
        }

    private fun HttpServletResponse.writeError(
        status: HttpStatus,
        code: String,
        message: String,
    ) {
        this.status = status.value()
        contentType = MediaType.APPLICATION_JSON_VALUE
        characterEncoding = UTF_8.name()
        writer.write(objectMapper.writeValueAsString(ApiErrorResponse(code = code, message = message)))
    }
}

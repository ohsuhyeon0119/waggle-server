package com.wafflestudio.spring2026.common.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 비밀번호는 BCrypt 해시로만 저장한다.
 * spring-security-crypto 만 쓰므로 Spring Security 의 필터 체인은 들어오지 않는다.
 */
@Configuration
class PasswordConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

package com.wafflestudio.spring2026.user.controller

import com.wafflestudio.spring2026.common.auth.AccessToken
import com.wafflestudio.spring2026.user.dto.LoginRequest
import com.wafflestudio.spring2026.user.dto.LoginResponse
import com.wafflestudio.spring2026.user.dto.SignupRequest
import com.wafflestudio.spring2026.user.dto.SignupResponse
import com.wafflestudio.spring2026.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증", description = "가입, 로그인, 로그아웃")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
) {
    @Operation(summary = "가입 신청", description = "루키 또는 운영진으로 가입을 신청한다. 신청은 대기 상태로 남는다.")
    @PostMapping("/signup")
    fun signUp(
        @Valid @RequestBody request: SignupRequest,
    ): ResponseEntity<SignupResponse> {
        val user = userService.signUp(request.toCommand())

        return ResponseEntity
            .created(URI.create("/users/${user.requireId()}"))
            .body(SignupResponse.from(user))
    }

    @Operation(summary = "로그인", description = "대기·반려 상태여도 로그인은 된다. 클라이언트는 GET /users/me 의 status 를 보고 안내 화면을 띄운다.")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): LoginResponse {
        return LoginResponse(accessToken = userService.login(request.email, request.password))
    }

    @Operation(summary = "로그아웃", description = "요청에 쓴 Access Token 하나만 무효화한다. 대기·반려 상태여도 쓸 수 있다.")
    @PostMapping("/logout")
    fun logout(authentication: Authentication): ResponseEntity<Void> {
        // JwtAuthenticationFilter 가 검증을 마친 토큰을 credentials 로 넣어 둔다.
        userService.logout(authentication.credentials as AccessToken)

        return ResponseEntity.noContent().build()
    }
}

package com.wafflestudio.spring2026.user.controller

import com.wafflestudio.spring2026.common.auth.requireApproved
import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.enrollment.dto.MyEnrollmentResponse
import com.wafflestudio.spring2026.enrollment.service.EnrollmentService
import com.wafflestudio.spring2026.user.dto.UserApprovalRequest
import com.wafflestudio.spring2026.user.dto.UserResponse
import com.wafflestudio.spring2026.user.dto.UserUpdateRequest
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import com.wafflestudio.spring2026.user.model.UserStatus
import com.wafflestudio.spring2026.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "사용자", description = "내 정보, 가입 심사")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val enrollmentService: EnrollmentService,
) {
    @Operation(summary = "내 정보", description = "대기·반려 사용자가 쓸 수 있는 유일한 조회 API.")
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal user: User,
    ): UserResponse = UserResponse.from(user)

    @Operation(summary = "내 정보 수정", description = "이름과 GitHub 사용자명만 바꿀 수 있다.")
    @PatchMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal user: User,
        @RequestBody request: UserUpdateRequest,
    ): UserResponse {
        val valid = request.validated()

        return UserResponse.from(userService.updateProfile(user, valid.name, valid.githubUsername))
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "사용자 행을 지우지 않고 WITHDRAWN 으로 표시한다. 진행 중인 세미나의 신청만 취소해 자리를 돌려준다.",
    )
    @DeleteMapping("/me")
    fun withdraw(
        @AuthenticationPrincipal user: User,
    ): ResponseEntity<Void> {
        userService.withdraw(user)

        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "내 수강 목록")
    @GetMapping("/me/enrollments")
    fun myEnrollments(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<MyEnrollmentResponse> =
        enrollmentService.listMine(user, PageRequest.of(page, size)).map(MyEnrollmentResponse::from)

    @Operation(summary = "가입 신청 목록", description = "운영진은 루키 신청만, 와장은 전부 본다.")
    @GetMapping
    fun listApplicants(
        @AuthenticationPrincipal user: User,
        @RequestParam(required = false) status: UserStatus?,
        @RequestParam(required = false) role: Role?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<UserResponse> =
        userService.listApplicants(user, status, role, PageRequest.of(page, size)).map(UserResponse::from)

    @Operation(
        summary = "사용자 단건 조회",
        description = "승인된 사용자라면 역할과 관계없이 누구의 프로필이든 볼 수 있다. 고치는 것은 /users/me 뿐이다.",
    )
    @GetMapping("/{userId}")
    fun getUser(
        @AuthenticationPrincipal user: User,
        @PathVariable userId: Long,
    ): UserResponse {
        // 현황 표와 가입 심사 목록의 이름이 이 화면으로 연결된다. 표에 이미 보이는 정보라 역할로 가르지 않는다.
        user.requireApproved()

        return UserResponse.from(userService.getUser(userId))
    }

    @Operation(summary = "가입 심사", description = "대기 중인 가입 신청을 승인하거나 반려한다.")
    @PatchMapping("/{userId}/approval", "/{userId}/status")
    fun review(
        @AuthenticationPrincipal user: User,
        @PathVariable userId: Long,
        @Valid @RequestBody request: UserApprovalRequest,
    ): UserResponse = UserResponse.from(userService.review(user, userId, request.status))
}

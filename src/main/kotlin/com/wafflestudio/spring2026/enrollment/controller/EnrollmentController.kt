package com.wafflestudio.spring2026.enrollment.controller

import com.wafflestudio.spring2026.enrollment.dto.EnrollmentCreateResponse
import com.wafflestudio.spring2026.enrollment.dto.EnrollmentUpdateResponse
import com.wafflestudio.spring2026.enrollment.dto.EnrollmentUpdateRequest
import com.wafflestudio.spring2026.enrollment.service.EnrollmentService
import com.wafflestudio.spring2026.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "수강 신청", description = "선착순 수강 신청과 취소, 수강 정보 편집")
@RestController
class EnrollmentController(
    private val enrollmentService: EnrollmentService,
) {
    @Operation(summary = "수강 신청", description = "신청 기간이고 정원이 남아 있으면 즉시 확정된다.")
    @PostMapping("/seminars/{seminarId}/enrollments")
    fun enroll(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
    ): ResponseEntity<EnrollmentCreateResponse> {
        val enrollment = enrollmentService.enroll(user, seminarId)

        return ResponseEntity
            .created(URI.create("/seminars/$seminarId/enrollments/${enrollment.requireId()}"))
            .body(EnrollmentCreateResponse.from(enrollment))
    }

    @Operation(summary = "수강 취소", description = "신청 기간 안에서만 취소할 수 있다. 취소하면 자리가 빈다.")
    @DeleteMapping("/seminars/{seminarId}/enrollments/me")
    fun cancel(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
    ): ResponseEntity<Void> {
        enrollmentService.cancelMine(user, seminarId)

        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "수강 정보 수정",
        description =
            "잔여 Grace Day 와 탈락 여부. 보낸 항목만 바꾸며 하나라도 잘못되면 아무것도 바꾸지 않는다. " +
                "enrollmentId 는 사용자 ID 가 아니라 수강 신청 ID 다. 와장과 담당 운영진만 고칠 수 있다.",
    )
    @PatchMapping("/seminars/{seminarId}/enrollments/{enrollmentId}")
    fun edit(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
        @PathVariable enrollmentId: Long,
        @RequestBody request: EnrollmentUpdateRequest,
    ): EnrollmentUpdateResponse {
        val command = request.toCommand()

        return EnrollmentUpdateResponse.from(
            enrollmentService.edit(
                editor = user,
                seminarId = seminarId,
                enrollmentId = enrollmentId,
                graceDaysRemaining = command.graceDaysRemaining,
                dropped = command.dropped,
            ),
        )
    }
}

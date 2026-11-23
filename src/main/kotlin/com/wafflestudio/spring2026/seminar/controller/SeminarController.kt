package com.wafflestudio.spring2026.seminar.controller

import com.wafflestudio.spring2026.common.auth.requireApproved
import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.seminar.dto.SeminarCreateRequest
import com.wafflestudio.spring2026.seminar.dto.SeminarCreateResponse
import com.wafflestudio.spring2026.seminar.dto.SeminarResponse
import com.wafflestudio.spring2026.seminar.dto.SeminarUpdateRequest
import com.wafflestudio.spring2026.seminar.model.SeminarStatus
import com.wafflestudio.spring2026.seminar.service.SeminarService
import com.wafflestudio.spring2026.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "세미나", description = "세미나 개설과 조회")
@RestController
@RequestMapping("/seminars")
class SeminarController(
    private val seminarService: SeminarService,
) {
    @Operation(summary = "세미나 개설", description = "와장만 개설할 수 있다.")
    @PostMapping
    fun create(
        @AuthenticationPrincipal user: User,
        @Valid @RequestBody request: SeminarCreateRequest,
    ): ResponseEntity<SeminarCreateResponse> {
        val detail = seminarService.create(user, request.toCommand())

        return ResponseEntity
            .created(URI.create("/seminars/${detail.seminar.requireId()}"))
            .body(SeminarCreateResponse.from(detail))
    }

    @Operation(summary = "세미나 목록·검색", description = "로그인 없이도 볼 수 있다.")
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: SeminarStatus?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): PageResponse<SeminarResponse> =
        seminarService.search(keyword, status, PageRequest.of(page, size)).map(SeminarResponse::from)

    @Operation(summary = "세미나 단건 조회", description = "현재 신청 인원과 신청 상태를 함께 돌려준다. 승인된 사용자만 볼 수 있다.")
    @GetMapping("/{seminarId}")
    fun getSeminar(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
    ): SeminarResponse {
        user.requireApproved()

        return SeminarResponse.from(seminarService.getDetail(seminarId))
    }

    @Operation(summary = "세미나 수정", description = "제목과 설명만, 와장과 담당 운영진이 고칠 수 있다.")
    @PatchMapping("/{seminarId}")
    fun update(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
        @RequestBody request: SeminarUpdateRequest,
    ): SeminarResponse = SeminarResponse.from(seminarService.update(user, seminarId, request.toCommand()))
}

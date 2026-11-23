package com.wafflestudio.spring2026.session.controller

import com.wafflestudio.spring2026.session.dto.SessionCreateRequest
import com.wafflestudio.spring2026.session.dto.SessionCreateResponse
import com.wafflestudio.spring2026.session.dto.SessionDetailResponse
import com.wafflestudio.spring2026.session.dto.SessionSummaryResponse
import com.wafflestudio.spring2026.session.dto.SessionUpdateRequest
import com.wafflestudio.spring2026.session.service.SeminarSessionService
import com.wafflestudio.spring2026.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.net.URI
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "수업 회차", description = "회차 등록과 수업·과제 안내")
@RestController
class SeminarSessionController(
    private val sessionService: SeminarSessionService,
) {
    @Operation(summary = "회차 등록", description = "회차와 그 회차의 과제를 함께 등록한다. 담당 운영진과 와장만 할 수 있다.")
    @PostMapping("/seminars/{seminarId}/sessions")
    fun create(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
        @Valid @RequestBody request: SessionCreateRequest,
    ): ResponseEntity<SessionCreateResponse> {
        val rounded = sessionService.create(user, seminarId, request.toCommand())

        return ResponseEntity
            .created(URI.create("/sessions/${rounded.session.requireId()}"))
            .body(SessionCreateResponse.from(rounded))
    }

    @Operation(summary = "회차 목록", description = "회차 번호 오름차순. 본문이 긴 설명은 담지 않는다.")
    @GetMapping("/seminars/{seminarId}/sessions")
    fun listBySeminar(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
    ): List<SessionSummaryResponse> = sessionService.listBySeminar(user, seminarId).map(SessionSummaryResponse::from)

    @Operation(summary = "회차 단건 조회", description = "수업·과제 설명까지 함께 돌려준다.")
    @GetMapping("/sessions/{sessionId}")
    fun getSession(
        @AuthenticationPrincipal user: User,
        @PathVariable sessionId: Long,
    ): SessionDetailResponse = SessionDetailResponse.from(sessionService.getSession(user, sessionId))

    @Operation(summary = "회차 수정", description = "보낸 항목만 바꾼다. 설명은 null 을 보내 지운다.")
    @PatchMapping("/sessions/{sessionId}")
    fun update(
        @AuthenticationPrincipal user: User,
        @PathVariable sessionId: Long,
        @RequestBody request: SessionUpdateRequest,
    ): SessionDetailResponse = SessionDetailResponse.from(sessionService.update(user, sessionId, request.toCommand()))

    @Operation(summary = "회차 삭제", description = "마지막 회차만, 출석·과제 기록이 없을 때만 지울 수 있다.")
    @DeleteMapping("/sessions/{sessionId}")
    fun delete(
        @AuthenticationPrincipal user: User,
        @PathVariable sessionId: Long,
    ): ResponseEntity<Void> {
        sessionService.delete(user, sessionId)

        return ResponseEntity.noContent().build()
    }
}

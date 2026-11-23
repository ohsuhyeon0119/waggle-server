package com.wafflestudio.spring2026.dashboard.controller

import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.dashboard.dto.DashboardResponse
import com.wafflestudio.spring2026.dashboard.dto.RecordsUpdateRequest
import com.wafflestudio.spring2026.dashboard.service.DashboardService
import com.wafflestudio.spring2026.user.model.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "현황 대시보드", description = "출석·과제 기록과 현황 조회")
@RestController
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @Operation(
        summary = "현황 조회",
        description =
            "행은 수강생, 열은 회차인 표. 수강생만 페이지로 나누고 회차는 언제나 전부 담는다. " +
                "page 는 0 이상, size 는 1 이상 100 이하이며 벗어나면 400 이다. 와장, 담당 운영진, 수강 중인 루키가 볼 수 있다.",
    )
    @GetMapping("/seminars/{seminarId}/dashboard")
    fun getDashboard(
        @AuthenticationPrincipal user: User,
        @PathVariable seminarId: Long,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
    ): DashboardResponse =
        DashboardResponse.from(dashboardService.getDashboard(user, seminarId, PageRequest.strict(page, size)))

    @Operation(
        summary = "출석·과제 일괄 수정",
        description =
            "한 회차에서 여러 수강생의 기록을 한 번에 고친다. 생략한 칸은 그대로, null 은 기록 삭제다. " +
                "항목 하나라도 잘못되면 아무것도 반영하지 않는다. 와장과 담당 운영진만 할 수 있다.",
    )
    @PatchMapping("/sessions/{sessionId}/records")
    fun updateRecords(
        @AuthenticationPrincipal user: User,
        @PathVariable sessionId: Long,
        @RequestBody request: RecordsUpdateRequest,
    ): ResponseEntity<Void> {
        dashboardService.updateRecords(user, sessionId, request.toChanges())

        return ResponseEntity.noContent().build()
    }
}

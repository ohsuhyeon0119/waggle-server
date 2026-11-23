package com.wafflestudio.spring2026.seminar.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.wafflestudio.spring2026.seminar.service.SeminarCreateCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.time.OffsetDateTime

data class SeminarCreateRequest(
    @field:NotBlank(message = "세미나 제목은 공백일 수 없습니다.")
    val title: String,
    @field:Schema(description = "Markdown 형식의 세미나 소개")
    val description: String? = null,
    @field:Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    val capacity: Int,
    val applyStartAt: OffsetDateTime,
    val applyEndAt: OffsetDateTime,
    @field:PositiveOrZero(message = "총 Grace Day 는 0 이상이어야 합니다.")
    val totalGraceDays: Int,
) {
    @get:AssertTrue(message = "신청 종료 일시는 신청 시작 일시보다 뒤여야 합니다.")
    @get:JsonIgnore
    @get:Schema(hidden = true)
    val applyPeriodValid: Boolean
        get() = applyEndAt.isAfter(applyStartAt)

    fun toCommand(): SeminarCreateCommand =
        SeminarCreateCommand(
            title = title,
            description = description,
            capacity = capacity,
            applyStartAt = applyStartAt.toInstant(),
            applyEndAt = applyEndAt.toInstant(),
            totalGraceDays = totalGraceDays,
        )
}

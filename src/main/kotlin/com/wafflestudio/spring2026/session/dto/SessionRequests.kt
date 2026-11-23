package com.wafflestudio.spring2026.session.dto

import com.wafflestudio.spring2026.common.error.InvalidRequestException
import com.wafflestudio.spring2026.common.web.readPatch
import com.wafflestudio.spring2026.session.service.SessionCreateCommand
import com.wafflestudio.spring2026.session.service.SessionUpdateCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime

data class SessionCreateRequest(
    @field:NotBlank(message = "회차 제목은 공백일 수 없습니다.")
    val title: String,
    val startsAt: OffsetDateTime,
    @field:NotBlank(message = "장소는 공백일 수 없습니다.")
    val location: String,
    @field:NotBlank(message = "과제 제목은 공백일 수 없습니다.")
    val assignmentTitle: String,
    @field:Schema(description = "Markdown 형식의 수업 설명")
    val lectureContent: String? = null,
    @field:Schema(description = "Markdown 형식의 과제 설명")
    val assignmentContent: String? = null,
) {
    fun toCommand(): SessionCreateCommand =
        SessionCreateCommand(
            title = title,
            startsAt = startsAt.toInstant(),
            location = location,
            assignmentTitle = assignmentTitle,
            lectureContent = lectureContent,
            assignmentContent = assignmentContent,
        )
}

/**
 * 보낸 항목만 바꾼다.
 * 설명 둘은 `null` 을 보내 지울 수 있어야 하므로 키가 없는 경우와 구분하려고 [JsonNode] 로 받는다.
 */
data class SessionUpdateRequest(
    @field:Schema(type = "string")
    val title: String? = null,
    val startsAt: OffsetDateTime? = null,
    @field:Schema(type = "string")
    val location: String? = null,
    @field:Schema(type = "string")
    val assignmentTitle: String? = null,
    @field:Schema(type = "string", nullable = true, description = "null 을 보내면 지운다.")
    val lectureContent: JsonNode? = null,
    @field:Schema(type = "string", nullable = true, description = "null 을 보내면 지운다.")
    val assignmentContent: JsonNode? = null,
) {
    fun toCommand(): SessionUpdateCommand =
        SessionUpdateCommand(
            title = title?.requireNotBlank("title"),
            startsAt = startsAt?.toInstant(),
            location = location?.requireNotBlank("location"),
            assignmentTitle = assignmentTitle?.requireNotBlank("assignmentTitle"),
            lectureContent = lectureContent.readPatch { it.asString() },
            assignmentContent = assignmentContent.readPatch { it.asString() },
        )

    private fun String.requireNotBlank(field: String): String =
        takeIf { it.isNotBlank() } ?: throw InvalidRequestException(field, "공백일 수 없습니다.")
}

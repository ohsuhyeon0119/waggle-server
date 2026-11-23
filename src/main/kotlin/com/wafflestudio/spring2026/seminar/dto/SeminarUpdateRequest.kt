package com.wafflestudio.spring2026.seminar.dto

import com.wafflestudio.spring2026.common.error.InvalidRequestException
import com.wafflestudio.spring2026.common.web.PatchField
import com.wafflestudio.spring2026.common.web.readPatch
import com.wafflestudio.spring2026.seminar.service.SeminarUpdateCommand
import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode

/**
 * 보낸 항목만 바꾼다. `title` 과 `description` 외의 필드는 무시한다.
 *
 * `description` 은 `null` 을 보내 지울 수 있어야 하므로, 키를 보내지 않은 것과 구분하려고
 * 두 필드를 [JsonNode] 로 받는다. ([readPatch])
 */
data class SeminarUpdateRequest(
    @field:Schema(type = "string", description = "세미나 제목. 공백이나 null 은 보낼 수 없다.")
    val title: JsonNode? = null,
    @field:Schema(type = "string", nullable = true, description = "Markdown 소개. null 을 보내면 지운다.")
    val description: JsonNode? = null,
) {
    fun toCommand(): SeminarUpdateCommand =
        SeminarUpdateCommand(
            title = readTitle(),
            description = description.readPatch { it.asString() },
        )

    private fun readTitle(): String? =
        when (val patch = title.readPatch { it.asString() }) {
            PatchField.Absent -> null
            PatchField.Cleared -> throw InvalidRequestException("title", "세미나 제목은 null 일 수 없습니다.")
            is PatchField.Updated ->
                patch.value.takeIf { it.isNotBlank() }
                    ?: throw InvalidRequestException("title", "세미나 제목은 공백일 수 없습니다.")
        }
}

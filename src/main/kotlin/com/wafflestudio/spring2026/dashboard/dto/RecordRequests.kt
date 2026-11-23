package com.wafflestudio.spring2026.dashboard.dto

import com.wafflestudio.spring2026.common.error.InvalidRequestException
import com.wafflestudio.spring2026.common.web.readPatch
import com.wafflestudio.spring2026.dashboard.model.AttendanceStatus
import com.wafflestudio.spring2026.dashboard.model.SubmissionStatus
import com.wafflestudio.spring2026.dashboard.service.RecordChange
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode

/**
 * `PATCH /sessions/{sessionId}/records`. 한 회차에서 여러 수강생의 기록을 한 번에 고친다.
 * 형식이 하나라도 틀리면 DB 에 닿기 전에 요청 전체를 400 으로 거절한다.
 */
data class RecordsUpdateRequest(
    @field:ArraySchema(
        arraySchema = Schema(description = "한 개 이상. 같은 enrollmentId 는 한 번만.", requiredMode = Schema.RequiredMode.REQUIRED),
        minItems = 1,
    )
    val records: List<RecordUpdateItem?>? = null,
) {
    fun toChanges(): List<RecordChange> {
        if (records.isNullOrEmpty()) {
            throw InvalidRequestException("records", "한 개 이상의 기록이 필요합니다.")
        }

        val changes =
            records.mapIndexed { index, item ->
                item?.toChange("records[$index]") ?: throw InvalidRequestException("records[$index]", "객체여야 합니다.")
            }

        changes
            .groupingBy { it.enrollmentId }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
            ?.let { throw InvalidRequestException("records", "enrollmentId ${it.key}가 두 번 이상 있습니다.") }

        return changes
    }
}

/**
 * 생략한 항목은 기존 값을 유지하고, `null` 을 보낸 항목은 기록을 지운다.
 * 둘을 구분해야 하므로 [JsonNode] 로 받는다. ([com.wafflestudio.spring2026.common.web.PatchField])
 */
data class RecordUpdateItem(
    @field:Schema(type = "integer", format = "int64", description = "이 회차와 같은 세미나의 수강 신청 ID")
    val enrollmentId: JsonNode? = null,
    @field:Schema(
        type = "string",
        nullable = true,
        allowableValues = ["PRESENT", "ABSENT"],
        description = "생략하면 그대로 둔다. null 이면 기록을 지운다.",
    )
    val attendance: JsonNode? = null,
    @field:Schema(
        type = "string",
        nullable = true,
        allowableValues = ["PASSED", "SUBMITTED_BUT_FAILED", "NOT_SUBMITTED"],
        description = "생략하면 그대로 둔다. null 이면 기록을 지운다.",
    )
    val assignmentStatus: JsonNode? = null,
) {
    fun toChange(path: String): RecordChange {
        val id =
            enrollmentId
                ?.takeIf { (it.isInt || it.isLong) && it.longValue() > 0 }
                ?.longValue()
                ?: throw InvalidRequestException("$path.enrollmentId", "양의 정수여야 합니다.")

        if (attendance == null && assignmentStatus == null) {
            throw InvalidRequestException(path, "attendance 와 assignmentStatus 중 하나 이상을 보내야 합니다.")
        }

        return RecordChange(
            enrollmentId = id,
            attendance = attendance.readPatch { it.toEnum<AttendanceStatus>("$path.attendance") },
            assignmentStatus = assignmentStatus.readPatch { it.toEnum<SubmissionStatus>("$path.assignmentStatus") },
        )
    }

    /** 대소문자까지 정확히 같은 이름만 받는다. 숫자나 boolean 을 문자열로 바꿔 읽지 않는다. */
    private inline fun <reified E : Enum<E>> JsonNode.toEnum(field: String): E =
        takeIf { it.isString }
            ?.asString()
            ?.let { value -> enumValues<E>().firstOrNull { it.name == value } }
            ?: throw InvalidRequestException(field, "${enumValues<E>().joinToString()} 또는 null 이어야 합니다.")
}

package com.wafflestudio.spring2026.enrollment.dto

import com.wafflestudio.spring2026.common.error.InvalidRequestException
import io.swagger.v3.oas.annotations.media.Schema
import tools.jackson.databind.JsonNode

/**
 * Grace Day·탈락 칸 편집. 보낸 항목만 바꾸고, 명세에 없는 프로퍼티는 무시한다.
 *
 * 두 항목 모두 `null` 을 받지 않고, `1.5` 나 `1` 처럼 타입이 다른 값을 바꿔 읽지도 않는다.
 * Jackson 이 그런 값을 조용히 변환하지 않게 [JsonNode] 로 받아 직접 확인한다.
 */
data class EnrollmentUpdateRequest(
    @field:Schema(type = "integer", description = "잔여 Grace Day. 0 이상, 세미나의 totalGraceDays 이하. null 불가.")
    val graceDaysRemaining: JsonNode? = null,
    @field:Schema(type = "boolean", description = "탈락 여부. 표시용이며 다른 기능에 영향을 주지 않는다. null 불가.")
    val dropped: JsonNode? = null,
) {
    fun toCommand(): EnrollmentUpdateCommand {
        if (graceDaysRemaining == null && dropped == null) {
            throw InvalidRequestException("graceDaysRemaining", "graceDaysRemaining 과 dropped 중 하나 이상을 보내야 합니다.")
        }

        return EnrollmentUpdateCommand(
            graceDaysRemaining =
                graceDaysRemaining?.let { node ->
                    if (!node.isInt) throw InvalidRequestException("graceDaysRemaining", "정수여야 합니다.")
                    node.intValue()
                },
            dropped =
                dropped?.let { node ->
                    if (!node.isBoolean) throw InvalidRequestException("dropped", "true 또는 false 여야 합니다.")
                    node.booleanValue()
                },
        )
    }
}

/** 보내지 않은 항목은 `null` 이다. 요청에서 `null` 을 보내는 것은 [EnrollmentUpdateRequest] 가 이미 막았다. */
data class EnrollmentUpdateCommand(
    val graceDaysRemaining: Int?,
    val dropped: Boolean?,
)

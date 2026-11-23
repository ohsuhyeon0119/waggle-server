package com.wafflestudio.spring2026.seminar.service

import com.wafflestudio.spring2026.common.web.PatchField
import java.time.Instant

data class SeminarCreateCommand(
    val title: String,
    val description: String?,
    val capacity: Int,
    val applyStartAt: Instant,
    val applyEndAt: Instant,
    val totalGraceDays: Int,
)

data class SeminarUpdateCommand(
    /** `null` 이면 제목을 그대로 둔다. 제목은 지울 수 없으므로 세 가지 상태가 필요 없다. */
    val title: String?,
    val description: PatchField<String>,
)

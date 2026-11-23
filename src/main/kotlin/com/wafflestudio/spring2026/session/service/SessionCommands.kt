package com.wafflestudio.spring2026.session.service

import com.wafflestudio.spring2026.common.web.PatchField
import java.time.Instant

data class SessionCreateCommand(
    val title: String,
    val startsAt: Instant,
    val location: String,
    val assignmentTitle: String,
    val lectureContent: String?,
    val assignmentContent: String?,
)

/** 보낸 항목만 바꾼다. 설명 둘은 `null` 을 보내 지울 수 있어야 해서 [PatchField] 로 받는다. */
data class SessionUpdateCommand(
    val title: String?,
    val startsAt: Instant?,
    val location: String?,
    val assignmentTitle: String?,
    val lectureContent: PatchField<String>,
    val assignmentContent: PatchField<String>,
)

package com.wafflestudio.spring2026.common.web

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** 서비스가 날짜를 판단하고 응답에 표기하는 기준 시간대. */
val SERVICE_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

/** DB 에는 시점([Instant])으로 저장하고, 응답에는 ISO-8601 오프셋 일시로 내보낸다. */
fun Instant.toServiceTime(): OffsetDateTime = atZone(SERVICE_ZONE).toOffsetDateTime()

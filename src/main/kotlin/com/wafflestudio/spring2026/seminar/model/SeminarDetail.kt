package com.wafflestudio.spring2026.seminar.model

/** 세미나 조회 화면이 필요로 하는, 세미나 한 건과 그때그때 세는 값들. */
data class SeminarDetail(
    val seminar: Seminar,
    val enrolledCount: Int,
    val sessionCount: Int,
    val status: SeminarStatus,
)

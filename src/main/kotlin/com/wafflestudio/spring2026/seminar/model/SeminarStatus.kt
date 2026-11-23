package com.wafflestudio.spring2026.seminar.model

/** 세미나의 수강 신청 상태. 저장하지 않고 조회할 때마다 계산한다. */
enum class SeminarStatus {
    /** 신청 전 */
    BEFORE,

    /** 신청 중 */
    OPEN,

    /** 신청 마감 — 기간이 끝났거나 정원이 찼다. */
    CLOSED,
}

package com.wafflestudio.spring2026.enrollment.service

import com.wafflestudio.spring2026.enrollment.model.Enrollment
import com.wafflestudio.spring2026.seminar.model.SeminarDetail

/** 내 수강 목록 한 줄. 화면이 세미나 요약을 함께 보여 주므로 붙여서 내려 준다. */
data class MyEnrollment(
    val enrollment: Enrollment,
    val seminar: SeminarDetail,
)

package com.wafflestudio.spring2026.dashboard.model

import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.enrollment.model.Enrollment
import com.wafflestudio.spring2026.session.model.RoundedSession
import com.wafflestudio.spring2026.user.model.User

/** 행은 수강생, 열은 회차인 표. 페이지는 행에만 적용하고 회차는 언제나 전부 담는다. */
data class Dashboard(
    val seminarId: Long,
    val sessions: List<RoundedSession>,
    val rows: PageResponse<DashboardRow>,
)

/** [records] 에는 [Dashboard.sessions] 의 모든 회차가 같은 순서로 한 번씩 들어 있다. */
data class DashboardRow(
    val enrollment: Enrollment,
    val rookie: User,
    val records: List<RecordCell>,
)

package com.wafflestudio.spring2026.user.model

/** 가입 심사 상태. `PENDING` 에서만 `APPROVED` 또는 `REJECTED` 로 옮겨 갈 수 있다. */
enum class UserStatus {
    PENDING,
    APPROVED,
    REJECTED,

    /**
     * 탈퇴. 사용자 행을 지우는 대신 이 상태로 표시한다.
     * 지난 세미나의 수강 기록과 출석·과제 기록이 함께 사라지면 안 되기 때문이다.
     */
    WITHDRAWN,
}

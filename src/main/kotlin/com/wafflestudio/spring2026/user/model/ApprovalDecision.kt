package com.wafflestudio.spring2026.user.model

/** 가입 심사의 결론. `PENDING` 은 심사 결과가 될 수 없으므로 [UserStatus] 와 따로 둔다. */
enum class ApprovalDecision(
    val resultStatus: UserStatus,
) {
    APPROVED(UserStatus.APPROVED),
    REJECTED(UserStatus.REJECTED),
}

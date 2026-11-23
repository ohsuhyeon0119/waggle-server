package com.wafflestudio.spring2026.user.dto

import com.wafflestudio.spring2026.user.model.ApprovalDecision

data class UserApprovalRequest(
    val status: ApprovalDecision,
)

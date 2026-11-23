package com.wafflestudio.spring2026.user.model

import com.wafflestudio.spring2026.seminar.model.Seminar
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jdbc.core.mapping.AggregateReference
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("users")
data class User(
    @Id
    val id: Long? = null,
    val email: String,
    /** BCrypt 해시. 평문은 어디에도 남기지 않는다. */
    val password: String,
    val name: String,
    val githubUsername: String,
    val role: Role,
    val status: UserStatus = UserStatus.PENDING,
    /** 운영진이 담당하는 세미나. 와장과 루키는 비어 있다. */
    val seminarId: AggregateReference<Seminar, Long>? = null,
    @CreatedDate
    val createdAt: Instant = Instant.EPOCH,
    @LastModifiedDate
    val updatedAt: Instant = Instant.EPOCH,
) {
    fun requireId(): Long = requireNotNull(id) { "아직 저장되지 않은 사용자입니다." }

    /** 심사는 대기 중인 신청에만 할 수 있고, 한 번 내려진 결론은 되돌릴 수 없다. */
    fun review(decision: ApprovalDecision): User {
        if (status != UserStatus.PENDING) {
            throw UserNotPendingException(requireId(), status)
        }

        return copy(status = decision.resultStatus)
    }

    /** 이름과 GitHub 사용자명만 본인이 고칠 수 있다. 역할과 이메일은 가입 때 정해진 대로 둔다. */
    fun editProfile(
        name: String?,
        githubUsername: String?,
    ): User = copy(name = name ?: this.name, githubUsername = githubUsername ?: this.githubUsername)

    fun withdraw(): User {
        if (role == Role.ADMIN) {
            throw AdminCannotWithdrawException()
        }

        return copy(status = UserStatus.WITHDRAWN)
    }

    companion object {
        fun signUp(
            email: String,
            encodedPassword: String,
            name: String,
            githubUsername: String,
            role: Role,
            seminarId: Long?,
        ): User =
            User(
                email = email,
                password = encodedPassword,
                name = name,
                githubUsername = githubUsername,
                role = role,
                status = UserStatus.PENDING,
                seminarId = seminarId?.let { AggregateReference.to(it) },
            )
    }
}

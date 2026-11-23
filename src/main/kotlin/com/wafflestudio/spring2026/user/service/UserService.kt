package com.wafflestudio.spring2026.user.service

import com.wafflestudio.spring2026.common.auth.AccessToken
import com.wafflestudio.spring2026.common.auth.JwtProvider
import com.wafflestudio.spring2026.common.auth.RevokedTokenRepository
import com.wafflestudio.spring2026.common.auth.requireRole
import com.wafflestudio.spring2026.common.error.ForbiddenException
import com.wafflestudio.spring2026.common.error.InvalidCredentialsException
import com.wafflestudio.spring2026.common.error.InvalidRequestException
import com.wafflestudio.spring2026.common.web.PageRequest
import com.wafflestudio.spring2026.common.web.PageResponse
import com.wafflestudio.spring2026.enrollment.service.EnrollmentService
import com.wafflestudio.spring2026.seminar.model.SeminarNotFoundException
import com.wafflestudio.spring2026.seminar.repository.SeminarRepository
import com.wafflestudio.spring2026.user.model.ApprovalDecision
import com.wafflestudio.spring2026.user.model.EmailAlreadyExistsException
import com.wafflestudio.spring2026.user.model.Role
import com.wafflestudio.spring2026.user.model.User
import com.wafflestudio.spring2026.user.model.UserNotFoundException
import com.wafflestudio.spring2026.user.model.UserStatus
import com.wafflestudio.spring2026.user.repository.UserRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val seminarRepository: SeminarRepository,
    private val enrollmentService: EnrollmentService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val revokedTokenRepository: RevokedTokenRepository,
    private val clock: Clock,
) {
    @Transactional
    fun signUp(command: SignupCommand): User {
        if (userRepository.existsByEmail(command.email)) {
            throw EmailAlreadyExistsException(command.email)
        }

        command.seminarId?.let { seminarId ->
            if (!seminarRepository.existsById(seminarId)) {
                throw SeminarNotFoundException(seminarId)
            }
        }

        return userRepository.save(
            User.signUp(
                email = command.email,
                encodedPassword = requireNotNull(passwordEncoder.encode(command.password)),
                name = command.name,
                githubUsername = command.githubUsername,
                role = command.role,
                seminarId = command.seminarId,
            ),
        )
    }

    /**
     * 대기·반려 상태여도 로그인은 된다. 클라이언트가 상태를 보고 안내 화면을 띄운다.
     * 탈퇴한 계정만 다시 들어올 수 없다.
     */
    fun login(
        email: String,
        rawPassword: String,
    ): String {
        val user = userRepository.findByEmail(email) ?: throw InvalidCredentialsException()
        if (!passwordEncoder.matches(rawPassword, user.password) || user.status == UserStatus.WITHDRAWN) {
            throw InvalidCredentialsException()
        }

        return jwtProvider.issue(user.requireId())
    }

    /** 요청에 쓴 토큰 하나만 무효화한다. 같은 사용자가 다른 곳에서 받은 토큰은 그대로 쓸 수 있다. */
    @Transactional
    fun logout(token: AccessToken) {
        try {
            revokedTokenRepository.insert(
                tokenId = token.tokenId,
                userId = token.userId,
                expiresAt = token.expiresAt,
                createdAt = clock.instant(),
            )
        } catch (exception: DuplicateKeyException) {
            // 같은 토큰으로 동시에 로그아웃하면 둘 다 인증을 통과할 수 있다. 이미 무효이므로 목적은 이루었다.
        }
    }

    fun getUser(userId: Long): User = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)

    /** 가입 심사 목록. 운영진은 루키 신청만 볼 수 있고, 와장은 둘 다 본다. */
    fun listApplicants(
        reviewer: User,
        status: UserStatus?,
        role: Role?,
        page: PageRequest,
    ): PageResponse<User> {
        reviewer.requireRole(Role.ADMIN, Role.STAFF)
        // 와장은 가입 신청을 하지 않으므로 거를 수 있는 역할이 아니다.
        if (role == Role.ADMIN) throw InvalidRequestException("role", "STAFF 또는 ROOKIE 중 하나여야 합니다.")

        val visibleRole = if (reviewer.role == Role.STAFF) Role.ROOKIE else role

        return PageResponse.of(
            content = userRepository.findApplicants(visibleRole?.name, status?.name, page.size, page.offset),
            request = page,
            totalElements = userRepository.countApplicants(visibleRole?.name, status?.name),
        )
    }

    /** 운영진 가입 신청은 와장만 심사할 수 있다. 루키 신청은 운영진도 심사한다. */
    @Transactional
    fun review(
        reviewer: User,
        userId: Long,
        decision: ApprovalDecision,
    ): User {
        reviewer.requireRole(Role.ADMIN, Role.STAFF)

        val applicant = getUser(userId)
        if (applicant.role == Role.STAFF && reviewer.role != Role.ADMIN) {
            throw ForbiddenException("운영진 가입 신청은 와장만 심사할 수 있습니다.")
        }

        return userRepository.save(applicant.review(decision))
    }

    @Transactional
    fun updateProfile(
        user: User,
        name: String?,
        githubUsername: String?,
    ): User = userRepository.save(user.editProfile(name, githubUsername))

    /**
     * 탈퇴. 아직 신청 기간이 진행 중인 세미나의 신청은 출석·과제 기록과 함께 취소해서 자리를 돌려주고,
     * 이미 신청이 끝난 세미나의 수강 기록은 그대로 둔다. 취소와 상태 변경은 한 트랜잭션이다.
     * 신청 취소가 세미나를 먼저 잠그므로 사용자 행은 그 뒤에 고친다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun withdraw(user: User) {
        enrollmentService.cancelOpenEnrollmentsOf(user.requireId())

        userRepository.save(user.withdraw())
    }
}

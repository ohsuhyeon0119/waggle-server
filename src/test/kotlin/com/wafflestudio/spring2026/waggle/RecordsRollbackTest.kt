package com.wafflestudio.spring2026.waggle

import com.wafflestudio.spring2026.common.web.PatchField
import com.wafflestudio.spring2026.dashboard.model.AttendanceStatus
import com.wafflestudio.spring2026.dashboard.model.SubmissionRecord
import com.wafflestudio.spring2026.dashboard.model.SubmissionStatus
import com.wafflestudio.spring2026.dashboard.repository.AttendanceRepository
import com.wafflestudio.spring2026.dashboard.repository.SubmissionRepository
import com.wafflestudio.spring2026.dashboard.service.DashboardService
import com.wafflestudio.spring2026.dashboard.service.RecordChange
import com.wafflestudio.spring2026.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * 4주차 팀 자체 테스트의 "저장 도중 롤백". 공개 테스트로는 확인할 수 없는 부분이다.
 *
 * 잘못된 요청이 400 으로 끝나는 것만 보면, 검증이 저장 전에 끝나 DB 쓰기가 한 번도 없었을 수 있다.
 * 그래서 실제 MySQL 에 기록을 몇 번 쓴 뒤 다음 저장에서 예외를 던지고, 앞서 쓴 기록까지 되돌아가는지 본다.
 * 실패는 운영 코드를 고치지 않고 테스트에서 과제 기록 저장소를 감싸 주입한다.
 */
@Import(RecordsRollbackTest.FailureInjection::class)
class RecordsRollbackTest : WaggleIntegrationTest() {
    @Autowired
    private lateinit var dashboardService: DashboardService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var attendances: AttendanceRepository

    @Autowired
    private lateinit var submissions: FailingSubmissionRepository

    private data class Cell(
        val attendance: AttendanceStatus?,
        val assignmentStatus: SubmissionStatus?,
    )

    private class InjectedFailure(
        val observedBeforeFailure: Map<Long, Cell>,
    ) : RuntimeException("테스트가 주입한 실패")

    @Test
    fun `일괄 기록 중 실제 DB 쓰기 뒤에 실패하면 이미 쓴 기록까지 모두 되돌린다`() {
        val seminarId = openSeminar()
        val sessionId = createSession(seminarId)
        val first = enrollmentOf(seminarId)
        val second = enrollmentOf(seminarId)
        val ids = listOf(first, second)

        // 1. 바꿀 값과 다른 기존 기록을 API 로 저장하고 커밋한다.
        authPatch(
            "/sessions/$sessionId/records",
            adminToken(),
            mapOf(
                "records" to
                    listOf(
                        mapOf("enrollmentId" to first, "attendance" to "PRESENT", "assignmentStatus" to "PASSED"),
                        mapOf("enrollmentId" to second, "attendance" to "ABSENT"),
                    ),
            ),
        ).andExpect { status { isNoContent() } }
        val before = cells(sessionId, ids)
        assertEquals(
            mapOf(first to Cell(AttendanceStatus.PRESENT, SubmissionStatus.PASSED), second to Cell(AttendanceStatus.ABSENT, null)),
            before,
        )

        // 2~3. 실제 Service 로 일괄 수정을 시작한다. 처리 순서는 first 의 출석 수정 → 과제 삭제 → second 의 출석 수정 →
        // second 의 과제 저장이다. 앞의 세 번은 DB 에 실제로 쓰고, 네 번째 저장에서 예외를 던진다.
        submissions.failNextSave { InjectedFailure(observedBeforeFailure = cells(sessionId, ids)) }
        val admin = requireNotNull(userRepository.findByEmail(ADMIN_EMAIL))

        // 4. 예외가 호출자에게 그대로 전달된다.
        val failure =
            assertFailsWith<InjectedFailure> {
                dashboardService.updateRecords(
                    admin,
                    sessionId,
                    listOf(
                        RecordChange(first, PatchField.Updated(AttendanceStatus.ABSENT), PatchField.Cleared),
                        RecordChange(
                            second,
                            PatchField.Updated(AttendanceStatus.PRESENT),
                            PatchField.Updated(SubmissionStatus.NOT_SUBMITTED),
                        ),
                    ),
                )
            }

        // 첫 쓰기가 실제로 실행되었다는 증거. 실패 직전, 같은 트랜잭션 안에서는 앞선 세 번의 쓰기가 보였다.
        assertEquals(
            mapOf(first to Cell(AttendanceStatus.ABSENT, null), second to Cell(AttendanceStatus.PRESENT, null)),
            failure.observedBeforeFailure,
        )
        assertNotEquals(before, failure.observedBeforeFailure)

        // 5. 실패한 트랜잭션이 끝난 뒤 새 트랜잭션에서 읽으면, 수정·삭제한 기록이 모두 작업 전 상태다.
        assertEquals(before, cells(sessionId, ids))
    }

    /** 승인된 루키 한 명을 세미나에 신청시키고 수강 신청 ID 를 돌려준다. */
    private fun enrollmentOf(seminarId: Long): Long =
        responseId(
            authPost("/seminars/$seminarId/enrollments", signUpApprovedRookie().token)
                .andExpect { status { isCreated() } },
        )

    /** 저장소를 직접 읽는다. 트랜잭션 밖에서 부르면 호출마다 새 트랜잭션에서 커밋된 값을 읽는다. */
    private fun cells(
        sessionId: Long,
        enrollmentIds: List<Long>,
    ): Map<Long, Cell> {
        val attendance = attendances.findAllBySessionIdAndEnrollmentIds(sessionId, enrollmentIds).associate { it.enrollmentId.id to it.status }
        val submission = submissions.findAllBySessionIdAndEnrollmentIds(sessionId, enrollmentIds).associate { it.enrollmentId.id to it.status }

        return enrollmentIds.associateWith { Cell(attendance[it], submission[it]) }
    }

    @TestConfiguration
    class FailureInjection {
        /** 운영 코드가 주입받는 과제 기록 저장소를 이 감싼 것으로 바꾼다. 실제 저장은 원래 저장소가 한다. */
        @Bean
        @Primary
        fun failingSubmissionRepository(
            @Qualifier("submissionRepository") real: SubmissionRepository,
        ): FailingSubmissionRepository = FailingSubmissionRepository(real)
    }
}

/** 지정한 다음 한 번의 `save` 에서만 예외를 던지고, 나머지는 실제 저장소에 그대로 맡긴다. */
class FailingSubmissionRepository(
    private val real: SubmissionRepository,
) : SubmissionRepository by real {
    @Volatile
    private var nextFailure: (() -> RuntimeException)? = null

    fun failNextSave(failure: () -> RuntimeException) {
        nextFailure = failure
    }

    override fun <S : SubmissionRecord> save(entity: S): S {
        nextFailure?.let { failure ->
            nextFailure = null
            throw failure()
        }

        return real.save(entity)
    }
}

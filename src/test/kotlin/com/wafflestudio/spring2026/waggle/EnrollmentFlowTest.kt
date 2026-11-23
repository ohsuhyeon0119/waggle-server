package com.wafflestudio.spring2026.waggle

import kotlin.test.Test

class EnrollmentFlowTest : WaggleIntegrationTest() {
    @Test
    fun `신청하면 내 수강 목록에 보이고 취소하면 자리가 빈다`() {
        val seminarId = openSeminar(capacity = 1, totalGraceDays = 4)
        val rookie = signUpApprovedRookie()

        authPost("/seminars/$seminarId/enrollments", rookie.token).andExpect {
            status { isCreated() }
            jsonPath("$.graceDaysRemaining") { value(4) }
            jsonPath("$.createdAt") { exists() }
        }

        authGet("/users/me/enrollments", rookie.token).andExpect {
            status { isOk() }
            jsonPath("$.totalElements") { value(1) }
            jsonPath("$.content[0].seminar.id") { value(seminarId) }
            jsonPath("$.content[0].seminar.enrolledCount") { value(1) }
        }

        // 정원이 차면 다른 루키는 들어올 수 없다.
        val other = signUpApprovedRookie()
        authPost("/seminars/$seminarId/enrollments", other.token).andExpect { status { isConflict() } }

        authDelete("/seminars/$seminarId/enrollments/me", rookie.token).andExpect { status { isNoContent() } }
        authPost("/seminars/$seminarId/enrollments", other.token).andExpect { status { isCreated() } }
    }

    @Test
    fun `같은 세미나를 두 번 신청할 수 없다`() {
        val seminarId = openSeminar()
        val rookie = signUpApprovedRookie()

        authPost("/seminars/$seminarId/enrollments", rookie.token).andExpect { status { isCreated() } }
        authPost("/seminars/$seminarId/enrollments", rookie.token).andExpect { status { isConflict() } }
    }

    @Test
    fun `신청 기간이 아닌 세미나는 신청도 취소도 할 수 없다`() {
        val futureSeminarId = openSeminar(applyStartAt = now().plusDays(1), applyEndAt = now().plusDays(2))
        val rookie = signUpApprovedRookie()

        authPost("/seminars/$futureSeminarId/enrollments", rookie.token).andExpect { status { isConflict() } }
        authDelete("/seminars/$futureSeminarId/enrollments/me", rookie.token).andExpect { status { isNotFound() } }
    }

    @Test
    fun `루키가 아닌 사용자의 수강 신청은 403을 반환한다`() {
        val seminarId = openSeminar()
        val staff = signUpApprovedStaff(seminarId)

        authPost("/seminars/$seminarId/enrollments", staff.token).andExpect { status { isForbidden() } }
        authPost("/seminars/$seminarId/enrollments", loginDemo("newstaff@snu.ac.kr")).andExpect { status { isForbidden() } }
    }

    @Test
    fun `승인된 루키가 아니면 수강 취소는 403을 반환한다`() {
        val seminarId = openSeminar()
        val staff = signUpApprovedStaff(seminarId)

        authDelete("/seminars/$seminarId/enrollments/me", staff.token).andExpect { status { isForbidden() } }
        authDelete("/seminars/$seminarId/enrollments/me", loginDemo("pending@snu.ac.kr")).andExpect { status { isForbidden() } }
    }

    @Test
    fun `탈퇴하면 진행 중인 신청만 기록과 함께 취소되고 다시 로그인할 수 없다`() {
        val seminarId = openSeminar()
        val rookie = signUpApprovedRookie()
        val enrollmentId = responseId(
            authPost("/seminars/$seminarId/enrollments", rookie.token).andExpect { status { isCreated() } },
        )
        // 기록이 남은 신청도 취소할 수 있어야 한다. 기록을 먼저 지우지 않으면 외래 키에 막힌다.
        val sessionId = createSession(seminarId)
        authPatch(
            "/sessions/$sessionId/records",
            adminToken(),
            mapOf("records" to listOf(mapOf("enrollmentId" to enrollmentId, "attendance" to "PRESENT"))),
        ).andExpect { status { isNoContent() } }

        authDelete("/users/me", rookie.token).andExpect { status { isNoContent() } }

        authGet("/seminars/$seminarId", adminToken()).andExpect {
            status { isOk() }
            jsonPath("$.enrolledCount") { value(0) }
        }
        authPost("/auth/login", body = mapOf("email" to rookie.email, "password" to demoPassword))
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `내 정보는 이름과 GitHub 사용자명만 고칠 수 있다`() {
        val rookie = signUpApprovedRookie()

        authPatch("/users/me", rookie.token, mapOf("name" to "새 이름")).andExpect {
            status { isOk() }
            jsonPath("$.name") { value("새 이름") }
            jsonPath("$.email") { value(rookie.email) }
        }
        authPatch("/users/me", rookie.token, """{"name":"   "}""").andExpect { status { isBadRequest() } }
    }
}

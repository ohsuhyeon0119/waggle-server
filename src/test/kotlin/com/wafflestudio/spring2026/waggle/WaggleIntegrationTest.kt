package com.wafflestudio.spring2026.waggle

import com.wafflestudio.spring2026.support.ApiIntegrationTest
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime

/**
 * 완성된 Waggle 서비스(인증·인가 포함)를 확인하는 테스트의 공통부.
 *
 * 컨테이너와 ObjectMapper 는 [ApiIntegrationTest] 것을 그대로 쓰고,
 * 로그인·토큰 헬퍼도 그쪽 것을 쓰고, 여기서는 시연 계정과 완성본 테스트에서 자주 쓰는 호출을 더한다.
 */
abstract class WaggleIntegrationTest : ApiIntegrationTest() {
    /** 마이그레이션이 넣어 둔 시연 계정. 모두 같은 비밀번호를 쓴다. */
    protected val demoPassword = "waggle1234"

    /** 시연 계정으로 로그인한다. 와장 로그인은 [ApiIntegrationTest.adminToken] 을 쓴다. */
    protected fun loginDemo(email: String): String = login(email, demoPassword)

    /** 가입 → 와장이 승인 → 로그인까지. 테스트마다 새 계정을 만들어 서로 간섭하지 않게 한다. */
    protected fun signUpApprovedRookie(): AuthUser = signUpApproved(role = "ROOKIE", seminarId = null)

    protected fun signUpApprovedStaff(seminarId: Long): AuthUser =
        signUpApproved(role = "STAFF", seminarId = seminarId)

    private fun signUpApproved(
        role: String,
        seminarId: Long?,
    ): AuthUser {
        val email = uniqueEmail()
        val signUp = mockMvc.post("/auth/signup") {
            json(
                mapOf(
                    "email" to email,
                    "password" to demoPassword,
                    "name" to unique("이름"),
                    "githubUsername" to unique("github"),
                    "role" to role,
                    "seminarId" to seminarId,
                ),
            )
        }.andExpect { status { isCreated() } }

        val id = responseId(signUp)
        mockMvc.patch("/users/$id/approval") {
            bearer(adminToken())
            json(mapOf("status" to "APPROVED"))
        }.andExpect { status { isOk() } }

        return AuthUser(id = id, email = email, token = loginDemo(email))
    }

    /** 와장이 세미나를 하나 연다. 기본값은 "지금 신청 중"인 세미나다. */
    protected fun openSeminar(
        capacity: Int = 10,
        totalGraceDays: Int = 3,
        applyStartAt: OffsetDateTime = now().minusDays(1),
        applyEndAt: OffsetDateTime = now().plusDays(1),
    ): Long {
        val result = mockMvc.post("/seminars") {
            bearer(adminToken())
            json(
                mapOf(
                    "title" to unique("세미나"),
                    "description" to "설명",
                    "capacity" to capacity,
                    "applyStartAt" to applyStartAt.toString(),
                    "applyEndAt" to applyEndAt.toString(),
                    "totalGraceDays" to totalGraceDays,
                ),
            )
        }.andExpect { status { isCreated() } }

        return responseId(result)
    }

    protected fun authGet(
        path: String,
        token: String? = null,
    ): ResultActionsDsl = mockMvc.get(path) { authorize(token, null) }

    protected fun authPost(
        path: String,
        token: String? = null,
        body: Any? = null,
    ): ResultActionsDsl = mockMvc.post(path) { authorize(token, body) }

    protected fun authPatch(
        path: String,
        token: String? = null,
        body: Any? = null,
    ): ResultActionsDsl = mockMvc.patch(path) { authorize(token, body) }

    protected fun authDelete(
        path: String,
        token: String? = null,
    ): ResultActionsDsl = mockMvc.delete(path) { authorize(token, null) }

    private fun MockHttpServletRequestDsl.authorize(
        token: String?,
        body: Any?,
    ) {
        if (token != null) bearer(token)
        if (body != null) json(body)
    }

    protected data class AuthUser(
        val id: Long,
        val email: String,
        val token: String,
    )
}

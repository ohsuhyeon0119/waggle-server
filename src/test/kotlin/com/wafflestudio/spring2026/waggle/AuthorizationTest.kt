package com.wafflestudio.spring2026.waggle

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.`is`
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.Date
import kotlin.test.Test

class AuthorizationTest : WaggleIntegrationTest() {
    private val sessionBody =
        mapOf(
            "title" to "1회차",
            "startsAt" to now().plusDays(1).toString(),
            "location" to "302동 208호",
            "assignmentTitle" to "과제",
        )

    @Test
    fun `로그인하면 토큰으로 내 정보를 볼 수 있다`() {
        val token = loginDemo("rookie@snu.ac.kr")

        authGet("/users/me", token).andExpect {
            status { isOk() }
            jsonPath("$.email") { value("rookie@snu.ac.kr") }
            jsonPath("$.role") { value("ROOKIE") }
            jsonPath("$.status") { value("APPROVED") }
        }
    }

    @Test
    fun `로그인 응답에는 accessToken 만 담긴다`() {
        authPost("/auth/login", body = mapOf("email" to "rookie@snu.ac.kr", "password" to demoPassword)).andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.user") { doesNotExist() }
        }
    }

    @Test
    fun `비밀번호가 틀리거나 가입하지 않은 이메일이면 401을 반환한다`() {
        authPost("/auth/login", body = mapOf("email" to "rookie@snu.ac.kr", "password" to "wrong"))
            .andExpect { status { isUnauthorized() } }
        authPost("/auth/login", body = mapOf("email" to "nobody@snu.ac.kr", "password" to demoPassword))
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `토큰이 없거나 유효하지 않으면 401을 반환한다`() {
        val valid = loginDemo("rookie@snu.ac.kr")
        val otherKey =
            Jwts
                .builder()
                .subject("3")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(ByteArray(32) { 7 }))
                .compact()

        authGet("/users/me").andExpect { status { isUnauthorized() } }
        mockMvc.get("/users/me") { header(HttpHeaders.AUTHORIZATION, "Token $valid") }
            .andExpect { status { isUnauthorized() } }
        authGet("/users/me", "not-a-token").andExpect { status { isUnauthorized() } }
        authGet("/users/me", otherKey).andExpect { status { isUnauthorized() } }
        authGet("/users/me", valid.dropLast(2) + "xx").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `인증 실패는 다른 검사보다 먼저 판단한다`() {
        authPost("/seminars/999999/sessions", body = mapOf("title" to "")).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `대기 상태 사용자는 자기 정보 말고는 쓸 수 없다`() {
        val pending = loginDemo("pending@snu.ac.kr")

        authGet("/users/me", pending).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("PENDING") }
        }
        authGet("/users/1", pending).andExpect { status { isForbidden() } }
        authGet("/seminars/1", pending).andExpect { status { isForbidden() } }
        authGet("/users/me/enrollments", pending).andExpect { status { isForbidden() } }
        authPost("/seminars/2/enrollments", pending).andExpect { status { isForbidden() } }
    }

    @Test
    fun `세미나 목록은 로그인 없이, 단건 조회는 승인된 사용자만 볼 수 있다`() {
        authGet("/seminars").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(greaterThan(0)) }
        }
        authGet("/seminars/1").andExpect { status { isUnauthorized() } }
        authGet("/seminars/1", loginDemo("rookie@snu.ac.kr")).andExpect { status { isOk() } }
    }

    @Test
    fun `세미나 개설은 와장만 할 수 있다`() {
        val seminarId = openSeminar()
        val body =
            mapOf(
                "title" to "세미나",
                "capacity" to 10,
                "applyStartAt" to now().toString(),
                "applyEndAt" to now().plusDays(1).toString(),
                "totalGraceDays" to 1,
            )

        authPost("/seminars", signUpApprovedRookie().token, body).andExpect { status { isForbidden() } }
        authPost("/seminars", signUpApprovedStaff(seminarId).token, body).andExpect { status { isForbidden() } }
    }

    @Test
    fun `세미나 수정은 와장과 담당 운영진만 할 수 있다`() {
        val seminarId = openSeminar()
        val otherSeminarId = openSeminar()
        val patch = mapOf("title" to "바뀐 제목")

        authPatch("/seminars/$seminarId", signUpApprovedStaff(seminarId).token, patch).andExpect { status { isOk() } }
        authPatch("/seminars/$seminarId", adminToken(), patch).andExpect { status { isOk() } }
        authPatch("/seminars/$seminarId", signUpApprovedStaff(otherSeminarId).token, patch)
            .andExpect { status { isForbidden() } }
        authPatch("/seminars/$seminarId", signUpApprovedRookie().token, patch).andExpect { status { isForbidden() } }
        // 시드의 대기 중인 운영진은 2번 세미나 담당이다.
        authPatch("/seminars/2", loginDemo("newstaff@snu.ac.kr"), patch).andExpect { status { isForbidden() } }
    }

    @Test
    fun `회차 등록과 수정은 와장과 담당 운영진만 할 수 있다`() {
        val mySeminarId = openSeminar()
        val otherSeminarId = openSeminar()
        val staff = signUpApprovedStaff(mySeminarId)

        val sessionId =
            responseId(authPost("/seminars/$mySeminarId/sessions", staff.token, sessionBody).andExpect { status { isCreated() } })
        authPost("/seminars/$mySeminarId/sessions", adminToken(), sessionBody).andExpect { status { isCreated() } }
        authPost("/seminars/$otherSeminarId/sessions", staff.token, sessionBody).andExpect { status { isForbidden() } }
        authPost("/seminars/$mySeminarId/sessions", signUpApprovedRookie().token, sessionBody)
            .andExpect { status { isForbidden() } }

        authPatch("/sessions/$sessionId", signUpApprovedStaff(otherSeminarId).token, mapOf("title" to "바뀐 제목"))
            .andExpect { status { isForbidden() } }
        authPatch("/sessions/$sessionId", staff.token, mapOf("title" to "바뀐 제목")).andExpect { status { isOk() } }
    }

    @Test
    fun `회차는 와장, 담당 운영진, 수강 중인 루키만 볼 수 있다`() {
        val seminarId = openSeminar()
        val staff = signUpApprovedStaff(seminarId)
        val sessionId =
            responseId(authPost("/seminars/$seminarId/sessions", staff.token, sessionBody).andExpect { status { isCreated() } })
        val enrolled = signUpApprovedRookie()
        authPost("/seminars/$seminarId/enrollments", enrolled.token).andExpect { status { isCreated() } }

        for (token in listOf(adminToken(), staff.token, enrolled.token)) {
            authGet("/seminars/$seminarId/sessions", token).andExpect { status { isOk() } }
            authGet("/sessions/$sessionId", token).andExpect { status { isOk() } }
        }

        val outsiders = listOf(signUpApprovedRookie().token, signUpApprovedStaff(openSeminar()).token)
        for (token in outsiders) {
            authGet("/seminars/$seminarId/sessions", token).andExpect { status { isForbidden() } }
            authGet("/sessions/$sessionId", token).andExpect { status { isForbidden() } }
        }
    }

    @Test
    fun `운영진 가입 신청은 와장만 심사할 수 있다`() {
        val seminarId = openSeminar()
        val reviewer = signUpApprovedStaff(seminarId)

        val applicantId =
            responseId(
                authPost(
                    "/auth/signup",
                    body =
                        mapOf(
                            "email" to uniqueEmail(),
                            "password" to demoPassword,
                            "name" to "운영진 지원자",
                            "githubUsername" to unique("github"),
                            "role" to "STAFF",
                            "seminarId" to seminarId,
                        ),
                ).andExpect { status { isCreated() } },
            )

        authPatch("/users/$applicantId/approval", reviewer.token, mapOf("status" to "APPROVED"))
            .andExpect { status { isForbidden() } }
        authPatch("/users/$applicantId/approval", signUpApprovedRookie().token, mapOf("status" to "APPROVED"))
            .andExpect { status { isForbidden() } }
        authPatch("/users/$applicantId/approval", adminToken(), mapOf("status" to "APPROVED"))
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("APPROVED") }
            }
    }

    @Test
    fun `가입 신청 목록은 와장과 운영진만 보고, 운영진에게는 루키 신청만 보인다`() {
        val staff = signUpApprovedStaff(openSeminar())

        authGet("/users?role=STAFF", staff.token).andExpect {
            status { isOk() }
            jsonPath("$.content[*].role") { value(everyItem(`is`("ROOKIE"))) }
        }
        authGet("/users", signUpApprovedRookie().token).andExpect { status { isForbidden() } }
        authGet("/users?status=UNKNOWN", adminToken()).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `내 수강 목록은 루키만 볼 수 있다`() {
        authGet("/users/me/enrollments", signUpApprovedStaff(openSeminar()).token).andExpect { status { isForbidden() } }
        authGet("/users/me/enrollments", signUpApprovedRookie().token).andExpect { status { isOk() } }
    }
}

package com.wafflestudio.spring2026.waggle

import kotlin.test.Test

/** 공개 테스트가 다루지 않는 회차 삭제·수정. 대시보드와 기록 API 는 4주차 공개 테스트가 확인한다. */
class DashboardFlowTest : WaggleIntegrationTest() {
    private fun recordAttendance(
        sessionId: Long,
        enrollmentId: Long,
        token: String,
        attendanceJson: String,
    ) {
        authPatch(
            "/sessions/$sessionId/records",
            token,
            """{"records":[{"enrollmentId":$enrollmentId,"attendance":$attendanceJson}]}""",
        ).andExpect { status { isNoContent() } }
    }

    @Test
    fun `회차는 마지막 것부터, 기록이 없을 때만 지울 수 있다`() {
        val seminarId = openSeminar()
        val staff = signUpApprovedStaff(seminarId)
        val rookie = signUpApprovedRookie()
        val enrollmentId = responseId(
            authPost("/seminars/$seminarId/enrollments", rookie.token).andExpect { status { isCreated() } },
        )

        fun addSession(days: Long) = responseId(
            authPost(
                "/seminars/$seminarId/sessions",
                staff.token,
                mapOf(
                    "title" to "회차",
                    "startsAt" to now().plusDays(days).toString(),
                    "location" to "302동",
                    "assignmentTitle" to "과제",
                ),
            ).andExpect { status { isCreated() } },
        )

        val first = addSession(1)
        val second = addSession(2)

        authDelete("/sessions/$first", staff.token).andExpect { status { isConflict() } }

        recordAttendance(second, enrollmentId, staff.token, "\"PRESENT\"")
        authDelete("/sessions/$second", staff.token).andExpect { status { isConflict() } }

        recordAttendance(second, enrollmentId, staff.token, "null")
        authDelete("/sessions/$second", staff.token).andExpect { status { isNoContent() } }
        authDelete("/sessions/$first", staff.token).andExpect { status { isNoContent() } }
    }

    @Test
    fun `회차 수정은 보낸 항목만 바꾸고 설명은 null 로 지운다`() {
        val seminarId = openSeminar()
        val staff = signUpApprovedStaff(seminarId)
        val sessionId = responseId(
            authPost(
                "/seminars/$seminarId/sessions",
                staff.token,
                mapOf(
                    "title" to "원래 제목",
                    "startsAt" to now().plusDays(1).toString(),
                    "location" to "302동",
                    "assignmentTitle" to "과제",
                    "lectureContent" to "## 수업",
                ),
            ).andExpect { status { isCreated() } },
        )

        authPatch("/sessions/$sessionId", staff.token, """{"title":"새 제목"}""").andExpect {
            status { isOk() }
            jsonPath("$.title") { value("새 제목") }
            jsonPath("$.lectureContent") { value("## 수업") }
        }
        authPatch("/sessions/$sessionId", staff.token, """{"lectureContent":null}""").andExpect {
            status { isOk() }
            jsonPath("$.title") { value("새 제목") }
            jsonPath("$.lectureContent") { value(null) }
        }
    }
}

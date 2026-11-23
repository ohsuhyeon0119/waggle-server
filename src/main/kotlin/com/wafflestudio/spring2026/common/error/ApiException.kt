package com.wafflestudio.spring2026.common.error

import org.springframework.http.HttpStatus

/**
 * API 로 그대로 내보낼 수 있는 예외.
 *
 * 상태 코드와 오류 코드를 예외 자신이 들고 있으므로,
 * [GlobalExceptionHandler] 는 예외 종류마다 핸들러를 늘리지 않고 한 곳에서 변환한다.
 */
abstract class ApiException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    val fieldErrors: List<FieldErrorResponse> = emptyList(),
) : RuntimeException(message)

/**
 * 애너테이션만으로는 표현하기 어려운 요청값 규칙을 어겼을 때 던진다.
 * 400 응답의 `code` 는 언제나 `INVALID_REQUEST` 이고, 구체적인 사유는 `fieldErrors` 로만 전달한다.
 */
class InvalidRequestException(
    field: String,
    reason: String,
) : ApiException(
        status = HttpStatus.BAD_REQUEST,
        code = "INVALID_REQUEST",
        message = "요청값이 올바르지 않습니다.",
        fieldErrors = listOf(FieldErrorResponse(field, reason)),
    )

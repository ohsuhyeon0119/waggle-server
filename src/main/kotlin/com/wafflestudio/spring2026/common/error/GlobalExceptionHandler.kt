package com.wafflestudio.spring2026.common.error

import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        exception: ApiException,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.status(exception.status).body(
            ApiErrorResponse(
                code = exception.code,
                message = exception.message,
                fieldErrors = exception.fieldErrors,
            ),
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException,
    ): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors.map { error ->
            FieldErrorResponse(
                field = error.field,
                message = error.defaultMessage ?: "잘못된 값입니다.",
            )
        }

        return ResponseEntity.badRequest().body(
            ApiErrorResponse(
                code = "INVALID_REQUEST",
                message = "요청값이 올바르지 않습니다.",
                fieldErrors = fieldErrors,
            ),
        )
    }

    /** 경로·쿼리 값의 타입이 맞지 않는 경우. 예: `GET /users?status=UNKNOWN`, `GET /seminars/abc` */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                code = "INVALID_REQUEST",
                message = "요청값이 올바르지 않습니다.",
                fieldErrors = listOf(FieldErrorResponse(exception.name, "올바른 값이 아닙니다.")),
            ),
        )

    /** 본문이 JSON 이 아니거나, 필수 값이 빠졌거나, 열거형에 없는 값이 온 경우. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        exception: HttpMessageNotReadableException,
    ): ResponseEntity<ApiErrorResponse> =
        ResponseEntity.badRequest().body(
            ApiErrorResponse(
                code = "INVALID_REQUEST",
                message = "요청 본문을 읽을 수 없습니다.",
            ),
        )
}

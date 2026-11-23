package com.wafflestudio.spring2026.common.web

import com.wafflestudio.spring2026.common.error.InvalidRequestException
import io.swagger.v3.oas.annotations.media.Schema

/** 목록 응답의 공통 형태. */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    fun <R> map(transform: (T) -> R): PageResponse<R> =
        PageResponse(content.map(transform), page, size, totalElements, totalPages)

    companion object {
        fun <T> of(
            content: List<T>,
            request: PageRequest,
            totalElements: Long,
        ): PageResponse<T> =
            PageResponse(
                content = content,
                page = request.page,
                size = request.size,
                totalElements = totalElements,
                totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / request.size + 1).toInt(),
            )

        /** 이미 다 읽어 온 목록을 잘라 내보낸다. 원소가 적고 정렬 기준이 계산값일 때 쓴다. */
        fun <T> slice(
            all: List<T>,
            request: PageRequest,
        ): PageResponse<T> = of(all.drop(request.offset.toInt()).take(request.size), request, all.size.toLong())
    }
}

/** `page`(0부터)와 `size` 쿼리. 범위를 벗어난 값은 조용히 다듬는다. */
data class PageRequest(
    @field:Schema(description = "0부터 시작하는 페이지 번호") val page: Int = 0,
    @field:Schema(description = "한 페이지 크기") val size: Int = 20,
) {
    val offset: Long get() = page.toLong() * size

    init {
        require(page >= 0) { "page 는 0 이상이어야 합니다." }
    }

    companion object {
        private const val DEFAULT_SIZE = 20
        private const val MAX_SIZE = 100

        fun of(
            page: Int?,
            size: Int?,
        ): PageRequest = PageRequest(page = (page ?: 0).coerceAtLeast(0), size = (size ?: DEFAULT_SIZE).coerceIn(1, MAX_SIZE))

        /** 범위를 벗어난 값을 다듬지 않고 400 으로 거절한다. (4주차 과제 문서의 대시보드 조회) */
        fun strict(
            page: Int?,
            size: Int?,
        ): PageRequest {
            val requestedPage = page ?: 0
            val requestedSize = size ?: DEFAULT_SIZE
            if (requestedPage < 0) throw InvalidRequestException("page", "0 이상이어야 합니다.")
            if (requestedSize !in 1..MAX_SIZE) throw InvalidRequestException("size", "1 이상 $MAX_SIZE 이하여야 합니다.")

            return PageRequest(page = requestedPage, size = requestedSize)
        }
    }
}

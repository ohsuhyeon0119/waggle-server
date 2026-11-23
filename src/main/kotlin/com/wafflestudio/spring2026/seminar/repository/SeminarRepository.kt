package com.wafflestudio.spring2026.seminar.repository

import com.wafflestudio.spring2026.seminar.model.Seminar
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface SeminarRepository : CrudRepository<Seminar, Long> {
    /**
     * 제목 키워드로 거른다. 신청 상태는 저장하는 값이 아니라 조회 시점에 계산하므로
     * SQL 로 거르지 않고 서비스에서 걸러 낸다. 세미나 수는 많아야 수십 개다.
     */
    @Query("SELECT * FROM seminars WHERE :keyword IS NULL OR title LIKE CONCAT('%', :keyword, '%') ORDER BY id")
    fun findAllByKeyword(
        @Param("keyword") keyword: String?,
    ): List<Seminar>

    /**
     * 세미나 행을 쓰기 잠금하며 읽는다. 한 세미나의 수강 신청과 취소는 이 잠금을 차례로 얻어 하나씩 실행된다.
     * 잠금은 호출한 트랜잭션이 끝날 때 풀린다.
     */
    @Query("SELECT * FROM seminars WHERE id = :seminarId FOR UPDATE")
    fun findByIdForUpdate(
        @Param("seminarId") seminarId: Long,
    ): Seminar?
}

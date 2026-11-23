package com.wafflestudio.spring2026.session.repository

import com.wafflestudio.spring2026.session.model.SeminarSession
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface SeminarSessionRepository : CrudRepository<SeminarSession, Long> {
    /** 회차 번호 순서, 즉 `startsAt` 오름차순(같으면 ID 오름차순)으로 돌려준다. */
    @Query("SELECT * FROM seminar_sessions WHERE seminar_id = :seminarId ORDER BY starts_at, id")
    fun findAllBySeminarIdInRoundOrder(
        @Param("seminarId") seminarId: Long,
    ): List<SeminarSession>

    @Query("SELECT COUNT(*) FROM seminar_sessions WHERE seminar_id = :seminarId")
    fun countBySeminarId(
        @Param("seminarId") seminarId: Long,
    ): Int

    /** 출석·과제를 기록하는 동안 회차가 지워지지 않게 읽기 잠금을 건다. 기록끼리는 서로 막지 않는다. */
    @Query("SELECT * FROM seminar_sessions WHERE id = :sessionId FOR SHARE")
    fun findByIdForShare(
        @Param("sessionId") sessionId: Long,
    ): SeminarSession?

    /** 회차를 지우기 전에 잡는다. 진행 중인 기록이 끝나기를 기다린 뒤 기록이 남았는지 본다. */
    @Query("SELECT * FROM seminar_sessions WHERE id = :sessionId FOR UPDATE")
    fun findByIdForUpdate(
        @Param("sessionId") sessionId: Long,
    ): SeminarSession?
}

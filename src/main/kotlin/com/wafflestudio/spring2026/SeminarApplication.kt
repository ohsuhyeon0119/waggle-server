package com.wafflestudio.spring2026

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import java.time.ZoneOffset
import java.util.TimeZone

// 로그인은 JWT 로 직접 처리하므로 Spring Boot 가 만드는 기본 사용자(임의 비밀번호)는 필요 없다.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class SeminarApplication

fun main(args: Array<String>) {
    // MySQL 의 DATETIME 은 시간대를 담지 않고, JDBC 드라이버는 그 값을 JVM 기본 시간대의
    // 벽시계로 읽는다. 그래서 기본 시간대를 UTC 로 고정한다. 그러지 않으면 같은 행이
    // 개발 머신(KST)과 서버에서 다른 시각으로 읽힌다.
    // 응답에 쓰는 표시 시간대는 이것과 별개로 SERVICE_ZONE 이 정한다.
    TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))

    runApplication<SeminarApplication>(*args)
}

package com.wafflestudio.spring2026.common.persistence

import com.wafflestudio.spring2026.common.web.SERVICE_ZONE
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing
import java.time.Clock
import java.util.Optional

/**
 * `@CreatedDate`, `@LastModifiedDate` 를 Spring Data JDBC 가 채우도록 한다.
 * 시각은 [Clock] 한 곳에서만 얻으므로 감사 필드와 업무 규칙(신청 기간 판단)이 같은 기준을 쓴다.
 */
@Configuration
@EnableJdbcAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class PersistenceConfig {
    @Bean
    fun clock(): Clock = Clock.system(SERVICE_ZONE)

    @Bean
    fun auditingDateTimeProvider(clock: Clock): DateTimeProvider = DateTimeProvider { Optional.of(clock.instant()) }
}

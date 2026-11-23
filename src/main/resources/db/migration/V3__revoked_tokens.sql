-- 로그아웃으로 무효화된 Access Token. (3주차 과제 문서의 "로그아웃과 토큰 무효화")
-- 서버가 재시작되어도 무효여야 하므로 DB 에 둔다. 토큰 원문 대신 발급할 때 넣은 jti 만 저장한다.
-- expires_at 이 지난 행은 어차피 만료로 거절되므로, 나중에 지워도 결과가 달라지지 않는다.
CREATE TABLE revoked_tokens
(
    token_id   VARCHAR(36) NOT NULL COMMENT 'JWT 의 jti (UUID)',
    user_id    BIGINT      NOT NULL,
    expires_at DATETIME(6) NOT NULL COMMENT '토큰의 exp. 이 시각이 지나면 행을 지워도 된다',
    created_at DATETIME(6) NOT NULL COMMENT '로그아웃한 시각',
    PRIMARY KEY (token_id),
    CONSTRAINT fk_revoked_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

package com.wafflestudio.spring2026.user.model

/** 한 사용자는 하나의 역할만 가지며, 가입할 때 정해진 뒤 바뀌지 않는다. */
enum class Role {
    /** 와장 — 가입으로 만들어지지 않고 처음부터 DB 에 존재한다. */
    ADMIN,

    /** 운영진 — 정확히 하나의 세미나를 담당한다. */
    STAFF,

    /** 루키 — 여러 세미나를 수강할 수 있다. */
    ROOKIE,
}

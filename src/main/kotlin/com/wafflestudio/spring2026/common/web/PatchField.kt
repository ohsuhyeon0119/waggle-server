package com.wafflestudio.spring2026.common.web

import tools.jackson.databind.JsonNode

/**
 * PATCH 요청의 한 필드가 어떤 의도로 왔는지를 나타낸다.
 *
 * `{"description": null}` (값 지우기)과 `{}` (건드리지 않음)은 서로 다른 요청이므로,
 * 단순한 nullable 타입으로는 구분할 수 없다.
 */
sealed interface PatchField<out T : Any> {
    /** 요청에 키가 없었다. 기존 값을 유지한다. */
    data object Absent : PatchField<Nothing>

    /** 키가 `null` 로 왔다. 값을 지운다. */
    data object Cleared : PatchField<Nothing>

    /** 새 값이 왔다. */
    data class Updated<out T : Any>(val value: T) : PatchField<T>
}

fun <T : Any> PatchField<T>.orKeep(current: T?): T? =
    when (this) {
        PatchField.Absent -> current
        PatchField.Cleared -> null
        is PatchField.Updated -> value
    }

/**
 * Jackson 은 키가 없으면 프로퍼티를 `null` 로 두고, `null` 이 오면 `NullNode` 를 채운다.
 * DTO 필드를 [JsonNode] 로 받아 이 차이를 [PatchField] 로 옮긴다.
 */
fun <T : Any> JsonNode?.readPatch(read: (JsonNode) -> T): PatchField<T> =
    when {
        this == null -> PatchField.Absent
        isNull -> PatchField.Cleared
        else -> PatchField.Updated(read(this))
    }

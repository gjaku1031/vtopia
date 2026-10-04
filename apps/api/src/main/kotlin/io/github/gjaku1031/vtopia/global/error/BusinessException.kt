package io.github.gjaku1031.vtopia.global.error

import org.springframework.http.HttpStatus

/**
 * 업무 규칙 위반의 HTTP 상태와 공개 가능한 설명
 *
 * publicDetail에 내부 예외 메시지·SQL·비밀값을 전달하지 않는 계약
 */
open class BusinessException(
    /**
     * HTTP 상태
     */
    val status: HttpStatus,

    /**
     * 외부에 공개할 오류 설명
     */
    val publicDetail: String,
    cause: Throwable? = null,
) : RuntimeException(null, cause)

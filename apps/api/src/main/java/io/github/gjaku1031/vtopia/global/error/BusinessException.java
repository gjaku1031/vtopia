package io.github.gjaku1031.vtopia.global.error;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반의 HTTP 상태와 공개 가능한 설명
 *
 * publicDetail에 내부 예외 메시지·SQL·비밀값을 전달하지 않는 계약
 */
public class BusinessException extends RuntimeException {

    /**
     * HTTP 상태
     */
    private final HttpStatus status;

    /**
     * 외부에 공개할 오류 설명
     */
    private final String publicDetail;

    /**
     * 원인 예외 없이 생성
     */
    public BusinessException(HttpStatus status, String publicDetail) {
        this(status, publicDetail, null);
    }

    /**
     * 원인 예외를 보존하여 생성, 예외 메시지는 비워 둠
     */
    public BusinessException(HttpStatus status, String publicDetail, @Nullable Throwable cause) {
        super(null, cause);
        this.status = status;
        this.publicDetail = publicDetail;
    }

    /**
     * HTTP 상태 조회
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * 외부에 공개할 오류 설명 조회
     */
    public String getPublicDetail() {
        return publicDetail;
    }
}

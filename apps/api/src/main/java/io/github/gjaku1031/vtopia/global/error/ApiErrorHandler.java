package io.github.gjaku1031.vtopia.global.error;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * API 오류를 내부 원문이 없는 RFC 9457 응답으로 변환
 */
@RestControllerAdvice
public class ApiErrorHandler extends ResponseEntityExceptionHandler {

    /**
     * Spring MVC의 HTTP 상태·헤더를 유지하고 고정 설명으로 응답
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatusCode statusCode,
        WebRequest request
    ) {
        return super.handleExceptionInternal(
            ex, ProblemDetail.forStatusAndDetail(statusCode, safeDetail(statusCode)), headers, statusCode, request
        );
    }

    /**
     * 업무 오류는 공개 설명으로 변환하고 예상하지 못한 오류는 타입만 기록
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleFailure(Exception ex) {
        // 업무 오류와 프레임워크 오류의 상태를 보존하고 나머지는 서버 오류로 분류
        ProblemDetail problem = switch (ex) {
            case BusinessException business ->
                ProblemDetail.forStatusAndDetail(business.getStatus(), business.getPublicDetail());
            case ErrorResponse error ->
                ProblemDetail.forStatusAndDetail(error.getStatusCode(), safeDetail(error.getStatusCode()));
            default -> ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, safeDetail(HttpStatus.INTERNAL_SERVER_ERROR));
        };
        if (problem.getStatus() >= 500) {
            logger.error("Unhandled API exception: " + ex.getClass().getName());
        }
        // 메서드 허용 목록 등 프로토콜 헤더 유지
        ResponseEntity.BodyBuilder response = ResponseEntity.status(problem.getStatus());
        if (ex instanceof ErrorResponse error) {
            response.headers(error.getHeaders());
        }
        return response.body(problem);
    }

    /**
     * 요청값·내부 사유가 없는 HTTP 오류 설명
     */
    private static String safeDetail(HttpStatusCode status) {
        return status.is4xxClientError() ? "요청을 처리할 수 없습니다." : "서버에서 요청을 처리하지 못했습니다.";
    }
}

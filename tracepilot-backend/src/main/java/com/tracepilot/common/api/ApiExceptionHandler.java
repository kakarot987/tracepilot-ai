
package com.tracepilot.common.api;

import com.tracepilot.llm.exception.*;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> badRequest(
            Exception ex) {
        return error(
                HttpStatus.BAD_REQUEST,
                "Invalid incident request",
                "Check the request fields and their lengths."
        );
    }

    @ExceptionHandler(ModelUnavailableException.class)
    public ResponseEntity<ProblemDetail> unavailable(
            ModelUnavailableException ex) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI service unavailable",
                "The local inference service is unavailable."
        );
    }

    @ExceptionHandler(ModelTimeoutException.class)
    public ResponseEntity<ProblemDetail> timeout(
            ModelTimeoutException ex) {
        return error(
                HttpStatus.GATEWAY_TIMEOUT,
                "AI request timed out",
                "The model did not respond within the time limit."
        );
    }

    @ExceptionHandler(ModelOutputException.class)
    public ResponseEntity<ProblemDetail> invalidOutput(
            ModelOutputException ex) {
        return error(
                HttpStatus.BAD_GATEWAY,
                "Invalid AI response",
                "The model could not produce a valid analysis."
        );
    }

    private ResponseEntity<ProblemDetail> error(
            HttpStatus status,
            String title,
            String detail) {

        ProblemDetail problem =
                ProblemDetail.forStatus(status);

        problem.setTitle(title);
        problem.setDetail(detail);

        return ResponseEntity.status(status).body(problem);
    }
}

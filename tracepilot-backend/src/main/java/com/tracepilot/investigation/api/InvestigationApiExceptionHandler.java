
package com.tracepilot.investigation.api;

import com.tracepilot.investigation.service.InvestigationConflictException;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@RestControllerAdvice
@Profile("postgres")
public class InvestigationApiExceptionHandler {

    @ExceptionHandler(InvestigationConflictException.class)
    public ResponseEntity<Map<String, String>> conflict(
            InvestigationConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "INVESTIGATION_CONFLICT",
                        "message", "Investigation already active or terminal"
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(
            IllegalArgumentException ex) {

        // Deliberately don't expose arbitrary exception
        // messages from internal components.
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "INVALID_REQUEST",
                        "message", "Invalid or unknown resource"
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> invalidPath(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "INVALID_PATH_PARAMETER",
                        "message", "Invalid resource identifier"
                ));
    }
}

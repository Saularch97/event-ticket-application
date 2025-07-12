package com.example.booking.config;

import com.example.booking.exception.*;
import com.example.booking.exception.base.ConflictException;
import com.example.booking.exception.base.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        fieldError -> fieldError.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        Map<String, Object> responseBody = buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more fields are invalid",
                request
        );
        responseBody.put("errors", errors);

        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException ex, HttpServletRequest request) {
        return notFoundResponse(ex, request);
    }

    @ExceptionHandler(RefreshTokenEmptyException.class)
    public ResponseEntity<Object> handleRefreshTokenEmptyException(RefreshTokenEmptyException ex, HttpServletRequest request) {
        return badRequestResponse(ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflictExceptions(RuntimeException ex, HttpServletRequest request) {
        return conflictResponse(ex, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFoundExceptions(RuntimeException ex, HttpServletRequest request) {
        return notFoundResponse(ex, request);
    }


    private static ResponseEntity<Object> notFoundResponse(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.NOT_FOUND, "Not found", ex, request);
    }

    private static ResponseEntity<Object> conflictResponse(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.CONFLICT, "Conflict", ex, request);
    }

    private static ResponseEntity<Object> badRequestResponse(Exception ex, HttpServletRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex, request);
    }

    private static ResponseEntity<Object> errorResponse(HttpStatus status, String error, Exception ex, HttpServletRequest request) {
        Map<String, Object> responseBody = buildResponse(status, error, ex.getMessage(), request);
        return new ResponseEntity<>(responseBody, status);
    }

    private static Map<String, Object> buildResponse(HttpStatus status, String error, String message, HttpServletRequest request) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status.value());
        responseBody.put("error", error);
        responseBody.put("message", message);
        responseBody.put("path", request.getRequestURI());
        responseBody.put("timestamp", Instant.now().toString());
        return responseBody;
    }
}

package com.ctn.offerwall.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> duplicateEmail(DuplicateEmailException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UserInputException.class)
    ResponseEntity<ApiError> userInput(UserInputException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> invalidCredentials(InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<ApiError> authenticationRequired(AuthenticationRequiredException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + " " + fieldError.getDefaultMessage();
    }
}

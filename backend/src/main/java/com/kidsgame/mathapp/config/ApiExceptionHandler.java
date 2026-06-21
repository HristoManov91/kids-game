package com.kidsgame.mathapp.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null || ex.getReason().isBlank()
                ? "Заявката не беше успешна."
                : ex.getReason();
        return error(status, message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> authentication() {
        return error(HttpStatus.UNAUTHORIZED, "Грешен акаунт или парола.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::fieldErrorMessage)
                .orElse("Провери въведените данни.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> dataIntegrity() {
        return error(HttpStatus.CONFLICT, "Думата вече е добавена.");
    }

    private String fieldErrorMessage(FieldError error) {
        return switch (error.getField()) {
            case "username" -> "Въведи акаунт.";
            case "email" -> "Въведи валиден email адрес.";
            case "token" -> "Линкът за нова парола е невалиден.";
            case "password" -> "Въведи парола.";
            case "repeatPassword" -> "Повтори паролата.";
            case "word" -> "Въведи дума.";
            case "image" -> "Въведи картинка.";
            case "difficulty" -> "Избери ниво от 1 до 10.";
            default -> error.getDefaultMessage() == null ? "Провери въведените данни." : error.getDefaultMessage();
        };
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("detail", message);
        return ResponseEntity.status(status).body(body);
    }
}

package com.alimertkaya.digitalwallet.exception;

import com.alimertkaya.digitalwallet.exception.kafka.EventSerializationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j // for log
@RestControllerAdvice // global exception oldugunu belirtir
public class GlobalExceptionHandler {

    // for dto's
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex,
                                                                         ServerWebExchange exchange) {
        Map<String, String> fieldMap = new LinkedHashMap<>();
        ex.getFieldErrors().forEach(fe -> fieldMap.put(fe.getField(), fe.getDefaultMessage()));

        var globalErrors = ex.getGlobalErrors().stream()
                .map(ge -> ge.getObjectName() + ": " + ge.getDefaultMessage())
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Doğrulama hatası. Lütfen alanları kontrol edin.")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .validationErrors(fieldMap.isEmpty() ? null : fieldMap)
                .details(globalErrors.isEmpty() ? null : globalErrors)
                .build();

        log.warn("Validation error on {} -> {}", errorResponse.getPath(), fieldMap);
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadInputException(ServerWebInputException ex,
                                                                       ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getReason() != null ? ex.getReason() : "Geçersiz istek gövdesi/parametre")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .build();
        log.info("Bad input: {}", ex.getMessage());
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    // JWT errors
    @ExceptionHandler({ExpiredJwtException.class, SignatureException.class, MalformedJwtException.class})
    public Mono<ResponseEntity<ErrorResponse>> handleJwtException(Exception ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Unauthorized")
                .message("Invalid or expired token")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .build();
        log.info("JWT error: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAccessDeniedException(AccessDeniedException ex,
                                                                           ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Bu kaynağa erişim yetkiniz yok.")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .build();
        log.info("Access denied: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse));
    }

    @ExceptionHandler(EventSerializationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleEventSerializationException(EventSerializationException ex,
                                                                                 ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Event Serialization Error")
                .message("Kafka event JSON formatına dönüştürülürken hata oluştu.")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .details(List.of(ex.getMessage()))
                .build();

        log.error("Event serialization error on {}: {}", errorResponse.getPath(), ex.getMessage(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    // ozel durumlar
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(ResponseStatusException ex,
                                                                             ServerWebExchange exchange) {
        var status = ex.getStatusCode();
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.toString())
                .message(ex.getReason() != null ? ex.getReason() : "İstek işlenemedi")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .build();
        log.info("RSE {}: {}", status, ex.getReason());
        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }

    // 500: beklenmeyen her sey
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Beklenmeyen bir hata oluştu.")
                .path(exchange.getRequest().getPath().value())
                .requestId(exchange.getRequest().getId())
                .build();
        log.error("Unhandled error on {}: {}", errorResponse.getPath(), ex.getMessage(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}
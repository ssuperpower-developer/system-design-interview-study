package com.examp.Webflux.exception;

import com.examp.Webflux.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApplicationException(ApplicationException ex) {
        return Mono.just(ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ErrorResponse.from(ex.getErrorCode())));
    }
}

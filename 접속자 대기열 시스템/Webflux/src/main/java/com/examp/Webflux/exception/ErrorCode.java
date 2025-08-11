package com.examp.Webflux.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * packageName   : com.examp.Webflux.exception
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 9.
 * Description   :
 */

@AllArgsConstructor
@Getter
public enum ErrorCode {

    QUEUE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "UQ-0001", "Already registered user");

    private HttpStatus httpStatus;
    private String code;
    private String message;

}

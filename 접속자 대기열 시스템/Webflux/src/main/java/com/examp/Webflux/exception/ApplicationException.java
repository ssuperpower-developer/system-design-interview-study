package com.examp.Webflux.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * packageName   : com.examp.Webflux.exception
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 9.
 * Description   :
 */
@RequiredArgsConstructor
@Getter
public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    @Override
    public String getMessage(){
        return errorCode.getMessage();
    }
}

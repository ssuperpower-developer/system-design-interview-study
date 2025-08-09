package com.examp.Webflux.dto;

import com.examp.Webflux.exception.ErrorCode;

/**
 * packageName   : com.examp.Webflux.dto
 * Author        : imhyeong-gyu
 * Data          : 2025. 8. 9.
 * Description   :
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}

package com.oingmaryho.business.delivery_service.exception;

import lombok.Getter;

@Getter
public class LockException extends RuntimeException {
    private final ErrorCode errorCode;

    public LockException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

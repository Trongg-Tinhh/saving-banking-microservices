package com.saving.contract.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String    detail;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detail    = null;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detail    = detail;
    }

    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail != null ? detail : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.detail    = detail;
    }
}

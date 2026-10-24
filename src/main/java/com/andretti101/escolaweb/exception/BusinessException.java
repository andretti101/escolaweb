package com.andretti101.escolaweb.exception;

public class BusinessException extends IllegalArgumentException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

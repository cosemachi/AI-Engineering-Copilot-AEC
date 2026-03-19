package com.aec.application;

public class AecException extends RuntimeException {
    public AecException(String message) {
        super(message);
    }

    public AecException(String message, Throwable cause) {
        super(message, cause);
    }
}

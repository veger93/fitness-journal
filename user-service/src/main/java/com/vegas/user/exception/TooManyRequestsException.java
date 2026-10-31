package com.vegas.user.exception;

/** 429: лимит исчерпан (например, демо — docs/demo.md). Сообщение — для человека, фронт показывает его как есть */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}

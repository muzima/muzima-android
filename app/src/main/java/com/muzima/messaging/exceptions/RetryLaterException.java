package com.muzima.messaging.exceptions;

public class RetryLaterException extends Exception {
    public RetryLaterException() {
        super();
    }

    public RetryLaterException(Exception e) {
        super(e);
    }
}

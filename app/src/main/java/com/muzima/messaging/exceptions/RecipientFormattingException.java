package com.muzima.messaging.exceptions;

public class RecipientFormattingException extends Exception {
    public RecipientFormattingException() {
        super();
    }

    public RecipientFormattingException(String message) {
        super(message);
    }

    public RecipientFormattingException(String message, Throwable nested) {
        super(message, nested);
    }

    public RecipientFormattingException(Throwable nested) {
        super(nested);
    }
}

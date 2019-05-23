package com.muzima.messaging.exceptions;

public class MmsRadioException extends Throwable {
    public MmsRadioException(String s) {
        super(s);
    }

    public MmsRadioException(Exception e) {
        super(e);
    }
}

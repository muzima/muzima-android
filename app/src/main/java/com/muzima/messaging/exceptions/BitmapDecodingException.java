package com.muzima.messaging.exceptions;

public class BitmapDecodingException extends Exception {

    public BitmapDecodingException(String s){
        super(s);
    }

    public BitmapDecodingException(Exception nestedException){
        super(nestedException);
    }
}

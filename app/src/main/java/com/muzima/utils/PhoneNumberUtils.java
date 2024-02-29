package com.muzima.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jose Julai Ritsure
 */
public class PhoneNumberUtils {
    public static final String PHONE_NUMBER_MASK = "821234567";
    public static boolean validatePhoneNumber(String phoneNumber) {
        Pattern pattern = Pattern.compile("^\\d{9}$");
        Matcher matcher = pattern.matcher(PHONE_NUMBER_MASK);
        if(!matcher.matches()) {
           return false;
        }
        if(!phoneNumber.startsWith("8")) {
            return false;
        }
        return true;
    }

}

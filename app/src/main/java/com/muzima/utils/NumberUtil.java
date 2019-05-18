package com.muzima.utils;

import android.telephony.PhoneNumberUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtil {
    private static final Pattern emailPattern = android.util.Patterns.EMAIL_ADDRESS;

    public static boolean isValidEmail(String number) {
        Matcher matcher = emailPattern.matcher(number);
        return matcher.matches();
    }

    public static boolean isValidSmsOrEmail(String number) {
        return PhoneNumberUtils.isWellFormedSmsAddress(number) || isValidEmail(number);
    }
}

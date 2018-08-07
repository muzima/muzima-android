/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import java.util.Arrays;
import java.util.List;

import static android.text.TextUtils.split;

public class StringUtils {

    public static final String EMPTY = "";

    public static String getCommaSeparatedStringFromList(final List<String> values){
        if(values == null){
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String next : values) {
            builder.append(next).append(",");
        }
        String commaSeparated = builder.toString();
        return commaSeparated.substring(0, commaSeparated.length() - 1);
    }

    public static List<String> getListFromCommaSeparatedString(String value){
        String[] values = split(value, ",");
        return Arrays.asList(values);
    }

    public static boolean isEmpty(String string) {
        return (string == null || string.trim().length() == 0);
    }

    public static String defaultString(String string) {
        return string == null ? "" : string;
    }

    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }
    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    public static int nullSafeCompare(String str1, String str2){
        return str1 == null ? str2 == null ? 0 : -1 :
                str2 == null ? 1 : str1.compareTo(str2);
    }
}

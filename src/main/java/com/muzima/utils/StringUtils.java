package com.muzima.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static android.text.TextUtils.split;

public class StringUtils {

    public static final String EMPTY = "";

    public static String getCommaSeparatedStringFromList(final List<String> values){
        if(values == null){
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Iterator<String> valuesIterator = values.iterator();
        while (valuesIterator.hasNext()) {
            String next = valuesIterator.next();
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
}

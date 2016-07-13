/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtils {
    public static String getFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("dd-MM-yyyy");
        return formattedDate.format(date);
    }

    public static Date parse(String dateAsString) throws ParseException {
        String datePattern = "[\\d]{2}-[\\d]{2}-[\\d]{4}";
        Pattern pattern = Pattern.compile(datePattern);
        Matcher matcher = pattern.matcher(dateAsString);
        if (matcher.matches()) {
            SimpleDateFormat formattedDate = new SimpleDateFormat("dd-MM-yyyy");
            return formattedDate.parse(dateAsString);
        } else {
            SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");
            return formattedDate.parse(dateAsString);
        }
    }

    public static String getFormattedDateTime(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("dd-MM-yyyy hh:mm");
        return formattedDate.format(date);
    }

    public static String getMonthNameFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("dd MMM yyyy");
        return formattedDate.format(date);
    }
}

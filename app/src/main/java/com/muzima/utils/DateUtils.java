/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.muzima.utils.Constants.*;

public class DateUtils {
    public static final String SIMPLE_DATE_FORMAT = "yyyy.MM.dd";

    public static String getFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DATE_FORMAT);
        return formattedDate.format(date);
    }

    public static Date parse(String dateAsString) throws ParseException {

        Pattern dateTimePattern = Pattern.compile("[\\d]{2}-[\\d]{2}-[\\d]{4} [\\d]{2}:[\\d]{2}");
        Pattern datePattern = Pattern.compile("[\\d]{2}-[\\d]{2}-[\\d]{4}");
        if (dateTimePattern.matcher(dateAsString).matches()) {
            SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DATE_LOCALE_FORMAT);
            return formattedDate.parse(dateAsString);
        } else if (datePattern.matcher(dateAsString).matches()) {
            SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DATE_FORMAT);
            return formattedDate.parse(dateAsString);
        } else {
            SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd");
            return formattedDate.parse(dateAsString);
        }
    }

    public static String getFormattedDateTime(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DATE_LOCALE_FORMAT);
        return formattedDate.format(date);
    }

    public static String getFormattedStandardDisplayDateTime(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DISPLAY_FORMAT);
        return formattedDate.format(date);
    }

    public static String getFormattedDate(Date date, String pattern){
        SimpleDateFormat formattedDate = new SimpleDateFormat(pattern);
        return formattedDate.format(date);
    }

    public static String getMonthNameFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("dd MMM yyyy");
        return formattedDate.format(date);
    }

    public static Date parseDateByPattern(String dateAsString, String pattern) throws ParseException {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
        return dateFormatter.parse(dateAsString);
    }

    public static String getTime(Date date) {
        SimpleDateFormat formattedDate = new SimpleDateFormat("HH:ss");
        return formattedDate.format(date);
    }

    public static String convertDateToStdString(Date date) {
        if (date == null) return "Invalid";
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT, Locale.getDefault());
        return simpleDateFormat.format(calendar.getTime());
    }
}

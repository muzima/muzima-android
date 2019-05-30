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

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.format.DateFormat;

import com.muzima.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.muzima.utils.Constants.*;
import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;

public class DateUtils extends android.text.format.DateUtils {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    public static String getFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DATE_FORMAT);
        return formattedDate.format(date);
    }

    public static Date parse(String dateAsString) throws ParseException {

        String datePattern = "[\\d]{2}-[\\d]{2}-[\\d]{4}";
        Pattern pattern = Pattern.compile(datePattern);
        Matcher matcher = pattern.matcher(dateAsString);
        if (matcher.matches()) {
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

    public static String getBriefRelativeTimeSpanString(final Context c, final Locale locale, final long timestamp) {
        if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
            return c.getString(R.string.date_utils_just_now);
        } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
            int mins = convertDelta(timestamp, TimeUnit.MINUTES);
            return c.getResources().getString(R.string.date_utils_minutes_ago, mins);
        } else if (isWithin(timestamp, 1, TimeUnit.DAYS)) {
            int hours = convertDelta(timestamp, TimeUnit.HOURS);
            return c.getResources().getQuantityString(R.plurals.hours_ago, hours, hours);
        } else if (isWithin(timestamp, 6, TimeUnit.DAYS)) {
            return getFormattedDateTime(timestamp, "EEE", locale);
        } else if (isWithin(timestamp, 365, TimeUnit.DAYS)) {
            return getFormattedDateTime(timestamp, "MMM d", locale);
        } else {
            return getFormattedDateTime(timestamp, "MMM d, yyyy", locale);
        }
    }

    private static boolean isWithin(final long millis, final long span, final TimeUnit unit) {
        return System.currentTimeMillis() - millis <= unit.toMillis(span);
    }

    private static int convertDelta(final long millis, TimeUnit to) {
        return (int) to.convert(System.currentTimeMillis() - millis, TimeUnit.MILLISECONDS);
    }

    public  static String getFormattedDateTime(long time, String template, Locale locale) {
        final String localizedPattern = getLocalizedPattern(template, locale);
        return new SimpleDateFormat(localizedPattern, locale).format(new Date(time));
    }

    private static String getLocalizedPattern(String template, Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return DateFormat.getBestDateTimePattern(locale, template);
        } else {
            return new SimpleDateFormat(template, locale).toLocalizedPattern();
        }
    }

    public static String getExtendedRelativeTimeSpanString(final Context c, final Locale locale, final long timestamp) {
        if (isWithin(timestamp, 1, TimeUnit.MINUTES)) {
            return c.getString(R.string.date_utils_just_now);
        } else if (isWithin(timestamp, 1, TimeUnit.HOURS)) {
            int mins = (int)TimeUnit.MINUTES.convert(System.currentTimeMillis() - timestamp, TimeUnit.MILLISECONDS);
            return c.getResources().getString(R.string.date_utils_minutes_ago, mins);
        } else {
            StringBuilder format = new StringBuilder();
            if      (isWithin(timestamp,   6, TimeUnit.DAYS)) format.append("EEE ");
            else if (isWithin(timestamp, 365, TimeUnit.DAYS)) format.append("MMM d, ");
            else                                              format.append("MMM d, yyyy, ");

            if (DateFormat.is24HourFormat(c)) format.append("HH:mm");
            else                              format.append("hh:mm a");

            return getFormattedDateTime(timestamp, format.toString(), locale);
        }
    }

    public static boolean isSameExtendedRelativeTimestamp(@NonNull Context context, @NonNull Locale locale, long t1, long t2) {
        return getExtendedRelativeTimeSpanString(context, locale, t1).equals(getExtendedRelativeTimeSpanString(context, locale, t2));
    }

    public static boolean isSameDay(long t1, long t2) {
        return DATE_FORMAT.format(new Date(t1)).equals(DATE_FORMAT.format(new Date(t2)));
    }

    public static String getRelativeDate(@NonNull Context context, @NonNull Locale locale, long timestamp)
    {
        if (isToday(timestamp)) {
            return context.getString(R.string.DateUtils_today);
        } else if (isYesterday(timestamp)) {
            return context.getString(R.string.DateUtils_yesterday);
        } else {
            return getFormattedDateTime(timestamp, "EEE, MMM d, yyyy", locale);
        }
    }

    private static boolean isYesterday(final long when) {
        return DateUtils.isToday(when + TimeUnit.DAYS.toMillis(1));
    }

    public static SimpleDateFormat getDetailedDateFormatter(Context context, Locale locale) {
        String dateFormatPattern;

        if (DateFormat.is24HourFormat(context)) {
            dateFormatPattern = getLocalizedPattern("MMM d, yyyy HH:mm:ss zzz", locale);
        } else {
            dateFormatPattern = getLocalizedPattern("MMM d, yyyy hh:mm:ss a zzz", locale);
        }

        return new SimpleDateFormat(dateFormatPattern, locale);
    }

    public static String getDayPrecisionTimeSpanString(Context context, Locale locale, long timestamp) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

        if (simpleDateFormat.format(System.currentTimeMillis()).equals(simpleDateFormat.format(timestamp))) {
            return context.getString(R.string.DeviceListItem_today);
        } else {
            String format;

            if      (isWithin(timestamp, 6, TimeUnit.DAYS))   format = "EEE ";
            else if (isWithin(timestamp, 365, TimeUnit.DAYS)) format = "MMM d";
            else                                              format = "MMM d, yyy";

            return getFormattedDateTime(timestamp, format, locale);
        }
    }
}

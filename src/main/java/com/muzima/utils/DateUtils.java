package com.muzima.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    public static String getFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy/MM/dd");
        return formattedDate.format(date);
    }

    public static Date parse(String dateAsString) throws ParseException {
        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy/MM/dd");
        return formattedDate.parse(dateAsString);
    }

    public static String getFormattedDateTime(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy/MM/dd hh:mm");
        return formattedDate.format(date);
    }

    public static String getMonthNameFormattedDate(Date date){
        SimpleDateFormat formattedDate = new SimpleDateFormat("dd MMM yyyy");
        return formattedDate.format(date);
    }
}

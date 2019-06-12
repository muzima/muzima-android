package com.muzima.messaging.utils;

import android.content.Context;

import com.muzima.R;

import java.util.concurrent.TimeUnit;

public class ExpirationUtil {
    public static String getExpirationDisplayValue(Context context, int expirationTime) {
        if (expirationTime <= 0) {
            return context.getString(R.string.general_off);
        } else if (expirationTime < TimeUnit.MINUTES.toSeconds(1)) {
            return context.getResources().getQuantityString(R.plurals.plurals_seconds, expirationTime, expirationTime);
        } else if (expirationTime < TimeUnit.HOURS.toSeconds(1)) {
            int minutes = expirationTime / (int)TimeUnit.MINUTES.toSeconds(1);
            return context.getResources().getQuantityString(R.plurals.plurals_minutes, minutes, minutes);
        } else if (expirationTime < TimeUnit.DAYS.toSeconds(1)) {
            int hours = expirationTime / (int)TimeUnit.HOURS.toSeconds(1);
            return context.getResources().getQuantityString(R.plurals.plurals_hours, hours, hours);
        } else if (expirationTime < TimeUnit.DAYS.toSeconds(7)) {
            int days = expirationTime / (int)TimeUnit.DAYS.toSeconds(1);
            return context.getResources().getQuantityString(R.plurals.plurals_days, days, days);
        } else {
            int weeks = expirationTime / (int)TimeUnit.DAYS.toSeconds(7);
            return context.getResources().getQuantityString(R.plurals.plurals_weeks, weeks, weeks);
        }
    }

    public static String getExpirationAbbreviatedDisplayValue(Context context, int expirationTime) {
        if (expirationTime < TimeUnit.MINUTES.toSeconds(1)) {
            return context.getResources().getString(R.string.general_seconds_abbreviated, expirationTime);
        } else if (expirationTime < TimeUnit.HOURS.toSeconds(1)) {
            int minutes = expirationTime / (int)TimeUnit.MINUTES.toSeconds(1);
            return context.getResources().getString(R.string.general_minutes_abbreviated, minutes);
        } else if (expirationTime < TimeUnit.DAYS.toSeconds(1)) {
            int hours = expirationTime / (int)TimeUnit.HOURS.toSeconds(1);
            return context.getResources().getString(R.string.general_hours_abbreviated, hours);
        } else if (expirationTime < TimeUnit.DAYS.toSeconds(7)) {
            int days = expirationTime / (int)TimeUnit.DAYS.toSeconds(1);
            return context.getResources().getString(R.string.general_days_abbreviated, days);
        } else {
            int weeks = expirationTime / (int)TimeUnit.DAYS.toSeconds(7);
            return context.getResources().getString(R.string.general_weeks_abbreviated, weeks);
        }
    }
}

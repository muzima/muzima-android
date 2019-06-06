/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.TypedValue;

import com.muzima.R;

public class ThemeUtils {

    private int lightThemeId;
    private int darkThemeId;
    private boolean lightMode;

    public ThemeUtils() {
        lightThemeId = R.style.AppTheme_Light;
        darkThemeId = R.style.AppTheme_Dark;
    }

    public ThemeUtils(int lightThemeId, int darkThemeId) {
        this.lightThemeId = lightThemeId;
        this.darkThemeId = darkThemeId;
    }

    public void onCreate(Activity activity) {
        setLightMode(activity);
        setThemeForActivity(activity);
    }

    public void onResume(Activity activity) {
        if (isLightModeChanged(activity)) {
            Intent intent = activity.getIntent();
            activity.finish();
            activity.overridePendingTransition(0, 0);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    private void setLightMode(Activity activity){
        lightMode = getPreferenceLightMode(activity);
    }

    protected void setThemeForActivity(Activity activity) {
        if (lightMode) {
            activity.setTheme(lightThemeId);
        } else {
            activity.setTheme(darkThemeId);
        }
    }

    private static boolean getPreferenceLightMode(Context context) {
        //check if night mode enabled or not
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String lightModeKey = context.getResources().getString(R.string.preference_light_mode);
        return preferences.getBoolean(lightModeKey, false);
    }

    public boolean isLightModeChanged(Context context) {
        return (lightMode != getPreferenceLightMode(context));
    }

    private static Drawable getIcon(Context context, int iconIdForLightMode, int iconIdForNightMode) {
        boolean lightMode = getPreferenceLightMode(context);
        if (lightMode) {
            return context.getResources().getDrawable(iconIdForLightMode);
        } else {
            return context.getResources().getDrawable(iconIdForNightMode);
        }
    }

    public static Drawable getIconWarning(Context context){
        return getIcon(context, R.drawable.ic_warning_light, R.drawable.ic_warning);
    }

    public static Drawable getIconRefresh(Context context) {
        return getIcon(context, R.drawable.ic_refresh_light, R.drawable.ic_refresh);
    }

    public static boolean isDarkTheme(@NonNull Context context) {
       return getAttribute(context, R.attr.theme_type, "light").equals("dark");
    }

    private static String getAttribute(Context context, int attribute, String defaultValue) {
        TypedValue outValue = new TypedValue();

        if (context.getTheme().resolveAttribute(attribute, outValue, true)) {
            return outValue.coerceToString().toString();
        } else {
            return defaultValue;
        }
    }

    public static int getThemedColor(@NonNull Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();

        if (theme.resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return Color.RED;
    }
}

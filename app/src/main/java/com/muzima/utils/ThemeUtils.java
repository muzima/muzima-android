/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.muzima.R;

public class ThemeUtils {

    private int lightThemeId;
    private int darkThemeId;
    private boolean lightMode;
    private static ThemeUtils themeUtils;

    private ThemeUtils() {
    }

    public static ThemeUtils getInstance(){
        if (themeUtils == null){
            themeUtils = new ThemeUtils();
        }
        return themeUtils;
    }

    public void onCreate(Activity activity, boolean showActionBar) {
        setThemeResourceIdentifiers(showActionBar);
        setThemeForActivity(activity);
    }

    private void setThemeResourceIdentifiers(boolean showActionBar) {
        if(showActionBar){
            lightThemeId = R.style.AppTheme_Light;
            darkThemeId = R.style.AppTheme;
        } else {
            lightThemeId = R.style.AppTheme_Light_NoActionBar;
            darkThemeId = R.style.AppTheme_NoActionBar;
        }
    }

    private void setThemeForActivity(Activity activity) {
        lightMode = isLightModeSettingEnabled(activity);
        if (lightMode) {
            activity.setTheme(lightThemeId);
        } else {
            activity.setTheme(darkThemeId);
        }
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

    public boolean isLightModeSettingEnabled(Context context) {
        //check if night mode enabled or not
        String lightModeKey = context.getResources().getString(R.string.preference_light_mode);
        return MuzimaPreferences.getBooleanPreference(context, lightModeKey, false);
    }

    public boolean isLightModeChanged(Context context) {
        return (lightMode != isLightModeSettingEnabled(context));
    }

    public static Drawable getIconWarning(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_warning);
    }

    public static Drawable getIconRefresh(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_refresh);
    }
}

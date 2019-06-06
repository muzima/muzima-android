package com.muzima.utils;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.muzima.R;

public class DynamicNoActionBarTheme extends ThemeUtils {
    @Override
    protected void setThemeForActivity(Activity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String lightModeKey = activity.getResources().getString(R.string.preference_light_mode);
        boolean isLightModeEnabled = preferences.getBoolean(lightModeKey, false);
        if (isLightModeEnabled) {
            activity.setTheme(R.style.AppThemeNoActionBar_Light);
        } else {
            activity.setTheme(R.style.AppThemeNoActionBar_Dark);
        }
    }
}

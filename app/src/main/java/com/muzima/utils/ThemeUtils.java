package com.muzima.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
        lightMode = getPreferenceLightMode(activity);
        setThemeForActivity(activity);
    }

    public void onResume(Activity activity) {
        if (isLightModeCahnged(activity)) {
            Intent intent = activity.getIntent();
            activity.finish();
            activity.overridePendingTransition(0, 0);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
        }
    }

    public void setThemeForActivity(Activity activity) {
        if (lightMode) {
            activity.setTheme(lightThemeId);
        } else {
            activity.setTheme(darkThemeId);
        }
    }

    public boolean getPreferenceLightMode(Context context) {
        //check if night mode enabled or not
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String light_mode_key = context.getResources().getString(R.string.preference_light_mode);
        return preferences.getBoolean(light_mode_key, false);
    }

    public boolean isLightModeCahnged(Context context){
        return (lightMode != getPreferenceLightMode(context));
    }

}

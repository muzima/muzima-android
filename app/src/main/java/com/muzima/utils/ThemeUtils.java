package com.muzima.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
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
        setLightMode(activity);
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

    private void setLightMode(Activity activity){
        lightMode = getPreferenceLightMode(activity);
    }

    private void setThemeForActivity(Activity activity) {
        if (lightMode) {
            activity.setTheme(lightThemeId);
        } else {
            activity.setTheme(darkThemeId);
        }
    }

    private static boolean getPreferenceLightMode(Context context) {
        //check if night mode enabled or not
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String light_mode_key = context.getResources().getString(R.string.preference_light_mode);
        return preferences.getBoolean(light_mode_key, false);
    }

    public boolean isLightModeCahnged(Context context) {
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

    public static Drawable getIconRefresh(Context context){
        return getIcon(context, R.drawable.ic_refresh_light, R.drawable.ic_refresh);
    }
}

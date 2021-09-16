/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
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
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;

import com.muzima.R;

import java.util.Locale;

public class LanguageUtil {
    private static final String DEFAULT = "zz";

    private Locale currentLocale;

    public void onCreate(Activity activity) {
        currentLocale = getSelectedLocale(activity);
        setContextLocale(activity, currentLocale);
    }

    public void onResume(Activity activity) {
        if (currentLocale != null && !currentLocale.equals(getSelectedLocale(activity))) {
            Intent intent = activity.getIntent();
            activity.finish();
            OverridePendingTransition.invoke(activity);
            activity.startActivity(intent);
            OverridePendingTransition.invoke(activity);
        }
    }

    private static void setContextLocale(Context context, Locale selectedLocale) {
        Configuration configuration = context.getResources().getConfiguration();

        if (!configuration.locale.equals(selectedLocale)) {
            configuration.locale = selectedLocale;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLocale(selectedLocale);
                configuration.setLayoutDirection(selectedLocale);
            }else{
                configuration.locale = selectedLocale;
            }
            context.getResources().updateConfiguration(configuration,
                    context.getResources().getDisplayMetrics());
        }
    }

    public Locale getSelectedLocale(Context context) {
        String languageKey = context.getResources().getString(R.string.preference_app_language);
        String defaultLanguage = context.getString(R.string.language_english);
        String preferredLocale = PreferenceManager.getDefaultSharedPreferences(context).getString(languageKey,defaultLanguage);
        return new Locale(preferredLocale);
    }

    private static final class OverridePendingTransition {
        static void invoke(Activity activity) {
            activity.overridePendingTransition(0, 0);
        }
    }

    public Context getLocalizedContext(Context context){
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(getSelectedLocale(context));
        return context.createConfigurationContext(conf);
    }
}

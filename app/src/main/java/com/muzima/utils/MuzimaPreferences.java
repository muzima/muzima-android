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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.security.crypto.EncryptedSharedPreferences;

import com.muzima.R;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Support system preferences including prefs not
 * displayed in the settings UI
 */
public class MuzimaPreferences  extends PreferenceDataStore {

    private static final String CONFIG_FILE_NAME = "FileName";
    private static final String CONFIG_MASTER_KEY_ALIAS = "muzima_secret_shared_prefs";
    private  static MuzimaPreferences instance;
    private SharedPreferences sharedPreferences;

    public MuzimaPreferences(Context context) {
        try {
            sharedPreferences = EncryptedSharedPreferences.create(
                    CONFIG_FILE_NAME,
                    CONFIG_MASTER_KEY_ALIAS,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (Exception e) {
            Log.e("MuzimaPreferences", "Cannot get secure preferences datastore", e);
        }
    }

    public static MuzimaPreferences getInstance(Context context) {
        if (instance == null)
            instance = new MuzimaPreferences(context);
        return instance;

    }
    public static SharedPreferences getSecureSharedPreferences(Context context) {
        return getInstance(context).getSharedPreferences();
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    private static final String ON_BOARDING_COMPLETED_PREFERENCE = "onboarding_completed_pref";

    public static void setOnBoardingCompletedPreference(Context context, boolean isLightMode) {
        setBooleanPreference(context, ON_BOARDING_COMPLETED_PREFERENCE, isLightMode);
    }

    public static boolean getOnBoardingCompletedPreference(Context context) {
        return getBooleanPreference(context, ON_BOARDING_COMPLETED_PREFERENCE, false);
    }

    public static boolean getIsLightModeThemeSelectedPreference(Context context) {
        try {
            return getSecureSharedPreferences(context)
                    .getBoolean(context.getResources().getString(R.string.preference_light_mode), false);
        } catch (Exception e) {
            Log.e("MuzimaPreferences", "Error getting secure shared preferences");
            return false;
        }
    }

    public static void setBooleanPreference(Context context, String key, boolean value) {
        try {
            getSecureSharedPreferences(context).edit().putBoolean(key, value).apply();
        } catch (Exception e){
            Log.e("MuzimaPreferences", "Error getting secure shared preferences");
        }
    }

    public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
        try {
            return getSecureSharedPreferences(context).getBoolean(key, defaultValue);
        } catch (Exception e){
            Log.e("MuzimaPreferences", "Error getting secure shared preferences");
            return defaultValue;
        }
    }

    public static void setStringPreference(Context context, String key, String value) {
        try {
            getSecureSharedPreferences(context).edit().putString(key, value).apply();
        } catch (Exception e){
            Log.e("MuzimaPreferences", "Error getting secure shared preferences");
        }
    }

    public static String getStringPreference(Context context, String key, String defaultValue) {
        try {
            return getSecureSharedPreferences(context).getString(key, defaultValue);
        } catch (Exception e){
            Log.e("MuzimaPreferences", "Error getting secure shared preferences");
            return defaultValue;
        }
    }

    @Override
    public void putString(String key, @Nullable String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    @Override
    public void putStringSet(String key, @Nullable Set<String> values) {
        sharedPreferences.edit().putStringSet(key, values).apply();
    }

    @Override
    public void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    @Override
    public void putLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    @Override
    public void putFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    @Override
    public void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return sharedPreferences.getString(key, defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return sharedPreferences.getStringSet(key, defValues);
    }

    @Override
    public int getInt(String key, int defValue) {
        return sharedPreferences.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return sharedPreferences.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return sharedPreferences.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return sharedPreferences.getBoolean(key, defValue);
    }
}

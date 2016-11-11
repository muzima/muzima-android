package com.muzima.service;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;

import java.io.IOException;

public class LocalePreferenceService {
    private final String TAG = "LocalePreferenceService";

    private MuzimaApplication muzimaApplication;
    public LocalePreferenceService(MuzimaApplication muzimaApplication){
        this.muzimaApplication = muzimaApplication;
    }
    public void setPreferredLocale(String preferredLocale){
        Context muzimaContext = muzimaApplication.getMuzimaContext();
        try {
            muzimaContext.setPreferredLocale(preferredLocale);
        } catch(IOException e){
            Log.e(TAG, "Exception thrown while setting preferred Locale", e);
        }
    }
}

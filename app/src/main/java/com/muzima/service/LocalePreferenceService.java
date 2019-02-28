/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;

import java.io.IOException;

public class LocalePreferenceService {

    private final MuzimaApplication muzimaApplication;
    public LocalePreferenceService(MuzimaApplication muzimaApplication){
        this.muzimaApplication = muzimaApplication;
    }
    public void setPreferredLocale(String preferredLocale){
        Context muzimaContext = muzimaApplication.getMuzimaContext();
        try {
            muzimaContext.setPreferredLocale(preferredLocale);
        } catch(IOException e){
            Log.e(getClass().getSimpleName(), "Exception thrown while setting preferred Locale", e);
        }
    }
}

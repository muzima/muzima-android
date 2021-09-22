/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.help;

import android.os.Bundle;
import android.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.domain.Credentials;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseActivity;

public class BaseHelpActivity extends BaseActivity {

    private final ThemeUtils themeUtils = new ThemeUtils(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        if (isUserLoggedOut()) {
            ((MuzimaApplication) getApplication()).cancelTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public String getUserName() {
        Credentials credentials = new Credentials(this);
        return credentials.getUserName();
    }

    public boolean isUserLoggedOut() {
        Credentials credentials = new Credentials(this);
        return credentials.isEmpty();
    }
}

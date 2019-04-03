/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.api.model.User;
import com.muzima.domain.Credentials;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseActivity;

import java.io.IOException;

public class BaseHelpActivity extends BaseActivity {

    private final ThemeUtils themeUtils = new ThemeUtils();

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
        boolean returnValue = super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        setMenuInvisible(menu);
        return returnValue;
    }

    private void removeHelpMenu(Menu menu) {
        MenuItem menuHelp = menu.findItem(R.id.action_help);
        if (menuHelp != null) menuHelp.setVisible(false);
    }

    private void setMenuInvisible(Menu menu) {
        if (isUserLoggedOut()) {
            for (int i = 0; i<menu.size(); i++) {
                menu.getItem(i).setVisible(false);
            }
            MenuItem menuHelp = menu.findItem(R.id.action_login);
            if (menuHelp != null) menuHelp.setVisible(true);
        }
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

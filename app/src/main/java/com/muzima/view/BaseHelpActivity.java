package com.muzima.view;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.api.model.User;
import com.muzima.domain.Credentials;
import com.muzima.view.BaseActivity;

import java.io.IOException;

public class BaseHelpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
        if (isUserLoggedOut()) {
            ((MuzimaApplication) getApplication()).cancelTimer();
        }
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
        Log.i(null, "Username: " + credentials.getUserName());
        return credentials.getUserName();
    }

    public boolean isUserLoggedOut() {
        Credentials credentials = new Credentials(this);
        return credentials.isEmpty();
    }

/*
    public boolean isUserLoggedOut() {
        User user = ((MuzimaApplication) getApplication()).getAuthenticatedUser();
        return (user == null);
    }
    */
}

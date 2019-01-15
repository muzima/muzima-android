package com.muzima.view;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.view.BaseActivity;

public class BaseHelpActivity extends BaseActivity {

    public static final String USER_LOGGED_OUT = "USER_LOGGED_OUT";
    private boolean isUserLoggedOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUserLoggedOut();
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
        if (isUserLoggedOut) {
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
        if (isUserLoggedOut) {
            menu.clear();
        }
    }

    private void setUserLoggedOut() {
        isUserLoggedOut = false;
        if (getIntent().hasExtra(USER_LOGGED_OUT)) {
            String logged_out = getIntent().getStringExtra(USER_LOGGED_OUT);
            isUserLoggedOut = Boolean.parseBoolean(logged_out);
        }
    }

    public boolean isUserLoggedOut() {
        return isUserLoggedOut;
    }
}

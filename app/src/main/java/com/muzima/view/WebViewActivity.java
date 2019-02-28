/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.os.Bundle;
import android.webkit.WebView;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.R;

public class WebViewActivity extends BaseActivity {

    public static final String HELP_FILE_PATH_PARAM = "HELP_FILE_PATH";
    public static final String HELP_TITLE = "HELP_TITLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_display);
        setHelpContent();
    }

    private void setHelpContent() {
        WebView webView = findViewById(R.id.webViewHelpDisplay);
        webView.getSettings().setJavaScriptEnabled(true);
        setTitle(getIntent().getStringExtra(HELP_TITLE));
        webView.loadUrl(getIntent().getStringExtra(HELP_FILE_PATH_PARAM));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help, menu);
        super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        return true;
    }

    private void removeHelpMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_help);
        menuSettings.setVisible(false);
    }
}
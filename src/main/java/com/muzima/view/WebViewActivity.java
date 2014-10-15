package com.muzima.view;

import android.os.Bundle;
import android.webkit.WebView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
        WebView webView = (WebView) findViewById(R.id.webViewHelpDisplay);
        setTitle(getIntent().getStringExtra(HELP_TITLE));
        webView.loadUrl(getIntent().getStringExtra(HELP_FILE_PATH_PARAM));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.help, menu);
        super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        return true;
    }

    private void removeHelpMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_help);
        menuSettings.setVisible(false);
    }
}
/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.TimeoutPreferenceService;

public class VideoWebViewActivity extends BaseHelpActivity {

    private WebView webView;
    private View mCustomView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
    private int muzimaTimeout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_video_web_view);
        setMuzimaTimout();
        setVideoHelpContent();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //set session timeout back to original
        if (!isUserLoggedOut()) {
            ((MuzimaApplication) getApplication()).resetTimer(muzimaTimeout);
        }
    }

    private void setMuzimaTimout() {
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        muzimaTimeout = new TimeoutPreferenceService(muzimaApplication).getTimeout();
    }

    private void setVideoHelpContent() {
        setTitle(getIntent().getStringExtra(VIDEO_TITLE));
        webView = (WebView) findViewById(R.id.videoWebView);

        webView.setWebViewClient(new myWebViewClient());

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);

        String html = makeHtml();
        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
    }

    private String makeHtml() {
        String videoId = getVideoId(getIntent().getStringExtra(VIDEO_PATH));
        String videoUrl = "https://www.youtube.com/embed/" + videoId;
        String html = "<iframe src='" + videoUrl + "' frameborder='0' allowFullScreen></iframe>";
        return html;
    }

    private String getVideoId(String videoUrl) {
        String[] splitUrl = videoUrl.split("/");
        String videoId = splitUrl[splitUrl.length - 1];
        return videoId;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return true;
    }

    class myWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            webView.getHeight();
            webView.getWidth();

            super.onPageFinished(view, url);
            if (!isUserLoggedOut()) {
                setTimer();
            }
        }

        private void setTimer() {
            //set session timeout to 20 minutes
            int duration = 20;
            int timeout = muzimaTimeout;
            if (timeout < duration) {
                ((MuzimaApplication) getApplication()).resetTimer(duration);
            }
        }
    }
}


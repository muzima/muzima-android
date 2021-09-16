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

import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.TimeoutPreferenceService;
import com.muzima.view.help.BaseHelpActivity;

public class VideoWebViewActivity extends BaseHelpActivity {

    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
    private WebView webView;
    private int muzimaTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_video_web_view);
        setMuzimaTimout();
        setVideoHelpContent();
        logEvent("VIEW_VIDEOS");
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
        webView = findViewById(R.id.videoWebView);
        webView.setWebChromeClient(new WebChromeClient(){});
        webView.setWebViewClient(new videoWebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(makeHtml(), "text/html", "utf-8");
    }

    private String makeHtml() {
        String videoId = getVideoId(getIntent().getStringExtra(VIDEO_PATH));
        String videoUrl = "https://www.youtube.com/embed/" + videoId;
        String html = "<html><head><iframe class='youtube-player' src='" + videoUrl + "' frameborder='0' allowFullScreen></iframe>";
        return html;
    }

    private String getVideoId(String videoUrl) {
        String[] splitUrl = videoUrl.split("/");
        String videoId = splitUrl[splitUrl.length - 1];
        return videoId;
    }

    class videoWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (!isUserLoggedOut()) {
                setTimer();
            }
        }

        private void setTimer() {
            //set session timeout to 20 minutes
            int duration = 20;
            if (muzimaTimeout < duration) {
                ((MuzimaApplication) getApplication()).resetTimer(duration);
            }
        }
    }
}


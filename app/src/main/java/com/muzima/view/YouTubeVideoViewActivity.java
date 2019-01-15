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
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.youtube.player.*;
import com.muzima.R;

public class YouTubeVideoViewActivity extends BaseHelpActivity
        implements YouTubePlayer.OnInitializedListener {

    private static String YOUTUBE_API_KEY = "";
    private static final int RECOVERY_REQUEST = 1;
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
    private String videoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_video_display);
        setVideoContent();
    }

    private void setVideoContent() {
        setTitle(getIntent().getStringExtra(VIDEO_TITLE));
        YouTubePlayerFragment youTubePlayerFragment = (YouTubePlayerFragment) getYouTubePlayerProvider();
        videoId = getVideoId(getIntent().getStringExtra(VIDEO_PATH));
        youTubePlayerFragment.initialize(YOUTUBE_API_KEY, this);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        if (!wasRestored) {
            player.cueVideo(videoId);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        //eg. youtube is disabled - Determines whether this error is user-recoverable.
        if (errorReason.isUserRecoverableError()) {
            startVideoWebViewActivity();
        } else {
            String error = String.format(getString(R.string.youTube_player_error), errorReason.toString());
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_REQUEST) {
            // Retry initialization if user performed a recovery action
            getYouTubePlayerProvider().initialize(YOUTUBE_API_KEY, this);
        }
    }

    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
    }

    private String getVideoId(String videoUrl) {
        String[] splitUrl = videoUrl.split("/");
        String videoId = splitUrl[splitUrl.length - 1];
        return videoId;
    }

    //WebView
    private void startVideoWebViewActivity() {
        Intent videoIntent = new Intent(this, VideoWebViewActivity.class);
        videoIntent.putExtra(VideoWebViewActivity.VIDEO_PATH, getIntent().getStringExtra(VIDEO_PATH));
        videoIntent.putExtra(VideoWebViewActivity.VIDEO_TITLE, getIntent().getStringExtra(VIDEO_TITLE));
        videoIntent.putExtra(VideoWebViewActivity.USER_LOGGED_OUT, String.valueOf(isUserLoggedOut()));
        startActivity(videoIntent);
    }
}
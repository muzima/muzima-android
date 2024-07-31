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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.TimeoutPreferenceService;

public class YouTubeVideoViewActivity extends BaseHelpActivity implements YouTubePlayer.OnInitializedListener {

    private static String YOUTUBE_API_KEY = "AIzaSyB95WSRhfe-Pa6ZxU8ZB3C__E51ZQbZdu8";
    private static final int RECOVERY_REQUEST = 1;
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
    private String videoId;
    private YouTubePlayer youTubePlayer;
    private int muzimaTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_video_display);
        setMuzimaTimout();
        setVideoContent();
        logEvent("VIEW_VIDEOS");
    }

    private void setVideoContent() {
        setTitle(getIntent().getStringExtra(VIDEO_TITLE));
        YouTubePlayerFragment youTubePlayerFragment = (YouTubePlayerFragment) getYouTubePlayerProvider();
        videoId = getVideoId(getIntent().getStringExtra(VIDEO_PATH));
        youTubePlayerFragment.initialize(YOUTUBE_API_KEY, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //set session timeout back to original
        if (!isUserLoggedOut()) {
            ((MuzimaApplication) getApplication()).resetTimer(muzimaTimeout);
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        if (!wasRestored) {
            youTubePlayer = player;
            youTubePlayer.setPlayerStateChangeListener(new MyPlayerStateChangeListener());
            player.loadVideo(videoId);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(HelpActivity.YOUTUBE_API_CANCEL_CASE, HelpActivity.YOUTUBE_INITIALIZATION_FAILURE);
        resultIntent.putExtra(HelpActivity.VIDEO_PATH, getIntent().getStringExtra(VIDEO_PATH));
        resultIntent.putExtra(HelpActivity.VIDEO_TITLE, getIntent().getStringExtra(VIDEO_TITLE));
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
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

    private void setMuzimaTimout() {
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        muzimaTimeout = new TimeoutPreferenceService(muzimaApplication).getTimeout();
    }

    private final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoaded(String s) {
        }

        @Override
        public void onVideoEnded() {
            //set sessiontimeout back
            onBackPressed();
        }

        @Override
        public void onVideoStarted() {
            if (!isUserLoggedOut()) {
                setTimer();
            }
        }

        @Override
        public void onLoading() {
        }

        @Override
        public void onAdStarted() {
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
        }

        private void setTimer() {
            int duration = youTubePlayer.getDurationMillis();
            int timeout = muzimaTimeout * 60 * 1000;
            if (timeout < duration) {
                //set the timeout 2 minutes longet than the video duration
                int newTimeout = duration / 60 / 1000 + 2;
                ((MuzimaApplication) getApplication()).resetTimer(newTimeout);
            }
        }
    }
}

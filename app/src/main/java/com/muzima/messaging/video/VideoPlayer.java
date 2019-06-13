package com.muzima.messaging.video;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.muzima.R;
import com.muzima.messaging.attachments.AttachmentServer;
import com.muzima.messaging.mms.PartAuthority;
import com.muzima.messaging.mms.VideoSlide;
import com.muzima.messaging.video.exo.AttachmentDataSourceFactory;
import com.muzima.utils.ViewUtil;

import java.io.IOException;

public class VideoPlayer extends FrameLayout {

    private static final String TAG = VideoPlayer.class.getSimpleName();

    @Nullable
    private final VideoView videoView;
    @Nullable private final PlayerView exoView;

    @Nullable private SimpleExoPlayer exoPlayer;
    @Nullable private PlayerControlView exoControls;
    @Nullable private AttachmentServer attachmentServer;
    @Nullable private Window window;

    public VideoPlayer(Context context) {
        this(context, null);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.video_player, this);

        if (Build.VERSION.SDK_INT >= 16) {
            this.exoView   = ViewUtil.findById(this, R.id.video_view);
            this.videoView = null;
            this.exoControls = new PlayerControlView(getContext());
            this.exoControls.setShowTimeoutMs(-1);
        } else {
            this.videoView = ViewUtil.findById(this, R.id.video_view);
            this.exoView   = null;
            initializeVideoViewControls(videoView);
        }
    }

    public void setVideoSource(@NonNull VideoSlide videoSource, boolean autoplay)
            throws IOException
    {
        if (Build.VERSION.SDK_INT >= 16) setExoViewSource(videoSource, autoplay);
        else                             setVideoViewSource(videoSource, autoplay);
    }

    public void pause() {
        if (this.attachmentServer != null && this.videoView != null) {
            this.videoView.stopPlayback();
        } else if (this.exoPlayer != null) {
            this.exoPlayer.setPlayWhenReady(false);
        }
    }

    public void hideControls() {
        if (this.exoView != null) {
            this.exoView.hideController();
        }
    }

    public @Nullable
    View getControlView() {
        if (this.exoControls != null) {
            return this.exoControls;
        }
        return null;
    }

    public void cleanup() {
        if (this.attachmentServer != null) {
            this.attachmentServer.stop();
        }

        if (this.exoPlayer != null) {
            this.exoPlayer.release();
        }
    }

    public void setWindow(@Nullable Window window) {
        this.window = window;
    }

    private void setExoViewSource(@NonNull VideoSlide videoSource, boolean autoplay)
            throws IOException
    {
        BandwidthMeter bandwidthMeter             = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector              = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl                = new DefaultLoadControl();

        exoPlayer = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, loadControl);
        exoPlayer.addListener(new ExoPlayerListener(window));
        //noinspection ConstantConditions
        exoView.setPlayer(exoPlayer);
        //noinspection ConstantConditions
        exoControls.setPlayer(exoPlayer);

        DefaultDataSourceFactory defaultDataSourceFactory    = new DefaultDataSourceFactory(getContext(), "GenericUserAgent", null);
        AttachmentDataSourceFactory attachmentDataSourceFactory = new AttachmentDataSourceFactory(getContext(), defaultDataSourceFactory, null);
        ExtractorsFactory extractorsFactory           = new DefaultExtractorsFactory();

        MediaSource mediaSource = new ExtractorMediaSource(videoSource.getUri(), attachmentDataSourceFactory, extractorsFactory, null, null);

        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(autoplay);
    }

    private void setVideoViewSource(@NonNull VideoSlide videoSource, boolean autoplay)
            throws IOException
    {
        if (this.attachmentServer != null) {
            this.attachmentServer.stop();
        }

        if (videoSource.getUri() != null && PartAuthority.isLocalUri(videoSource.getUri())) {
            Log.i(TAG, "Starting video attachment server for part provider Uri...");
            this.attachmentServer = new AttachmentServer(getContext(), videoSource.asAttachment());
            this.attachmentServer.start();

            //noinspection ConstantConditions
            this.videoView.setVideoURI(this.attachmentServer.getUri());
        } else if (videoSource.getUri() != null) {
            Log.i(TAG, "Playing video directly from non-local Uri...");
            //noinspection ConstantConditions
            this.videoView.setVideoURI(videoSource.getUri());
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.error_playing_video), Toast.LENGTH_LONG).show();
            return;
        }

        if (autoplay) this.videoView.start();
    }

    private void initializeVideoViewControls(@NonNull VideoView videoView) {
        MediaController mediaController = new MediaController(getContext());
        mediaController.setAnchorView(videoView);
        mediaController.setMediaPlayer(videoView);

        videoView.setMediaController(mediaController);
    }

    private static class ExoPlayerListener extends Player.DefaultEventListener {
        private final Window window;

        ExoPlayerListener(Window window) {
            this.window = window;
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch(playbackState) {
                case Player.STATE_IDLE:
                case Player.STATE_BUFFERING:
                case Player.STATE_ENDED:
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    break;
                case Player.STATE_READY:
                    if (playWhenReady) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}

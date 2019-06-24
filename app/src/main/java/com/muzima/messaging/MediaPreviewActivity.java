package com.muzima.messaging;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.messaging.attachments.DatabaseAttachment;
import com.muzima.messaging.components.MediaView;
import com.muzima.messaging.components.viewpager.ExtendedOnPageChangedListener;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.MediaDatabase.MediaRecord;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.loaders.PagingMediaLoader;
import com.muzima.messaging.utils.AttachmentUtil;
import com.muzima.messaging.utils.SaveAttachmentTask;
import com.muzima.messaging.utils.Util;
import com.muzima.model.MediaPreviewViewModel;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Permissions;

import java.io.IOException;
import java.util.WeakHashMap;

public class MediaPreviewActivity extends PassphraseRequiredActionBarActivity implements RecipientModifiedListener,
        LoaderManager.LoaderCallbacks<Pair<Cursor, Integer>>,
        AlbumRailAdapter.RailItemClickedListener
{

    private final static String TAG = MediaPreviewActivity.class.getSimpleName();

    public static final String ADDRESS_EXTRA = "address";
    public static final String DATE_EXTRA = "date";
    public static final String SIZE_EXTRA = "size";
    public static final String CAPTION_EXTRA = "caption";
    public static final String OUTGOING_EXTRA = "outgoing";
    public static final String LEFT_IS_RECENT_EXTRA = "left_is_recent";

    private ViewPager mediaPager;
    private View detailsContainer;
    private TextView caption;
    private View captionContainer;
    private RecyclerView albumRail;
    private AlbumRailAdapter albumRailAdapter;
    private ViewGroup playbackControlsContainer;
    private Uri initialMediaUri;
    private String initialMediaType;
    private long initialMediaSize;
    private String initialCaption;
    private SignalRecipient conversationRecipient;
    private boolean leftIsRecent;
    private GestureDetector clickDetector;
    private MediaPreviewViewModel viewModel;

    private int restartItem = -1;


    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle bundle, boolean ready) {
        hideActionBar();
        viewModel = ViewModelProviders.of(this).get(MediaPreviewViewModel.class);

        setFullscreenIfPossible();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.media_preview_activity);

        initializeViews();
        initializeResources();
        initializeObservers();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        clickDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setFullscreenIfPossible() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onModified(SignalRecipient recipient) {
        Util.runOnMain(this::initializeActionBar);
    }

    @Override
    public void onRailItemClicked(int distanceFromActive) {
        mediaPager.setCurrentItem(mediaPager.getCurrentItem() + distanceFromActive);
    }

    @SuppressWarnings("ConstantConditions")
    private void initializeActionBar() {
        MediaItem mediaItem = getCurrentMediaItem();

        if (mediaItem != null) {
            CharSequence relativeTimeSpan;

            if (mediaItem.date > 0) {
                relativeTimeSpan = DateUtils.getExtendedRelativeTimeSpanString(this, getResources().getConfiguration().locale, mediaItem.date);
            } else {
                relativeTimeSpan = getString(R.string.general_draft);
            }

            if (mediaItem.outgoing) getSupportActionBar().setTitle(getString(R.string.general_you));
            else if (mediaItem.recipient != null) getSupportActionBar().setTitle(mediaItem.recipient.toShortString());
            else getSupportActionBar().setTitle("");

            getSupportActionBar().setSubtitle(relativeTimeSpan);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initializeMedia();
    }

    @Override
    public void onPause() {
        super.onPause();
        restartItem = cleanupMedia();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initializeResources();
    }

    private void hideActionBar(){
        try
        {
            this.getSupportActionBar().hide();
        }  catch (NullPointerException e){

        }
    }

    private void initializeViews() {
        mediaPager = findViewById(R.id.media_pager);
        mediaPager.setOffscreenPageLimit(1);
        mediaPager.addOnPageChangeListener(new ViewPagerListener());

        albumRail = findViewById(R.id.media_preview_album_rail);
        albumRailAdapter = new AlbumRailAdapter(GlideApp.with(this), this);

        albumRail.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        albumRail.setAdapter(albumRailAdapter);

        detailsContainer = findViewById(R.id.media_preview_details_container);
        caption = findViewById(R.id.media_preview_caption);
        captionContainer = findViewById(R.id.media_preview_caption_container);
        playbackControlsContainer = findViewById(R.id.media_preview_playback_controls_container);
    }

    private void initializeResources() {
        SignalAddress address = getIntent().getParcelableExtra(ADDRESS_EXTRA);

        initialMediaUri = getIntent().getData();
        initialMediaType = getIntent().getType();
        initialMediaSize = getIntent().getLongExtra(SIZE_EXTRA, 0);
        initialCaption = getIntent().getStringExtra(CAPTION_EXTRA);
        leftIsRecent = getIntent().getBooleanExtra(LEFT_IS_RECENT_EXTRA, false);
        restartItem = -1;

        if (address != null) {
            conversationRecipient = SignalRecipient.from(this, address, true);
        } else {
            conversationRecipient = null;
        }
    }

    private void initializeObservers() {
        viewModel.getPreviewData().observe(this, previewData -> {
            if (previewData == null || mediaPager == null || mediaPager.getAdapter() == null) {
                return;
            }

            View playbackControls = ((MediaItemAdapter) mediaPager.getAdapter()).getPlaybackControls(mediaPager.getCurrentItem());

            if (previewData.getAlbumThumbnails().isEmpty() && previewData.getCaption() == null && playbackControls == null) {
                detailsContainer.setVisibility(View.GONE);
            } else {
                detailsContainer.setVisibility(View.VISIBLE);
            }

            albumRail.setVisibility(previewData.getAlbumThumbnails().isEmpty() ? View.GONE : View.VISIBLE);
            albumRailAdapter.setRecords(previewData.getAlbumThumbnails(), previewData.getActivePosition());
            albumRail.smoothScrollToPosition(previewData.getActivePosition());

            captionContainer.setVisibility(previewData.getCaption() == null ? View.GONE : View.VISIBLE);
            caption.setText(previewData.getCaption());

            if (playbackControls != null) {
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                playbackControls.setLayoutParams(params);

                playbackControlsContainer.removeAllViews();
                playbackControlsContainer.addView(playbackControls);
            } else {
                playbackControlsContainer.removeAllViews();
            }
        });

        clickDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (e.getY() < detailsContainer.getTop()) {
                    detailsContainer.setVisibility(detailsContainer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
                return super.onSingleTapUp(e);
            }
        });
    }

    private void initializeMedia() {
        if (!isContentTypeSupported(initialMediaType)) {
            Log.w(TAG, "Unsupported media type sent to MediaPreviewActivity, finishing.");
            Toast.makeText(getApplicationContext(), R.string.warning_unssuported_media_type, Toast.LENGTH_LONG).show();
            finish();
        }

        Log.i(TAG, "Loading Part URI: " + initialMediaUri);

        if (conversationRecipient != null) {
            getSupportLoaderManager().restartLoader(0, null, this);
        } else {
            mediaPager.setAdapter(new SingleItemPagerAdapter(this, GlideApp.with(this), getWindow(), initialMediaUri, initialMediaType, initialMediaSize));

            if (initialCaption != null) {
                detailsContainer.setVisibility(View.VISIBLE);
                captionContainer.setVisibility(View.VISIBLE);
                caption.setText(initialCaption);
            }
        }
    }

    private int cleanupMedia() {
        int restartItem = mediaPager.getCurrentItem();

        mediaPager.removeAllViews();
        mediaPager.setAdapter(null);

        return restartItem;
    }

    private void showOverview() {
        Intent intent = new Intent(this, MediaOverviewActivity.class);
        intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, conversationRecipient.getAddress());
        startActivity(intent);
    }

    private void forward() {
        MediaItem mediaItem = getCurrentMediaItem();

        if (mediaItem != null) {
            Intent composeIntent = new Intent(this, ShareActivity.class);
            composeIntent.putExtra(Intent.EXTRA_STREAM, mediaItem.uri);
            composeIntent.setType(mediaItem.type);
            startActivity(composeIntent);
        }
    }

    @SuppressWarnings("CodeBlock2Expr")
    @SuppressLint("InlinedApi")
    private void saveToDisk() {
        MediaItem mediaItem = getCurrentMediaItem();

        if (mediaItem != null) {
            SaveAttachmentTask.showWarningDialog(this, (dialogInterface, i) -> {
                Permissions.with(this)
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                        .ifNecessary()
                        .withPermanentDenialDialog(getString(R.string.warning_permission_to_write_to_external_denied))
                        .onAnyDenied(() -> Toast.makeText(this, R.string.warning_requires_permission_to_write_to_external_storage, Toast.LENGTH_LONG).show())
                        .onAllGranted(() -> {
                            SaveAttachmentTask saveTask = new SaveAttachmentTask(MediaPreviewActivity.this);
                            long saveDate = (mediaItem.date > 0) ? mediaItem.date : System.currentTimeMillis();
                            saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new SaveAttachmentTask.Attachment(mediaItem.uri, mediaItem.type, saveDate, null));
                        })
                        .execute();
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void deleteMedia() {
        MediaItem mediaItem = getCurrentMediaItem();
        if (mediaItem == null || mediaItem.attachment == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setTitle(R.string.prompt_delete_message);
        builder.setMessage(R.string.prompt_confirm_media_deletion);
        builder.setCancelable(true);

        builder.setPositiveButton(R.string.general_delete, (dialogInterface, which) -> {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    if (mediaItem.attachment == null) {
                        return null;
                    }
                    AttachmentUtil.deleteAttachment(MediaPreviewActivity.this.getApplicationContext(),
                            mediaItem.attachment);
                    return null;
                }
            }.execute();

            finish();
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.media_preview, menu);

        if (!isMediaInDb()) {
            menu.findItem(R.id.media_preview__overview).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.media_preview__overview:
                 showOverview();
                 return true;
            case R.id.media_preview__forward:
                 forward();
                 return true;
            case R.id.save:
                 saveToDisk();
                 return true;
            case R.id.delete:
                 deleteMedia();
                 return true;
            case android.R.id.home:
                 finish();
                 return true;
        }

        return false;
    }

    private boolean isMediaInDb() {
        return conversationRecipient != null;
    }

    private @Nullable MediaItem getCurrentMediaItem() {
        MediaItemAdapter adapter = (MediaItemAdapter)mediaPager.getAdapter();

        if (adapter != null) {
            return adapter.getMediaItemFor(mediaPager.getCurrentItem());
        } else {
            return null;
        }
    }

    public static boolean isContentTypeSupported(final String contentType) {
        return contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"));
    }

    @Override
    public Loader<Pair<Cursor, Integer>> onCreateLoader(int id, Bundle args) {
        return new PagingMediaLoader(this, conversationRecipient, initialMediaUri, leftIsRecent);
    }

    @Override
    public void onLoadFinished(Loader<Pair<Cursor, Integer>> loader, @Nullable Pair<Cursor, Integer> data) {
        if (data != null) {
            @SuppressWarnings("ConstantConditions")
            CursorPagerAdapter adapter = new CursorPagerAdapter(this, GlideApp.with(this), getWindow(), data.first, data.second, leftIsRecent);
            mediaPager.setAdapter(adapter);
            adapter.setActive(true);

            viewModel.setCursor(data.first, leftIsRecent);

            if (restartItem < 0) mediaPager.setCurrentItem(data.second);
            else mediaPager.setCurrentItem(restartItem);
        }
    }

    @Override
    public void onLoaderReset(Loader<Pair<Cursor, Integer>> loader) {

    }

    private class ViewPagerListener extends ExtendedOnPageChangedListener {

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);

            MediaItemAdapter adapter = (MediaItemAdapter)mediaPager.getAdapter();

            if (adapter != null) {
                MediaItem item = adapter.getMediaItemFor(position);
                if (item.recipient != null) item.recipient.addListener(MediaPreviewActivity.this);
                viewModel.setActiveAlbumRailItem(MediaPreviewActivity.this, position);
                initializeActionBar();
            }
        }


        @Override
        public void onPageUnselected(int position) {
            MediaItemAdapter adapter = (MediaItemAdapter)mediaPager.getAdapter();

            if (adapter != null) {
                MediaItem item = adapter.getMediaItemFor(position);
                if (item.recipient != null) item.recipient.removeListener(MediaPreviewActivity.this);

                adapter.pause(position);
            }
        }
    }

    private static class SingleItemPagerAdapter extends PagerAdapter implements MediaItemAdapter {

        private final GlideRequests glideRequests;
        private final Window window;
        private final Uri uri;
        private final String mediaType;
        private final long size;

        private final LayoutInflater inflater;

        SingleItemPagerAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests,
                               @NonNull Window window, @NonNull Uri uri, @NonNull String mediaType,
                               long size)
        {
            this.glideRequests = glideRequests;
            this.window = window;
            this.uri = uri;
            this.mediaType = mediaType;
            this.size = size;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
            View itemView = inflater.inflate(R.layout.media_view_page, container, false);
            MediaView mediaView = itemView.findViewById(R.id.media_view);

            try {
                mediaView.set(glideRequests, window, uri, mediaType, size, true);
            } catch (IOException e) {
                Log.w(TAG, e);
            }

            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            MediaView mediaView = ((FrameLayout)object).findViewById(R.id.media_view);
            mediaView.cleanup();

            container.removeView((FrameLayout)object);
        }

        @Override
        public MediaItem getMediaItemFor(int position) {
            return new MediaItem(null, null, uri, mediaType, -1, true);
        }

        @Override
        public void pause(int position) {

        }

        @Override
        public @Nullable View getPlaybackControls(int position) {
            return null;
        }
    }

    private static class CursorPagerAdapter extends PagerAdapter implements MediaItemAdapter {

        private final WeakHashMap<Integer, MediaView> mediaViews = new WeakHashMap<>();

        private final Context context;
        private final GlideRequests glideRequests;
        private final Window window;
        private final Cursor cursor;
        private final boolean leftIsRecent;

        private boolean active;
        private int     autoPlayPosition;

        CursorPagerAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests,
                           @NonNull Window window, @NonNull Cursor cursor, int autoPlayPosition,
                           boolean leftIsRecent)
        {
            this.context = context.getApplicationContext();
            this.glideRequests = glideRequests;
            this.window = window;
            this.cursor = cursor;
            this.autoPlayPosition = autoPlayPosition;
            this.leftIsRecent = leftIsRecent;
        }

        public void setActive(boolean active) {
            this.active = active;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (!active) return 0;
            else return cursor.getCount();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public @NonNull Object instantiateItem(@NonNull ViewGroup container, int position) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.media_view_page, container, false);
            MediaView mediaView = itemView.findViewById(R.id.media_view);
            boolean autoplay = position == autoPlayPosition;
            int cursorPosition = getCursorPosition(position);

            autoPlayPosition = -1;

            cursor.moveToPosition(cursorPosition);

            MediaRecord mediaRecord = MediaRecord.from(context, cursor);

            try {
                //noinspection ConstantConditions
                mediaView.set(glideRequests, window, mediaRecord.getAttachment().getDataUri(),
                        mediaRecord.getAttachment().getContentType(), mediaRecord.getAttachment().getSize(), autoplay);
            } catch (IOException e) {
                Log.w(TAG, e);
            }

            mediaViews.put(position, mediaView);
            container.addView(itemView);

            return itemView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            MediaView mediaView = ((FrameLayout)object).findViewById(R.id.media_view);
            mediaView.cleanup();

            mediaViews.remove(position);
            container.removeView((FrameLayout)object);
        }

        public MediaItem getMediaItemFor(int position) {
            cursor.moveToPosition(getCursorPosition(position));
            MediaRecord mediaRecord = MediaRecord.from(context, cursor);
            SignalAddress address = mediaRecord.getAddress();

            if (mediaRecord.getAttachment().getDataUri() == null) throw new AssertionError();

            return new MediaItem(address != null ? SignalRecipient.from(context, address,true) : null,
                    mediaRecord.getAttachment(),
                    mediaRecord.getAttachment().getDataUri(),
                    mediaRecord.getContentType(),
                    mediaRecord.getDate(),
                    mediaRecord.isOutgoing());
        }

        @Override
        public void pause(int position) {
            MediaView mediaView = mediaViews.get(position);
            if (mediaView != null) mediaView.pause();
        }

        @Override
        public @Nullable View getPlaybackControls(int position) {
            MediaView mediaView = mediaViews.get(position);
            if (mediaView != null) return mediaView.getPlaybackControls();
            return null;
        }

        private int getCursorPosition(int position) {
            if (leftIsRecent) return position;
            else return cursor.getCount() - 1 - position;
        }
    }

    private static class MediaItem {
        private final @Nullable SignalRecipient recipient;
        private final @Nullable DatabaseAttachment attachment;
        private final @NonNull Uri uri;
        private final @NonNull String type;
        private final long date;
        private final boolean outgoing;

        private MediaItem(@Nullable SignalRecipient recipient,
                          @Nullable DatabaseAttachment attachment,
                          @NonNull Uri uri,
                          @NonNull String type,
                          long date,
                          boolean outgoing)
        {
            this.recipient = recipient;
            this.attachment = attachment;
            this.uri = uri;
            this.type = type;
            this.date = date;
            this.outgoing = outgoing;
        }
    }

    interface MediaItemAdapter {
        MediaItem getMediaItemFor(int position);
        void pause(int position);
        @Nullable View getPlaybackControls(int position);
    }
}

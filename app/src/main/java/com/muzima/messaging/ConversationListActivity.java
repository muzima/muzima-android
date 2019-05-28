package com.muzima.messaging;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.TooltipCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.muzima.R;
import com.muzima.messaging.contacts.avatars.ContactColors;
import com.muzima.messaging.contacts.avatars.GeneratedContactPhoto;
import com.muzima.messaging.contacts.avatars.ProfileContactPhoto;
import com.muzima.messaging.customcomponents.SearchToolbar;
import com.muzima.messaging.fragments.SearchFragment;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.MarkedMessageInfo;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MarkReadReceiver;
import com.muzima.notifications.MessageNotifier;
import com.muzima.service.KeyCachingService;
import com.muzima.utils.MaterialColor;
import com.muzima.utils.Permissions;
import com.muzima.utils.concurrent.LifecycleBoundTask;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.List;

public class ConversationListActivity extends PassphraseRequiredActionBarActivity
        implements ConversationListFragment.ConversationSelectedListener
{
    @SuppressWarnings("unused")
    private static final String TAG = ConversationListActivity.class.getSimpleName();

    private ConversationListFragment conversationListFragment;
    private SearchFragment searchFragment;
    private SearchToolbar searchToolbar;
    private ImageView searchAction;
    private ViewGroup fragmentContainer;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
        setContentView(R.layout.conversation_list_activity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchToolbar            = findViewById(R.id.search_toolbar);
        searchAction             = findViewById(R.id.search_action);
        fragmentContainer        = findViewById(R.id.fragment_container);
        conversationListFragment = initFragment(R.id.fragment_container, new ConversationListFragment(), getResources().getConfiguration().locale);

        initializeSearchListener();

        // RatingManager.showRatingDialogIfNecessary(this);
       // RegistrationLockDialog.showReminderIfNecessary(this);

        TooltipCompat.setTooltipText(searchAction, getText(R.string.search_for_conversations_contacts_and_messages));
    }

    @Override
    public void onResume() {
        super.onResume();
        LifecycleBoundTask.run(getLifecycle(), () -> {
            return SignalRecipient.from(this, SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(this)), false);
        }, this::initializeProfileIcon);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.text_secure_normal, menu);

        menu.findItem(R.id.menu_clear_passphrase).setVisible(!TextSecurePreferences.isPasswordDisabled(this));

        super.onPrepareOptionsMenu(menu);
        return true;
    }

    private void initializeSearchListener() {
        searchAction.setOnClickListener(v -> {
            Permissions.with(this)
                    .request(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                    .ifNecessary()
                    .onAllGranted(() -> searchToolbar.display(searchAction.getX() + (searchAction.getWidth() / 2),
                            searchAction.getY() + (searchAction.getHeight() / 2)))
                    .withPermanentDenialDialog(getString(R.string.app_needs_contacts_permission_in_order_to_search_your_contacts_but_it_has_been_permanently_denied))
                    .execute();
        });

        searchToolbar.setListener(new SearchToolbar.SearchListener() {
            @Override
            public void onSearchTextChange(String text) {
                String trimmed = text.trim();

                if (trimmed.length() > 0) {
                    if (searchFragment == null) {
                        searchFragment = SearchFragment.newInstance(getResources().getConfiguration().locale);
                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, searchFragment, null)
                                .commit();
                    }
                    searchFragment.updateSearchQuery(trimmed);
                } else if (searchFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(searchFragment)
                            .commit();
                    searchFragment = null;
                }
            }

            @Override
            public void onSearchClosed() {
                if (searchFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(searchFragment)
                            .commit();
                    searchFragment = null;
                }
            }
        });
    }

    private void initializeProfileIcon(@NonNull SignalRecipient recipient) {
        ImageView     icon          = findViewById(R.id.toolbar_icon);
        String        name          = Optional.fromNullable(recipient.getName()).or(Optional.fromNullable(TextSecurePreferences.getProfileName(this))).or("");
        MaterialColor fallbackColor = recipient.getColor();

        if (fallbackColor == ContactColors.UNKNOWN_COLOR && !TextUtils.isEmpty(name)) {
            fallbackColor = ContactColors.generateFor(name);
        }

        Drawable fallback = new GeneratedContactPhoto(name, R.drawable.ic_profile_default).asDrawable(this, fallbackColor.toAvatarColor(this));

        GlideApp.with(this)
                .load(new ProfileContactPhoto(recipient.getAddress(), String.valueOf(TextSecurePreferences.getProfileAvatarId(this))))
                .error(fallback)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(icon);

        icon.setOnClickListener(v -> handleDisplaySettings());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
//            case R.id.menu_new_group:         createGroup();           return true;
//            case R.id.menu_settings:          handleDisplaySettings(); return true;
//            case R.id.menu_clear_passphrase:  handleClearPassphrase(); return true;
//            case R.id.menu_mark_all_read:     handleMarkAllRead();     return true;
//            case R.id.menu_invite:            handleInvite();          return true;
            case R.id.menu_help:              handleHelp();            return true;
        }

        return false;
    }

    @Override
    public void onCreateConversation(long threadId, SignalRecipient recipient, int distributionType, long lastSeen) {
        openConversation(threadId, recipient, distributionType, lastSeen, -1);
    }

    public void openConversation(long threadId, SignalRecipient recipient, int distributionType, long lastSeen, int startingPosition) {
        searchToolbar.clearFocus();

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, distributionType);
        intent.putExtra(ConversationActivity.TIMING_EXTRA, System.currentTimeMillis());
        intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, lastSeen);
        intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, startingPosition);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
    }

    @Override
    public void onSwitchToArchive() {
        Intent intent = new Intent(this, ConversationListArchiveActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (searchToolbar.isVisible()) searchToolbar.collapse();
        else                           super.onBackPressed();
    }

    private void createGroup() {
        //TODO +++Work on GroupCreateActivity
//        Intent intent = new Intent(this, GroupCreateActivity.class);
//        startActivity(intent);
    }

    private void handleDisplaySettings() {
//        Intent preferencesIntent = new Intent(this, ApplicationPreferencesActivity.class);
//        startActivity(preferencesIntent);
    }

    private void handleClearPassphrase() {
        Intent intent = new Intent(this, KeyCachingService.class);
        intent.setAction(KeyCachingService.CLEAR_KEY_ACTION);
        startService(intent);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleMarkAllRead() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Context context    = ConversationListActivity.this;
                List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setAllThreadsRead();

                MessageNotifier.updateNotification(context);
                MarkReadReceiver.process(context, messageIds);

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleInvite() {
        //TODO +++Work on InviteActivity
        //startActivity(new Intent(this, InviteActivity.class));
    }

    private void handleHelp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://muzima.org")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.ConversationListActivity_there_is_no_browser_installed_on_your_device, Toast.LENGTH_LONG).show();
        }
    }
}

package com.muzima.messaging;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.places.ui.PlacePicker;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.animations.AnimatingToggle;
import com.muzima.messaging.audio.AudioRecorder;
import com.muzima.messaging.audio.AudioSlidePlayer;
import com.muzima.messaging.camera.CameraActivity;
import com.muzima.messaging.components.ComposeText;
import com.muzima.messaging.components.HidingLinearLayout;
import com.muzima.messaging.components.InputPanel;
import com.muzima.messaging.components.KeyboardAwareLinearLayout.OnKeyboardShownListener;
import com.muzima.messaging.components.identity.UntrustedSendDialog;
import com.muzima.messaging.contacts.ContactAccessor;
import com.muzima.messaging.contacts.ContactAccessor.ContactData;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.messaging.contactshare.ContactShareEditActivity;
import com.muzima.messaging.contactshare.ContactUtil;
import com.muzima.messaging.crypto.IdentityKeyParcelable;
import com.muzima.messaging.crypto.SecurityEvent;
import com.muzima.messaging.customcomponents.AttachmentTypeSelector;
import com.muzima.messaging.customcomponents.ConversationTitleView;
import com.muzima.messaging.customcomponents.InputAwareLayout;
import com.muzima.messaging.customcomponents.ReminderView;
import com.muzima.messaging.customcomponents.UnverifiedBannerView;
import com.muzima.messaging.components.identity.UnverifiedSendDialog;
import com.muzima.messaging.customcomponents.emoji.EmojiDrawer;
import com.muzima.messaging.customcomponents.emoji.EmojiStrings;
import com.muzima.messaging.dialogs.ExpirationDialog;
import com.muzima.messaging.dialogs.MuteDialog;
import com.muzima.messaging.events.ReminderUpdateEvent;
import com.muzima.messaging.exceptions.RecipientFormattingException;
import com.muzima.messaging.fragments.ConversationFragment;
import com.muzima.messaging.group.GroupShareProfileView;
import com.muzima.messaging.jobs.MultiDeviceBlockedUpdateJob;
import com.muzima.messaging.jobs.ServiceOutageDetectionJob;
import com.muzima.messaging.location.SignalPlace;
import com.muzima.messaging.mms.AttachmentManager;
import com.muzima.messaging.mms.AudioSlide;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.mms.ImageSlide;
import com.muzima.messaging.mms.LocationSlide;
import com.muzima.messaging.mms.MediaConstraints;
import com.muzima.messaging.mms.OutgoingExpirationUpdateMessage;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.mms.OutgoingSecureMediaMessage;
import com.muzima.messaging.mms.QuoteId;
import com.muzima.messaging.mms.QuoteModel;
import com.muzima.messaging.mms.Slide;
import com.muzima.messaging.mms.SlideDeck;
import com.muzima.messaging.net.TransportOption;
import com.muzima.messaging.net.TransportOption.Type;
import com.muzima.messaging.provider.PersistentBlobProvider;
import com.muzima.messaging.scribbles.ScribbleActivity;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sms.OutgoingEncryptedMessage;
import com.muzima.messaging.sms.OutgoingEndSessionMessage;
import com.muzima.messaging.sms.OutgoingTextMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.DraftDatabase;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.IdentityDatabase;
import com.muzima.messaging.sqlite.database.MessagingDatabase;
import com.muzima.messaging.sqlite.database.MmsSmsColumns;
import com.muzima.messaging.sqlite.database.RecipientDatabase.RegisteredState;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.sqlite.database.DraftDatabase.Draft;
import com.muzima.messaging.sqlite.database.DraftDatabase.Drafts;
import com.muzima.messaging.sqlite.database.models.MmsMessageRecord;
import com.muzima.messaging.twofactoraunthentication.RegistrationActivity;
import com.muzima.messaging.utils.CharacterCalculator;
import com.muzima.messaging.utils.Dialogs;
import com.muzima.messaging.utils.DirectoryHelper;
import com.muzima.messaging.utils.ExpirationUtil;
import com.muzima.messaging.sqlite.database.IdentityDatabase.IdentityRecord;
import com.muzima.messaging.utils.IdentityUtil;
import com.muzima.messaging.utils.SimpleTextWatcher;
import com.muzima.messaging.utils.Stub;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MarkReadReceiver;
import com.muzima.notifications.MessageNotifier;
import com.muzima.service.KeyCachingService;
import com.muzima.utils.MaterialColor;
import com.muzima.utils.MediaUtil;
import com.muzima.utils.Permissions;
import com.muzima.utils.ServiceUtil;
import com.muzima.utils.ViewUtil;
import com.muzima.utils.concurrent.AssertedSuccessListener;
import com.muzima.messaging.QuickAttachmentDrawer.AttachmentDrawerListener;
import com.muzima.messaging.mms.AttachmentManager.MediaType;
import com.muzima.utils.concurrent.ListenableFuture;
import com.muzima.utils.concurrent.SettableFuture;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

@SuppressLint("StaticFieldLeak")
public class ConversationActivity extends PassphraseRequiredActionBarActivity
        implements ConversationFragment.ConversationFragmentListener,
        AttachmentManager.AttachmentListener,
        RecipientModifiedListener,
        OnKeyboardShownListener,
        AttachmentDrawerListener,
        InputPanel.Listener,
        InputPanel.MediaListener {
    private static final String TAG = ConversationActivity.class.getSimpleName();

    public static final String ADDRESS_EXTRA = "address";
    public static final String THREAD_ID_EXTRA = "thread_id";
    public static final String IS_ARCHIVED_EXTRA = "is_archived";
    public static final String TEXT_EXTRA = "draft_text";
    public static final String DISTRIBUTION_TYPE_EXTRA = "distribution_type";
    public static final String TIMING_EXTRA = "timing";
    public static final String LAST_SEEN_EXTRA = "last_seen";
    public static final String STARTING_POSITION_EXTRA = "starting_position";

    private static final int PICK_GALLERY = 1;
    private static final int PICK_DOCUMENT = 2;
    private static final int PICK_AUDIO = 3;
    private static final int PICK_CONTACT = 4;
    private static final int GET_CONTACT_DETAILS = 5;
    private static final int GROUP_EDIT = 6;
    private static final int TAKE_PHOTO = 7;
    private static final int ADD_CONTACT = 8;
    private static final int PICK_LOCATION = 9;
    private static final int SMS_DEFAULT = 11;
    private static final int PICK_CAMERA = 12;
    private static final int EDIT_IMAGE = 13;

    private GlideRequests glideRequests;
    protected ComposeText composeText;
    private AnimatingToggle buttonToggle;
    private SendButton sendButton;
    private ImageButton attachButton;
    protected ConversationTitleView titleView;
    private TextView charactersLeft;
    private ConversationFragment fragment;
    private Button unblockButton;
    private Button makeDefaultSmsButton;
    private Button registerButton;
    private InputAwareLayout container;
    private View composePanel;
    protected Stub<ReminderView> reminderView;
    private Stub<UnverifiedBannerView> unverifiedBannerView;
    private Stub<GroupShareProfileView> groupShareProfileView;
    private TypingStatusTextWatcher typingTextWatcher;

    private AttachmentTypeSelector attachmentTypeSelector;
    private AttachmentManager attachmentManager;
    private AudioRecorder audioRecorder;
    private BroadcastReceiver securityUpdateReceiver;
    private Stub<EmojiDrawer> emojiDrawerStub;
    protected HidingLinearLayout quickAttachmentToggle;
    protected HidingLinearLayout inlineAttachmentToggle;
    private QuickAttachmentDrawer quickAttachmentDrawer;
    private InputPanel inputPanel;

    private SignalRecipient recipient;
    private long threadId;
    private int distributionType;
    private boolean archived;
    private boolean isSecureText;
    private boolean isDefaultSms = true;
    private boolean isMmsEnabled = true;
    private boolean isSecurityInitialized = false;

    private final IdentityRecordList identityRecords = new IdentityRecordList();

    @Override
    protected void onPreCreate() {

    }

    @Override
    protected void onCreate(Bundle state, boolean ready) {
        Log.i(TAG, "onCreate()");

        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.conversation_activity);

        TypedArray typedArray = obtainStyledAttributes(new int[]{R.attr.conversation_background});
        int color = typedArray.getColor(0, Color.WHITE);
        typedArray.recycle();

        getWindow().getDecorView().setBackgroundColor(color);

        fragment = initFragment(R.id.fragment_content, new ConversationFragment(), getResources().getConfiguration().locale);

        initializeReceivers();
        initializeActionBar();
        initializeViews();
        initializeResources();
        initializeSecurity(false, isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                initializeProfiles();
                initializeDraft().addListener(new AssertedSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean loadedDraft) {
                        if (loadedDraft != null && loadedDraft) {
                            Log.i(TAG, "Finished loading draft");
                            Util.runOnMain(() -> {
                                if (fragment != null && fragment.isResumed()) {
                                    fragment.moveToLastSeen();
                                } else {
                                    Log.w(TAG, "Wanted to move to the last seen position, but the fragment was in an invalid state");
                                }
                            });
                        }

                        if (TextSecurePreferences.isTypingIndicatorsEnabled(ConversationActivity.this)) {
                            composeText.addTextChangedListener(typingTextWatcher);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(TAG, "onNewIntent()");

        if (isFinishing()) {
            Log.w(TAG, "Activity is finishing...");
            return;
        }

        if (!Util.isEmpty(composeText) || attachmentManager.isAttachmentPresent()) {
            saveDraft();
            attachmentManager.clear(glideRequests, false);
            silentlySetComposeText("");
        }

        setIntent(intent);
        initializeResources();
        initializeSecurity(false, isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                initializeDraft();
            }
        });

        if (fragment != null) {
            fragment.onNewIntent();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        quickAttachmentDrawer.onResume();

        initializeEnabledCheck();
        initializeMmsEnabledCheck();
        initializeIdentityRecords();
        composeText.setTransport(sendButton.getSelectedTransport());

        titleView.setTitle(glideRequests, recipient);
        setActionBarColor(recipient.getColor());
        setBlockedUserState(recipient, isSecureText, isDefaultSms);
        setGroupShareProfileReminder(recipient);
        calculateCharactersRemaining();

        MessageNotifier.setVisibleThread(threadId);
        markThreadAsRead();

        Log.i(TAG, "onResume() Finished: " + (System.currentTimeMillis() - getIntent().getLongExtra(TIMING_EXTRA, 0)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        MessageNotifier.setVisibleThread(-1L);
        if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
        quickAttachmentDrawer.onPause();
        inputPanel.onPause();

        fragment.setLastSeen(System.currentTimeMillis());
        markLastSeen();
        AudioSlidePlayer.stopAll();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i(TAG, "onConfigurationChanged(" + newConfig.orientation + ")");
        super.onConfigurationChanged(newConfig);
        composeText.setTransport(sendButton.getSelectedTransport());
        quickAttachmentDrawer.onConfigurationChanged();

        if (emojiDrawerStub.resolved() && container.getCurrentInput() == emojiDrawerStub.get()) {
            container.hideAttachedInput(true);
        }
    }

    @Override
    protected void onDestroy() {
        saveDraft();
        if (recipient != null) recipient.removeListener(this);
        if (securityUpdateReceiver != null) unregisterReceiver(securityUpdateReceiver);
        super.onDestroy();
    }

    @Override
    public void onActivityResult(final int reqCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult called: " + reqCode + ", " + resultCode + " , " + data);
        super.onActivityResult(reqCode, resultCode, data);

        if ((data == null && reqCode != TAKE_PHOTO && reqCode != SMS_DEFAULT) ||
                (resultCode != RESULT_OK && reqCode != SMS_DEFAULT)) {
            return;
        }

        switch (reqCode) {
            case PICK_GALLERY:
                MediaType mediaType;

                String mimeType = MediaUtil.getMimeType(this, data.getData());

                if (MediaUtil.isGif(mimeType)) mediaType = MediaType.GIF;
                else if (MediaUtil.isVideo(mimeType)) mediaType = MediaType.VIDEO;
                else mediaType = MediaType.IMAGE;

                setMedia(data.getData(), mediaType);

                break;
            case PICK_DOCUMENT:
                setMedia(data.getData(), MediaType.DOCUMENT);
                break;
            case PICK_AUDIO:
                setMedia(data.getData(), MediaType.AUDIO);
                break;
            case PICK_CONTACT:
                if (isSecureText && !isSmsForced()) {
                    openContactShareEditor(data.getData());
                } else {
                    addAttachmentContactInfo(data.getData());
                }
                break;
            case GET_CONTACT_DETAILS:
                sendSharedContact(data.getParcelableArrayListExtra(ContactShareEditActivity.KEY_CONTACTS));
                break;
            case GROUP_EDIT:
//                recipient = SignalRecipient.from(this, data.getParcelableExtra(GroupCreateActivity.GROUP_ADDRESS_EXTRA), true);
//                recipient.addListener(this);
//                titleView.setTitle(glideRequests, recipient);
//                NotificationChannels.updateContactChannelName(this, recipient);
//                setBlockedUserState(recipient, isSecureText, isDefaultSms);
//                supportInvalidateOptionsMenu();
                break;
            case TAKE_PHOTO:
                if (attachmentManager.getCaptureUri() != null) {
                    setMedia(attachmentManager.getCaptureUri(), MediaType.IMAGE);
                }
                break;
            case ADD_CONTACT:
                recipient = SignalRecipient.from(this, recipient.getAddress(), true);
                recipient.addListener(this);
                fragment.reloadList();
                break;
            case PICK_LOCATION:
                SignalPlace place = new SignalPlace(PlacePicker.getPlace(data, this));
                attachmentManager.setLocation(place, getCurrentMediaConstraints());
                break;
            case ScribbleActivity.SCRIBBLE_REQUEST_CODE:
                setMedia(data.getData(), MediaType.IMAGE);
                break;
            case SMS_DEFAULT:
                initializeSecurity(isSecureText, isDefaultSms);
                break;
            case PICK_CAMERA:
                int imageWidth = data.getIntExtra(CameraActivity.EXTRA_WIDTH, 0);
                int imageHeight = data.getIntExtra(CameraActivity.EXTRA_HEIGHT, 0);
                long imageSize = data.getLongExtra(CameraActivity.EXTRA_SIZE, 0);
                TransportOption transport = data.getParcelableExtra(CameraActivity.EXTRA_TRANSPORT);
                String message = data.getStringExtra(CameraActivity.EXTRA_MESSAGE);
                SlideDeck slideDeck = new SlideDeck();
                long expiresIn = recipient.getExpireMessages() * 1000L;
                int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
                boolean initiating = threadId == -1;

                if (transport == null) {
                    throw new IllegalStateException("Received a null transport from the CameraActivity.");
                }

                sendButton.setTransport(transport);

                slideDeck.addSlide(new ImageSlide(this, data.getData(), imageSize, imageWidth, imageHeight));

                sendMediaMessage(transport.isSms(), message, slideDeck, Collections.emptyList(), expiresIn, subscriptionId, initiating);
                break;
        }
    }

    @Override
    public void startActivity(Intent intent) {
        if (intent.getStringExtra(Browser.EXTRA_APPLICATION_ID) != null) {
            intent.removeExtra(Browser.EXTRA_APPLICATION_ID);
        }

        try {
            super.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, e);
            Toast.makeText(this, R.string.ConversationActivity_there_is_no_app_available_to_handle_this_link_on_your_device, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        if (isSecureText) {
            if (recipient.getExpireMessages() > 0) {
                inflater.inflate(R.menu.conversation_expiring_on, menu);

                final MenuItem item = menu.findItem(R.id.menu_expiring_messages);
                final View actionView = MenuItemCompat.getActionView(item);
                final TextView badgeView = actionView.findViewById(R.id.expiration_badge);

                badgeView.setText(ExpirationUtil.getExpirationAbbreviatedDisplayValue(this, recipient.getExpireMessages()));
                actionView.setOnClickListener(v -> onOptionsItemSelected(item));
            } else {
                inflater.inflate(R.menu.conversation_expiring_off, menu);
            }
        }

        if (isSingleConversation()) {
            if (isSecureText) inflater.inflate(R.menu.conversation_callable_secure, menu);
            else inflater.inflate(R.menu.conversation_callable_insecure, menu);
        } else if (isGroupConversation()) {
            inflater.inflate(R.menu.conversation_group_options, menu);

            if (!isPushGroupConversation()) {
                inflater.inflate(R.menu.conversation_mms_group_options, menu);
                if (distributionType == ThreadDatabase.DistributionTypes.BROADCAST) {
                    menu.findItem(R.id.menu_distribution_broadcast).setChecked(true);
                } else {
                    menu.findItem(R.id.menu_distribution_conversation).setChecked(true);
                }
            } else if (isActiveGroup()) {
                inflater.inflate(R.menu.conversation_push_group_options, menu);
            }
        }

        inflater.inflate(R.menu.conversation, menu);

        if (isSingleConversation() && isSecureText) {
            inflater.inflate(R.menu.conversation_secure, menu);
        } else if (isSingleConversation()) {
            inflater.inflate(R.menu.conversation_insecure, menu);
        }

        if (recipient != null && recipient.isMuted())
            inflater.inflate(R.menu.conversation_muted, menu);
        else inflater.inflate(R.menu.conversation_unmuted, menu);

        if (isSingleConversation() && getRecipient().getContactUri() == null) {
            inflater.inflate(R.menu.conversation_add_to_contacts, menu);
        }

        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_call_secure:
                handleDial(getRecipient(), true);
                return true;
            case R.id.menu_call_insecure:
                handleDial(getRecipient(), false);
                return true;
            case R.id.menu_view_media:
                handleViewMedia();
                return true;
            case R.id.menu_add_shortcut:
                handleAddShortcut();
                return true;
            case R.id.menu_add_to_contacts:
                handleAddToContacts();
                return true;
            case R.id.menu_reset_secure_session:
                handleResetSecureSession();
                return true;
            case R.id.menu_group_recipients:
                handleDisplayGroupRecipients();
                return true;
            case R.id.menu_distribution_broadcast:
                handleDistributionBroadcastEnabled(item);
                return true;
            case R.id.menu_distribution_conversation:
                handleDistributionConversationEnabled(item);
                return true;
            case R.id.menu_edit_group:
                handleEditPushGroup();
                return true;
            case R.id.menu_leave:
                handleLeavePushGroup();
                return true;
            case R.id.menu_invite:
                handleInviteLink();
                return true;
            case R.id.menu_mute_notifications:
                handleMuteNotifications();
                return true;
            case R.id.menu_unmute_notifications:
                handleUnmuteNotifications();
                return true;
            case R.id.menu_conversation_settings:
                handleConversationSettings();
                return true;
            case R.id.menu_expiring_messages_off:
            case R.id.menu_expiring_messages:
                handleSelectMessageExpiration();
                return true;
            case android.R.id.home:
                handleReturnToConversationList();
                return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        if (container.isInputOpen()) container.hideCurrentInput(composeText);
        else super.onBackPressed();
    }

    @Override
    public void onKeyboardShown() {
        inputPanel.onKeyboardShown();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ReminderUpdateEvent event) {
        updateReminders(recipient.hasSeenInviteReminder());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    //////// Event Handlers

    private void handleReturnToConversationList() {
//        TODO: +++++++++
//        Intent intent = new Intent(this, (archived ? ConversationListArchiveActivity.class : ConversationListActivity.class));
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
//        finish();
    }

    private void handleSelectMessageExpiration() {
        if (isPushGroupConversation() && !isActiveGroup()) {
            return;
        }

        //noinspection CodeBlock2Expr
        ExpirationDialog.show(this, recipient.getExpireMessages(), expirationTime -> {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    DatabaseFactory.getRecipientDatabase(ConversationActivity.this).setExpireMessages(recipient, expirationTime);
                    OutgoingExpirationUpdateMessage outgoingMessage = new OutgoingExpirationUpdateMessage(getRecipient(), System.currentTimeMillis(), expirationTime * 1000L);
                    MessageSender.send(ConversationActivity.this, outgoingMessage, threadId, false, null);

                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    invalidateOptionsMenu();
                    if (fragment != null) fragment.setLastSeen(0);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
    }

    private void handleMuteNotifications() {
        MuteDialog.show(this, until -> {
            recipient.setMuted(until);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    DatabaseFactory.getRecipientDatabase(ConversationActivity.this)
                            .setMuted(recipient, until);

                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        });
    }

    private void handleConversationSettings() {
        Intent intent = new Intent(ConversationActivity.this, RecipientPreferenceActivity.class);
        intent.putExtra(RecipientPreferenceActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(RecipientPreferenceActivity.CAN_HAVE_SAFETY_NUMBER_EXTRA,
                isSecureText && !isSelfConversation());

        startActivitySceneTransition(intent, titleView.findViewById(R.id.contact_photo_image), "avatar");
    }

    private void handleUnmuteNotifications() {
        recipient.setMuted(0);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DatabaseFactory.getRecipientDatabase(ConversationActivity.this)
                        .setMuted(recipient, 0);

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleUnblock() {
        int titleRes = R.string.ConversationActivity_unblock_this_contact_question;
        int bodyRes = R.string.ConversationActivity_you_will_once_again_be_able_to_receive_messages_and_calls_from_this_contact;

        if (recipient.isGroupRecipient()) {
            titleRes = R.string.ConversationActivity_unblock_this_group_question;
            bodyRes = R.string.ConversationActivity_unblock_this_group_description;
        }

        //noinspection CodeBlock2Expr
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(bodyRes)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ConversationActivity_unblock, (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            DatabaseFactory.getRecipientDatabase(ConversationActivity.this)
                                    .setBlocked(recipient, false);

                            MuzimaApplication.getInstance(ConversationActivity.this)
                                    .getJobManager()
                                    .add(new MultiDeviceBlockedUpdateJob(ConversationActivity.this));

                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }).show();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void handleMakeDefaultSms() {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, SMS_DEFAULT);
    }

    private void handleRegisterForSignal() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        intent.putExtra(RegistrationActivity.RE_REGISTRATION_EXTRA, true);
        startActivity(intent);
    }

    private void handleInviteLink() {
        try {
            String inviteText;

            boolean a = SecureRandom.getInstance("SHA1PRNG").nextBoolean();
            if (a)
                inviteText = getString(R.string.ConversationActivity_lets_switch_to_muzima, "https://play.google.com/store/apps/details?id=com.muzima");
            else
                inviteText = getString(R.string.ConversationActivity_lets_use_this_to_chat, "https://play.google.com/store/apps/details?id=com.muzima");

            if (isDefaultSms) {
                composeText.appendInvite(inviteText);
            } else {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("smsto:" + recipient.getAddress().serialize()));
                intent.putExtra("sms_body", inviteText);
                intent.putExtra(Intent.EXTRA_TEXT, inviteText);
                startActivity(intent);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private void handleResetSecureSession() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ConversationActivity_reset_secure_session_question);
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setCancelable(true);
        builder.setMessage(R.string.ConversationActivity_this_may_help_if_youre_having_encryption_problems);
        builder.setPositiveButton(R.string.ConversationActivity_reset, (dialog, which) -> {
            if (isSingleConversation()) {
                final Context context = getApplicationContext();

                OutgoingEndSessionMessage endSessionMessage =
                        new OutgoingEndSessionMessage(new OutgoingTextMessage(getRecipient(), "TERMINATE", 0, -1));

                new AsyncTask<OutgoingEndSessionMessage, Void, Long>() {
                    @Override
                    protected Long doInBackground(OutgoingEndSessionMessage... messages) {
                        return MessageSender.send(context, messages[0], threadId, false, null);
                    }

                    @Override
                    protected void onPostExecute(Long result) {
                        sendComplete(result);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, endSessionMessage);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void handleViewMedia() {
        Intent intent = new Intent(this, MediaOverviewActivity.class);
        intent.putExtra(MediaOverviewActivity.ADDRESS_EXTRA, recipient.getAddress());
        startActivity(intent);
    }

    private void handleAddShortcut() {
//        Todo: +++++++handle handleAddShortcut
//        Log.i(TAG, "Creating home screen shortcut for recipient " + recipient.getAddress());
//
//        new AsyncTask<Void, Void, IconCompat>() {
//
//            @Override
//            protected IconCompat doInBackground(Void... voids) {
//                Context context = getMuzimaApplication();
//                IconCompat icon = null;
//
//                if (recipient.getContactPhoto() != null) {
//                    try {
//                        Bitmap bitmap = BitmapFactory.decodeStream(recipient.getContactPhoto().openInputStream(context));
//                        bitmap = BitmapUtil.createScaledBitmap(bitmap, 300, 300);
//                        icon = IconCompat.createWithAdaptiveBitmap(bitmap);
//                    } catch (IOException e) {
//                        Log.w(TAG, "Failed to decode contact photo during shortcut creation. Falling back to generic icon.", e);
//                    }
//                }
//
//                if (icon == null) {
//                    icon = IconCompat.createWithResource(context, recipient.isGroupRecipient() ? R.mipmap.ic_group_shortcut
//                            : R.mipmap.ic_person_shortcut);
//                }
//
//                return icon;
//            }
//
//            @Override
//            protected void onPostExecute(IconCompat icon) {
//                Context context = getMuzimaApplication();
//                String name = Optional.fromNullable(recipient.getName())
//                        .or(Optional.fromNullable(recipient.getProfileName()))
//                        .or(recipient.toShortString());
//
//                ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(context, recipient.getAddress().serialize() + '-' + System.currentTimeMillis())
//                        .setShortLabel(name)
//                        .setIcon(icon)
//                        .setIntent(ShortcutLauncherActivity.createIntent(context, recipient.getAddress()))
//                        .build();
//
//                if (ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)) {
//                    Toast.makeText(context, getString(R.string.ConversationActivity_added_to_home_screen), Toast.LENGTH_LONG).show();
//                }
//            }
//        }.execute();
    }

    private void handleLeavePushGroup() {
//        if (getRecipient() == null) {
//            Toast.makeText(this, getString(R.string.ConversationActivity_invalid_recipient),
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle(getString(R.string.ConversationActivity_leave_group));
//        builder.setIconAttribute(R.attr.dialog_info_icon);
//        builder.setCancelable(true);
//        builder.setMessage(getString(R.string.ConversationActivity_are_you_sure_you_want_to_leave_this_group));
//        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
//            SignalRecipient groupRecipient = getRecipient();
//            long threadId = DatabaseFactory.getThreadDatabase(this).getThreadIdFor(groupRecipient);
//            Optional<OutgoingGroupMediaMessage> leaveMessage = GroupUtil.createGroupLeaveMessage(this, groupRecipient);
//
//            if (threadId != -1 && leaveMessage.isPresent()) {
//                MessageSender.send(this, leaveMessage.get(), threadId, false, null);
//
//                GroupDatabase groupDatabase = DatabaseFactory.getGroupDatabase(this);
//                String groupId = groupRecipient.getAddress().toGroupString();
//                groupDatabase.setActive(groupId, false);
//                groupDatabase.remove(groupId, SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(this)));
//
//                initializeEnabledCheck();
//            } else {
//                Toast.makeText(this, R.string.ConversationActivity_error_leaving_group, Toast.LENGTH_LONG).show();
//            }
//        });
//
//        builder.setNegativeButton(R.string.general_no, null);
//        builder.show();
    }

    private void handleEditPushGroup() {
//        Intent intent = new Intent(ConversationActivity.this, GroupCreateActivity.class);
//        intent.putExtra(GroupCreateActivity.GROUP_ADDRESS_EXTRA, recipient.getAddress());
//        startActivityForResult(intent, GROUP_EDIT);
    }

    private void handleDistributionBroadcastEnabled(MenuItem item) {
        distributionType = ThreadDatabase.DistributionTypes.BROADCAST;
        item.setChecked(true);

        if (threadId != -1) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    DatabaseFactory.getThreadDatabase(ConversationActivity.this)
                            .setDistributionType(threadId, ThreadDatabase.DistributionTypes.BROADCAST);
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void handleDistributionConversationEnabled(MenuItem item) {
        distributionType = ThreadDatabase.DistributionTypes.CONVERSATION;
        item.setChecked(true);

        if (threadId != -1) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    DatabaseFactory.getThreadDatabase(ConversationActivity.this)
                            .setDistributionType(threadId, ThreadDatabase.DistributionTypes.CONVERSATION);
                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void handleDial(final SignalRecipient recipient, boolean isSecure) {
        if (recipient == null) return;

        if (isSecure) {
            CommunicationActions.startVoiceCall(this, recipient);
        } else {
            try {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:" + recipient.getAddress().serialize()));
                startActivity(dialIntent);
            } catch (ActivityNotFoundException anfe) {
                Log.w(TAG, anfe);
                Dialogs.showAlertDialog(this,
                        getString(R.string.ConversationActivity_calls_not_supported),
                        getString(R.string.ConversationActivity_this_device_does_not_appear_to_support_dial_actions));
            }
        }
    }

    private void handleDisplayGroupRecipients() {
//        new GroupMembersDialog(this, getRecipient()).display();
    }

    private void handleAddToContacts() {
        if (recipient.getAddress().isGroup()) return;

        try {
            final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
            if (recipient.getAddress().isEmail()) {
                intent.putExtra(ContactsContract.Intents.Insert.EMAIL, recipient.getAddress().toEmailString());
            } else {
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, recipient.getAddress().toPhoneString());
            }
            intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
            startActivityForResult(intent, ADD_CONTACT);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, e);
        }
    }

    private boolean handleDisplayQuickContact() {
        if (recipient.getAddress().isGroup()) return false;

        if (recipient.getContactUri() != null) {
            ContactsContract.QuickContact.showQuickContact(ConversationActivity.this, titleView, recipient.getContactUri(), ContactsContract.QuickContact.MODE_LARGE, null);
        } else {
            handleAddToContacts();
        }

        return true;
    }

    private void handleAddAttachment() {
        if (this.isMmsEnabled || isSecureText) {
            if (attachmentTypeSelector == null) {
                attachmentTypeSelector = new AttachmentTypeSelector(this, getSupportLoaderManager(), new AttachmentTypeListener());
            }
            attachmentTypeSelector.show(this, attachButton);
        } else {
            handleManualMmsRequired();
        }
    }

    private void handleManualMmsRequired() {
        Toast.makeText(this, R.string.MmsDownloader_error_reading_mms_settings, Toast.LENGTH_LONG).show();

        Bundle extras = getIntent().getExtras();
        Intent intent = new Intent(this, PromptMmsActivity.class);
        if (extras != null) intent.putExtras(extras);
        startActivity(intent);
    }

    private void handleUnverifiedRecipients() {
        List<SignalRecipient> unverifiedRecipients = identityRecords.getUnverifiedRecipients(this);
        List<IdentityRecord> unverifiedRecords = identityRecords.getUnverifiedRecords();
        String message = IdentityUtil.getUnverifiedSendDialogDescription(this, unverifiedRecipients);

        if (message == null) return;

        //noinspection CodeBlock2Expr
        new UnverifiedSendDialog(this, message, unverifiedRecords, () -> {
            initializeIdentityRecords().addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    sendMessage();
                }

                @Override
                public void onFailure(ExecutionException e) {
                    throw new AssertionError(e);
                }
            });
        }).show();
    }

    private void handleUntrustedRecipients() {
        List<SignalRecipient> untrustedRecipients = identityRecords.getUntrustedRecipients(this);
        List<IdentityRecord> untrustedRecords = identityRecords.getUntrustedRecords();
        String untrustedMessage = IdentityUtil.getUntrustedSendDialogDescription(this, untrustedRecipients);

        if (untrustedMessage == null) return;

        //noinspection CodeBlock2Expr
        new UntrustedSendDialog(this, untrustedMessage, untrustedRecords, () -> {
            initializeIdentityRecords().addListener(new ListenableFuture.Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    sendMessage();
                }

                @Override
                public void onFailure(ExecutionException e) {
                    throw new AssertionError(e);
                }
            });
        }).show();
    }

    private void handleSecurityChange(boolean isSecureText, boolean isDefaultSms) {
        Log.i(TAG, "handleSecurityChange(" + isSecureText + ", " + isDefaultSms + ")");
        if (isSecurityInitialized && isSecureText == this.isSecureText && isDefaultSms == this.isDefaultSms) {
            return;
        }

        this.isSecureText = isSecureText;
        this.isDefaultSms = isDefaultSms;
        this.isSecurityInitialized = true;

        boolean isMediaMessage = recipient.isMmsGroupRecipient() || attachmentManager.isAttachmentPresent();

        sendButton.resetAvailableTransports(isMediaMessage);

        if (!isSecureText && !isPushGroupConversation())
            sendButton.disableTransport(Type.TEXTSECURE);
        if (recipient.isPushGroupRecipient()) sendButton.disableTransport(Type.SMS);

        if (isSecureText || isPushGroupConversation())
            sendButton.setDefaultTransport(Type.TEXTSECURE);
        else sendButton.setDefaultTransport(Type.SMS);

        calculateCharactersRemaining();
        supportInvalidateOptionsMenu();
        setBlockedUserState(recipient, isSecureText, isDefaultSms);
    }

    ///// Initializers

    private ListenableFuture<Boolean> initializeDraft() {
        final SettableFuture<Boolean> result = new SettableFuture<>();

        final String draftText = getIntent().getStringExtra(TEXT_EXTRA);
        final Uri draftMedia = getIntent().getData();
        final MediaType draftMediaType = MediaType.from(getIntent().getType());

        if (draftText != null) {
            composeText.setText(draftText);
            result.set(true);
        }

        if (draftMedia != null && draftMediaType != null) {
            return setMedia(draftMedia, draftMediaType);
        }

        if (draftText == null && draftMedia == null && draftMediaType == null) {
            return initializeDraftFromDatabase();
        } else {
            updateToggleButtonState();
            result.set(false);
        }

        return result;
    }

    private void initializeEnabledCheck() {
        boolean enabled = !(isPushGroupConversation() && !isActiveGroup());
        inputPanel.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        attachButton.setEnabled(enabled);
    }

    private ListenableFuture<Boolean> initializeDraftFromDatabase() {
        SettableFuture<Boolean> future = new SettableFuture<>();

        new AsyncTask<Void, Void, List<Draft>>() {
            @Override
            protected List<Draft> doInBackground(Void... params) {
                DraftDatabase draftDatabase = DatabaseFactory.getDraftDatabase(ConversationActivity.this);
                List<Draft> results = draftDatabase.getDrafts(threadId);

                draftDatabase.clearDrafts(threadId);

                return results;
            }

            @Override
            protected void onPostExecute(List<Draft> drafts) {
                if (drafts.isEmpty()) {
                    future.set(false);
                    updateToggleButtonState();
                    return;
                }

                AtomicInteger draftsRemaining = new AtomicInteger(drafts.size());
                AtomicBoolean success = new AtomicBoolean(false);
                ListenableFuture.Listener<Boolean> listener = new AssertedSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        success.compareAndSet(false, result);

                        if (draftsRemaining.decrementAndGet() <= 0) {
                            future.set(success.get());
                        }
                    }
                };

                for (Draft draft : drafts) {
                    try {
                        switch (draft.getType()) {
                            case Draft.TEXT:
                                composeText.setText(draft.getValue());
                                listener.onSuccess(true);
                                break;
                            case Draft.LOCATION:
                                attachmentManager.setLocation(SignalPlace.deserialize(draft.getValue()), getCurrentMediaConstraints()).addListener(listener);
                                break;
                            case Draft.IMAGE:
                                setMedia(Uri.parse(draft.getValue()), MediaType.IMAGE).addListener(listener);
                                break;
                            case Draft.AUDIO:
                                setMedia(Uri.parse(draft.getValue()), MediaType.AUDIO).addListener(listener);
                                break;
                            case Draft.VIDEO:
                                setMedia(Uri.parse(draft.getValue()), MediaType.VIDEO).addListener(listener);
                                break;
                            case Draft.QUOTE:
                                SettableFuture<Boolean> quoteResult = new SettableFuture<>();
                                new QuoteRestorationTask(draft.getValue(), quoteResult).execute();
                                quoteResult.addListener(listener);
                                break;
                        }
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }

                updateToggleButtonState();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return future;
    }

    private ListenableFuture<Boolean> initializeSecurity(final boolean currentSecureText,
                                                         final boolean currentIsDefaultSms) {
        final SettableFuture<Boolean> future = new SettableFuture<>();

        handleSecurityChange(currentSecureText || isPushGroupConversation(), currentIsDefaultSms);

        new AsyncTask<SignalRecipient, Void, boolean[]>() {
            @Override
            protected boolean[] doInBackground(SignalRecipient... params) {
                Context context = ConversationActivity.this;
                SignalRecipient recipient = params[0];
                Log.i(TAG, "Resolving registered state...");
                RegisteredState registeredState;

                if (recipient.isPushGroupRecipient()) {
                    Log.i(TAG, "Push group recipient...");
                    registeredState = RegisteredState.REGISTERED;
                } else if (recipient.isResolving()) {
                    Log.i(TAG, "Talking to DB directly.");
                    registeredState = DatabaseFactory.getRecipientDatabase(ConversationActivity.this).isRegistered(recipient.getAddress());
                } else {
                    Log.i(TAG, "Checking through resolved recipient");
                    registeredState = recipient.resolve().getRegistered();
                }

                Log.i(TAG, "Resolved registered state: " + registeredState);
                boolean signalEnabled = TextSecurePreferences.isPushRegistered(context);

                if (registeredState == RegisteredState.UNKNOWN) {
                    try {
                        Log.i(TAG, "Refreshing directory for user: " + recipient.getAddress().serialize());
                        registeredState = DirectoryHelper.refreshDirectoryFor(context, recipient);
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }

                Log.i(TAG, "Returning registered state...");
                return new boolean[]{registeredState == RegisteredState.REGISTERED && signalEnabled,
                        Util.isDefaultSmsProvider(context)};
            }

            @Override
            protected void onPostExecute(boolean[] result) {
                if (result[0] != currentSecureText || result[1] != currentIsDefaultSms) {
                    Log.i(TAG, "onPostExecute() handleSecurityChange: " + result[0] + " , " + result[1]);
                    handleSecurityChange(result[0], result[1]);
                }
                future.set(true);
                onSecurityUpdated();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient);

        return future;
    }

    private void onSecurityUpdated() {
        Log.i(TAG, "onSecurityUpdated()");
        updateReminders(recipient.hasSeenInviteReminder());
        updateDefaultSubscriptionId(recipient.getDefaultSubscriptionId());
    }

    protected void updateReminders(boolean seenInvite) {
        Log.i(TAG, "updateReminders(" + seenInvite + ")");

//        if (UnauthorizedReminder.isEligible(this)) {
//            reminderView.get().showReminder(new UnauthorizedReminder(this));
//        } else if (ExpiredBuildReminder.isEligible()) {
//            reminderView.get().showReminder(new ExpiredBuildReminder(this));
//        } else if (ServiceOutageReminder.isEligible(this)) {
//            MuzimaApplication.getInstance(this).getJobManager().add(new ServiceOutageDetectionJob(this));
//            reminderView.get().showReminder(new ServiceOutageReminder(this));
//        } else if (TextSecurePreferences.isPushRegistered(this) &&
//                TextSecurePreferences.isShowInviteReminders(this) &&
//                !isSecureText &&
//                !seenInvite &&
//                !recipient.isGroupRecipient()) {
//            InviteReminder reminder = new InviteReminder(this, recipient);
//            reminder.setOkListener(v -> {
//                handleInviteLink();
//                reminderView.get().requestDismiss();
//            });
//            reminderView.get().showReminder(reminder);
//        } else if (reminderView.resolved()) {
//            reminderView.get().hide();
//        }
    }

    private void updateDefaultSubscriptionId(Optional<Integer> defaultSubscriptionId) {
        Log.i(TAG, "updateDefaultSubscriptionId(" + defaultSubscriptionId.orNull() + ")");
        sendButton.setDefaultSubscriptionId(defaultSubscriptionId);
    }

    private void initializeMmsEnabledCheck() {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return Util.isMmsCapable(ConversationActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean isMmsEnabled) {
                ConversationActivity.this.isMmsEnabled = isMmsEnabled;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private ListenableFuture<Boolean> initializeIdentityRecords() {
        final SettableFuture<Boolean> future = new SettableFuture<>();

        new AsyncTask<SignalRecipient, Void, Pair<IdentityRecordList, String>>() {
            @Override
            protected @NonNull
            Pair<IdentityRecordList, String> doInBackground(SignalRecipient... params) {
                IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(ConversationActivity.this);
                IdentityRecordList identityRecordList = new IdentityRecordList();
                List<SignalRecipient> recipients = new LinkedList<>();

                if (params[0].isGroupRecipient()) {
                    recipients.addAll(DatabaseFactory.getGroupDatabase(ConversationActivity.this)
                            .getGroupMembers(params[0].getAddress().toGroupString(), false));
                } else {
                    recipients.add(params[0]);
                }

                for (SignalRecipient recipient : recipients) {
                    Log.i(TAG, "Loading identity for: " + recipient.getAddress());
                    identityRecordList.add(identityDatabase.getIdentity(recipient.getAddress()));
                }

                String message = null;

                if (identityRecordList.isUnverified()) {
                    message = IdentityUtil.getUnverifiedBannerDescription(ConversationActivity.this, identityRecordList.getUnverifiedRecipients(ConversationActivity.this));
                }

                return new Pair<>(identityRecordList, message);
            }

            @Override
            protected void onPostExecute(@NonNull Pair<IdentityRecordList, String> result) {
                Log.i(TAG, "Got identity records: " + result.first.isUnverified());
                identityRecords.replaceWith(result.first);

                if (result.second != null) {
                    Log.d(TAG, "Replacing banner...");
                    unverifiedBannerView.get().display(result.second, result.first.getUnverifiedRecords(),
                            new UnverifiedClickedListener(),
                            new UnverifiedDismissedListener());
                } else if (unverifiedBannerView.resolved()) {
                    Log.d(TAG, "Clearing banner...");
                    unverifiedBannerView.get().hide();
                }

                titleView.setVerified(isSecureText && identityRecords.isVerified());

                future.set(true);
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient);

        return future;
    }

    private void initializeViews() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        titleView = (ConversationTitleView) supportActionBar.getCustomView();
        buttonToggle = ViewUtil.findById(this, R.id.button_toggle);
        sendButton = ViewUtil.findById(this, R.id.send_button);
        attachButton = ViewUtil.findById(this, R.id.attach_button);
        composeText = ViewUtil.findById(this, R.id.embedded_text_editor);
        charactersLeft = ViewUtil.findById(this, R.id.space_left);
        emojiDrawerStub = ViewUtil.findStubById(this, R.id.emoji_drawer_stub);
        unblockButton = ViewUtil.findById(this, R.id.unblock_button);
        makeDefaultSmsButton = ViewUtil.findById(this, R.id.make_default_sms_button);
        registerButton = ViewUtil.findById(this, R.id.register_button);
        composePanel = ViewUtil.findById(this, R.id.bottom_panel);
        container = ViewUtil.findById(this, R.id.layout_container);
        reminderView = ViewUtil.findStubById(this, R.id.reminder_stub);
        unverifiedBannerView = ViewUtil.findStubById(this, R.id.unverified_banner_stub);
        groupShareProfileView = ViewUtil.findStubById(this, R.id.group_share_profile_view_stub);
        quickAttachmentDrawer = ViewUtil.findById(this, R.id.quick_attachment_drawer);
        quickAttachmentToggle = ViewUtil.findById(this, R.id.quick_attachment_toggle);
        inlineAttachmentToggle = ViewUtil.findById(this, R.id.inline_attachment_container);
        inputPanel = ViewUtil.findById(this, R.id.bottom_panel);

        ImageButton quickCameraToggle = ViewUtil.findById(this, R.id.quick_camera_toggle);
        ImageButton inlineAttachmentButton = ViewUtil.findById(this, R.id.inline_attachment_button);

        container.addOnKeyboardShownListener(this);
        inputPanel.setListener(this);
        inputPanel.setMediaListener(this);

        attachmentTypeSelector = null;
        attachmentManager = new AttachmentManager(this, this);
        audioRecorder = new AudioRecorder(this);
        typingTextWatcher = new TypingStatusTextWatcher();

        SendButtonListener sendButtonListener = new SendButtonListener();
        ComposeKeyPressedListener composeKeyPressedListener = new ComposeKeyPressedListener();

        composeText.setOnEditorActionListener(sendButtonListener);
        attachButton.setOnClickListener(new AttachButtonListener());
        attachButton.setOnLongClickListener(new AttachButtonLongClickListener());
        sendButton.setOnClickListener(sendButtonListener);
        sendButton.setEnabled(true);
        sendButton.addOnTransportChangedListener((newTransport, manuallySelected) -> {
            calculateCharactersRemaining();
            composeText.setTransport(newTransport);
            buttonToggle.getBackground().setColorFilter(newTransport.getBackgroundColor(), PorterDuff.Mode.MULTIPLY);
            buttonToggle.getBackground().invalidateSelf();
            if (manuallySelected)
                recordSubscriptionIdPreference(newTransport.getSimSubscriptionId());
        });

        titleView.setOnClickListener(v -> handleConversationSettings());
        titleView.setOnLongClickListener(v -> handleDisplayQuickContact());
        titleView.setOnBackClickedListener(view -> super.onBackPressed());
        unblockButton.setOnClickListener(v -> handleUnblock());
        makeDefaultSmsButton.setOnClickListener(v -> handleMakeDefaultSms());
        registerButton.setOnClickListener(v -> handleRegisterForSignal());

        composeText.setOnKeyListener(composeKeyPressedListener);
        composeText.addTextChangedListener(composeKeyPressedListener);
        composeText.setOnEditorActionListener(sendButtonListener);
        composeText.setOnClickListener(composeKeyPressedListener);
        composeText.setOnFocusChangeListener(composeKeyPressedListener);

        if (QuickAttachmentDrawer.isDeviceSupported(this)) {
            quickAttachmentDrawer.setListener(this);
            quickCameraToggle.setOnClickListener(new QuickCameraToggleListener());
        } else {
            quickCameraToggle.setVisibility(View.GONE);
            quickCameraToggle.setEnabled(false);
        }

        inlineAttachmentButton.setOnClickListener(v -> handleAddAttachment());
    }

    protected void initializeActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        supportActionBar.setDisplayHomeAsUpEnabled(false);
        supportActionBar.setCustomView(R.layout.conversation_title_view);
        supportActionBar.setDisplayShowCustomEnabled(true);
        supportActionBar.setDisplayShowTitleEnabled(false);
    }

    private void initializeResources() {
        if (recipient != null) recipient.removeListener(this);

        recipient = SignalRecipient.from(this, getIntent().getParcelableExtra(ADDRESS_EXTRA), true);
        threadId = getIntent().getLongExtra(THREAD_ID_EXTRA, -1);
        archived = getIntent().getBooleanExtra(IS_ARCHIVED_EXTRA, false);
        distributionType = getIntent().getIntExtra(DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        glideRequests = GlideApp.with(this);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            LinearLayout conversationContainer = ViewUtil.findById(this, R.id.conversation_container);
            conversationContainer.setClipChildren(true);
            conversationContainer.setClipToPadding(true);
        }

        recipient.addListener(this);
    }

    private void initializeProfiles() {
        if (!isSecureText) {
            Log.i(TAG, "SMS contact, no profile fetch needed.");
            return;
        }

//        MuzimaApplication.getInstance(this)
//                .getJobManager()
//                .add(new RetrieveProfileJob(this, recipient));
    }

    @Override
    public void onModified(final SignalRecipient recipient) {
        Log.i(TAG, "onModified(" + recipient.getAddress().serialize() + ")");
        Util.runOnMain(() -> {
            Log.i(TAG, "onModifiedRun(): " + recipient.getRegistered());
            titleView.setTitle(glideRequests, recipient);
            titleView.setVerified(identityRecords.isVerified());
            setBlockedUserState(recipient, isSecureText, isDefaultSms);
            setActionBarColor(recipient.getColor());
            setGroupShareProfileReminder(recipient);
            updateReminders(recipient.hasSeenInviteReminder());
            updateDefaultSubscriptionId(recipient.getDefaultSubscriptionId());
            initializeSecurity(isSecureText, isDefaultSms);
            invalidateOptionsMenu();
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onIdentityRecordUpdate(final IdentityRecord event) {
        initializeIdentityRecords();
    }

    private void initializeReceivers() {
        securityUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                initializeSecurity(isSecureText, isDefaultSms);
                calculateCharactersRemaining();
            }
        };

        registerReceiver(securityUpdateReceiver,
                new IntentFilter(SecurityEvent.SECURITY_UPDATE_EVENT),
                KeyCachingService.KEY_PERMISSION, null);
    }

    //////// Helper Methods

    private void addAttachment(int type) {
        Log.i(TAG, "Selected: " + type);
        switch (type) {
            case AttachmentTypeSelector.ADD_GALLERY:
                AttachmentManager.selectGallery(this, PICK_GALLERY);
                break;
            case AttachmentTypeSelector.ADD_DOCUMENT:
                AttachmentManager.selectDocument(this, PICK_DOCUMENT);
                break;
            case AttachmentTypeSelector.ADD_SOUND:
                AttachmentManager.selectAudio(this, PICK_AUDIO);
                break;
            case AttachmentTypeSelector.ADD_CONTACT_INFO:
                AttachmentManager.selectContactInfo(this, PICK_CONTACT);
                break;
            case AttachmentTypeSelector.ADD_LOCATION:
                AttachmentManager.selectLocation(this, PICK_LOCATION);
                break;
            case AttachmentTypeSelector.TAKE_PHOTO:
                attachmentManager.capturePhoto(this, TAKE_PHOTO);
                break;
        }
    }

    private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType) {
        return setMedia(uri, mediaType, 0, 0);
    }

    private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType, int width, int height) {
        if (uri == null) {
            return new SettableFuture<>(false);
        }

        if (MediaType.VCARD.equals(mediaType) && isSecureText) {
            openContactShareEditor(uri);
            return new SettableFuture<>(false);
        } else {
            return attachmentManager.setMedia(glideRequests, uri, mediaType, getCurrentMediaConstraints(), width, height);
        }
    }

    private void openContactShareEditor(Uri contactUri) {
        Intent intent = ContactShareEditActivity.getIntent(this, Collections.singletonList(contactUri));
        startActivityForResult(intent, GET_CONTACT_DETAILS);
    }

    private void addAttachmentContactInfo(Uri contactUri) {
        ContactAccessor contactDataList = ContactAccessor.getInstance();
        ContactData contactData = contactDataList.getContactData(this, contactUri);

        if (contactData.numbers.size() == 1) composeText.append(contactData.numbers.get(0).number);
        else if (contactData.numbers.size() > 1) selectContactInfo(contactData);
    }

    private void sendSharedContact(List<Contact> contacts) {
        int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
        long expiresIn = recipient.getExpireMessages() * 1000L;
        boolean initiating = threadId == -1;

        sendMediaMessage(isSmsForced(), "", attachmentManager.buildSlideDeck(), contacts, expiresIn, subscriptionId, initiating);
    }

    private void selectContactInfo(ContactData contactData) {
        final CharSequence[] numbers = new CharSequence[contactData.numbers.size()];
        final CharSequence[] numberItems = new CharSequence[contactData.numbers.size()];

        for (int i = 0; i < contactData.numbers.size(); i++) {
            numbers[i] = contactData.numbers.get(i).number;
            numberItems[i] = contactData.numbers.get(i).type + ": " + contactData.numbers.get(i).number;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIconAttribute(R.attr.conversation_attach_contact_info);
        builder.setTitle(R.string.ConversationActivity_select_contact_info);

        builder.setItems(numberItems, (dialog, which) -> composeText.append(numbers[which]));
        builder.show();
    }

    private Drafts getDraftsForCurrentState() {
        Drafts drafts = new Drafts();

        if (!Util.isEmpty(composeText)) {
            drafts.add(new Draft(Draft.TEXT, composeText.getTextTrimmed()));
        }

        for (Slide slide : attachmentManager.buildSlideDeck().getSlides()) {
            if (slide.hasAudio() && slide.getUri() != null)
                drafts.add(new Draft(Draft.AUDIO, slide.getUri().toString()));
            else if (slide.hasVideo() && slide.getUri() != null)
                drafts.add(new Draft(Draft.VIDEO, slide.getUri().toString()));
            else if (slide.hasLocation())

                drafts.add(new Draft(Draft.LOCATION, ((LocationSlide) slide).getPlace().serialize()));
            else if (slide.hasImage() && slide.getUri() != null)
                drafts.add(new Draft(Draft.IMAGE, slide.getUri().toString()));
        }

        Optional<QuoteModel> quote = inputPanel.getQuote();

        if (quote.isPresent()) {
            drafts.add(new Draft(Draft.QUOTE, new QuoteId(quote.get().getId(), quote.get().getAuthor()).serialize()));
        }

        return drafts;
    }

    protected ListenableFuture<Long> saveDraft() {
        final SettableFuture<Long> future = new SettableFuture<>();

        if (this.recipient == null) {
            future.set(threadId);
            return future;
        }

        final Drafts drafts = getDraftsForCurrentState();
        final long thisThreadId = this.threadId;
        final int thisDistributionType = this.distributionType;

        new AsyncTask<Long, Void, Long>() {
            @Override
            protected Long doInBackground(Long... params) {
                ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(ConversationActivity.this);
                DraftDatabase draftDatabase = DatabaseFactory.getDraftDatabase(ConversationActivity.this);
                long threadId = params[0];

                if (drafts.size() > 0) {
                    if (threadId == -1)
                        threadId = threadDatabase.getThreadIdFor(getRecipient(), thisDistributionType);

                    draftDatabase.insertDrafts(threadId, drafts);
                    threadDatabase.updateSnippet(threadId, drafts.getSnippet(ConversationActivity.this),
                            drafts.getUriSnippet(),
                            System.currentTimeMillis(), MmsSmsColumns.Types.BASE_DRAFT_TYPE, true);
                } else if (threadId > 0) {
                    threadDatabase.update(threadId, false);
                }

                return threadId;
            }

            @Override
            protected void onPostExecute(Long result) {
                future.set(result);
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, thisThreadId);

        return future;
    }

    private void setActionBarColor(MaterialColor color) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();
        supportActionBar.setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
        setStatusBarColor(color.toStatusBarColor(this));
    }

    private void setBlockedUserState(SignalRecipient recipient, boolean isSecureText, boolean isDefaultSms) {
        if (recipient.isBlocked()) {
            unblockButton.setVisibility(View.VISIBLE);
            composePanel.setVisibility(View.GONE);
            makeDefaultSmsButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
        } else if (!isSecureText && isPushGroupConversation()) {
            unblockButton.setVisibility(View.GONE);
            composePanel.setVisibility(View.GONE);
            makeDefaultSmsButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.VISIBLE);
        } else if (!isSecureText && !isDefaultSms) {
            unblockButton.setVisibility(View.GONE);
            composePanel.setVisibility(View.GONE);
            makeDefaultSmsButton.setVisibility(View.VISIBLE);
            registerButton.setVisibility(View.GONE);
        } else {
            composePanel.setVisibility(View.VISIBLE);
            unblockButton.setVisibility(View.GONE);
            makeDefaultSmsButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
        }
    }

    private void setGroupShareProfileReminder(@NonNull SignalRecipient recipient) {
        if (recipient.isPushGroupRecipient() && !recipient.isProfileSharing()) {
            groupShareProfileView.get().setRecipient(recipient);
            groupShareProfileView.get().setVisibility(View.VISIBLE);
        } else if (groupShareProfileView.resolved()) {
            groupShareProfileView.get().setVisibility(View.GONE);
        }
    }

    private void calculateCharactersRemaining() {
        String messageBody = composeText.getTextTrimmed();
        TransportOption transportOption = sendButton.getSelectedTransport();
        CharacterCalculator.CharacterState characterState = transportOption.calculateCharacters(messageBody);

        if (characterState.charactersRemaining <= 15 || characterState.messagesSpent > 1) {
            charactersLeft.setText(String.format("%d/%d (%d)",
                    characterState.charactersRemaining,
                    characterState.maxMessageSize,
                    characterState.messagesSpent));
            charactersLeft.setVisibility(View.VISIBLE);
        } else {
            charactersLeft.setVisibility(View.GONE);
        }
    }

    private boolean isSingleConversation() {
        return getRecipient() != null && !getRecipient().isGroupRecipient();
    }

    private boolean isActiveGroup() {
        if (!isGroupConversation()) return false;

        Optional<GroupDatabase.GroupRecord> record = DatabaseFactory.getGroupDatabase(this).getGroup(getRecipient().getAddress().toGroupString());
        return record.isPresent() && record.get().isActive();
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isSelfConversation() {
        if (!TextSecurePreferences.isPushRegistered(this)) return false;
        if (recipient.isGroupRecipient()) return false;

        return Util.isOwnNumber(this, recipient.getAddress());
    }

    private boolean isGroupConversation() {
        return getRecipient() != null && getRecipient().isGroupRecipient();
    }

    private boolean isPushGroupConversation() {
        return getRecipient() != null && getRecipient().isPushGroupRecipient();
    }

    private boolean isSmsForced() {
        return sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
    }

    protected SignalRecipient getRecipient() {
        return this.recipient;
    }

    protected long getThreadId() {
        return this.threadId;
    }

    private String getMessage() throws InvalidMessageException {
        String rawText = composeText.getTextTrimmed();

        if (rawText.length() < 1 && !attachmentManager.isAttachmentPresent())
            throw new InvalidMessageException(getString(R.string.ConversationActivity_message_is_empty_exclamation));

        return rawText;
    }

    private MediaConstraints getCurrentMediaConstraints() {
        return sendButton.getSelectedTransport().getType() == TransportOption.Type.TEXTSECURE
                ? MediaConstraints.getPushMediaConstraints()
                : MediaConstraints.getMmsMediaConstraints(sendButton.getSelectedTransport().getSimSubscriptionId().or(-1));
    }

    private void markThreadAsRead() {
        new AsyncTask<Long, Void, Void>() {
            @Override
            protected Void doInBackground(Long... params) {
                Context context = ConversationActivity.this;
                List<MessagingDatabase.MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(params[0], false);

                MessageNotifier.updateNotification(context);
                MarkReadReceiver.process(context, messageIds);

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
    }

    private void markLastSeen() {
        new AsyncTask<Long, Void, Void>() {
            @Override
            protected Void doInBackground(Long... params) {
                DatabaseFactory.getThreadDatabase(ConversationActivity.this).setLastSeen(params[0]);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
    }

    protected void sendComplete(long threadId) {
        boolean refreshFragment = (threadId != this.threadId);
        this.threadId = threadId;

        if (fragment == null || !fragment.isVisible() || isFinishing()) {
            return;
        }

        fragment.setLastSeen(0);

        if (refreshFragment) {
            fragment.reload(recipient, threadId);
            MessageNotifier.setVisibleThread(threadId);
        }

        fragment.scrollToBottom();
        attachmentManager.cleanup();
    }

    private void sendMessage() {
        try {
            SignalRecipient recipient = getRecipient();

            if (recipient == null) {
                throw new RecipientFormattingException("Badly formatted");
            }

            boolean forceSms = sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
            int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
            long expiresIn = recipient.getExpireMessages() * 1000L;
            boolean initiating = threadId == -1;

            Log.i(TAG, "isManual Selection: " + sendButton.isManualSelection());
            Log.i(TAG, "forceSms: " + forceSms);

            if ((recipient.isMmsGroupRecipient() || recipient.getAddress().isEmail()) && !isMmsEnabled) {
                handleManualMmsRequired();
            }
            else if (!forceSms && identityRecords.isUnverified()) {
                handleUnverifiedRecipients();
            } else if (!forceSms && identityRecords.isUntrusted()) {
                handleUntrustedRecipients();
            } else if (attachmentManager.isAttachmentPresent() || recipient.isGroupRecipient() || recipient.getAddress().isEmail() || inputPanel.getQuote().isPresent()) {
                sendMediaMessage(forceSms, expiresIn, subscriptionId, initiating);
            } else {
                sendTextMessage(forceSms, expiresIn, subscriptionId, initiating);
            }
        } catch (RecipientFormattingException ex) {
            Toast.makeText(ConversationActivity.this,
                    R.string.ConversationActivity_recipient_is_not_a_valid_sms_or_email_address_exclamation,
                    Toast.LENGTH_LONG).show();
            Log.w(TAG, ex);
        } catch (InvalidMessageException ex) {
            Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_message_is_empty_exclamation,
                    Toast.LENGTH_SHORT).show();
            Log.w(TAG, ex);
        }
    }

    private void sendMediaMessage(final boolean forceSms, final long expiresIn, final int subscriptionId, boolean initiating)
            throws InvalidMessageException {
        Log.i(TAG, "Sending media message...");
        sendMediaMessage(forceSms, getMessage(), attachmentManager.buildSlideDeck(), Collections.emptyList(), expiresIn, subscriptionId, initiating);
    }

    private ListenableFuture<Void> sendMediaMessage(final boolean forceSms, String body, SlideDeck slideDeck, List<Contact> contacts, final long expiresIn, final int subscriptionId, final boolean initiating) {
        OutgoingMediaMessage outgoingMessageCandidate = new OutgoingMediaMessage(recipient, slideDeck, body, System.currentTimeMillis(), subscriptionId, expiresIn, distributionType, inputPanel.getQuote().orNull(), contacts);

        final SettableFuture<Void> future = new SettableFuture<>();
        final Context context = getApplicationContext();

        final OutgoingMediaMessage outgoingMessage;

        if (isSecureText && !forceSms) {
            outgoingMessage = new OutgoingSecureMediaMessage(outgoingMessageCandidate);
//            MuzimaApplication.getInstance(context).getTypingStatusSender().onTypingStopped(threadId);
        } else {
            outgoingMessage = outgoingMessageCandidate;
        }

        Permissions.with(this)
                .request(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS)
                .ifNecessary(!isSecureText || forceSms)
                .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_sms_permission_in_order_to_send_an_sms))
                .onAllGranted(() -> {
                    inputPanel.clearQuote();
                    attachmentManager.clear(glideRequests, false);
                    silentlySetComposeText("");
                    final long id = fragment.stageOutgoingMessage(outgoingMessage);

                    new AsyncTask<Void, Void, Long>() {
                        @Override
                        protected Long doInBackground(Void... param) {
                            if (initiating) {
                                DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient, true);
                            }

                            return MessageSender.send(context, outgoingMessage, threadId, forceSms, () -> fragment.releaseOutgoingMessage(id));
                        }

                        @Override
                        protected void onPostExecute(Long result) {
                            sendComplete(result);
                            future.set(null);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                })
                .onAnyDenied(() -> future.set(null))
                .execute();

        return future;
    }

    private void sendTextMessage(final boolean forceSms, final long expiresIn, final int subscriptionId, final boolean initiatingConversation)
            throws InvalidMessageException {
        final Context context = getApplicationContext();
        final String messageBody = getMessage();

        OutgoingTextMessage message;

        if (isSecureText && !forceSms) {
            message = new OutgoingEncryptedMessage(recipient, messageBody, expiresIn);
//            MuzimaApplication.getInstance(context).getTypingStatusSender().onTypingStopped(threadId);
        } else {
            message = new OutgoingTextMessage(recipient, messageBody, expiresIn, subscriptionId);
        }

        Permissions.with(this)
                .request(Manifest.permission.SEND_SMS)
                .ifNecessary(forceSms || !isSecureText)
                .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_sms_permission_in_order_to_send_an_sms))
                .onAllGranted(() -> {
                    silentlySetComposeText("");
                    final long id = fragment.stageOutgoingMessage(message);

                    new AsyncTask<OutgoingTextMessage, Void, Long>() {
                        @Override
                        protected Long doInBackground(OutgoingTextMessage... messages) {
                            if (initiatingConversation) {
                                DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient, true);
                            }

                            return MessageSender.send(context, messages[0], threadId, forceSms, () -> fragment.releaseOutgoingMessage(id));
                        }

                        @Override
                        protected void onPostExecute(Long result) {
                            sendComplete(result);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);

                })
                .execute();
    }

    private void updateToggleButtonState() {
        if (composeText.getText().length() == 0 && !attachmentManager.isAttachmentPresent()) {
            buttonToggle.display(attachButton);
            quickAttachmentToggle.show();
            inlineAttachmentToggle.hide();
        } else {
            buttonToggle.display(sendButton);
            quickAttachmentToggle.hide();

            if (!attachmentManager.isAttachmentPresent()) {
                inlineAttachmentToggle.show();
            } else {
                inlineAttachmentToggle.hide();
            }
        }
    }

    private void recordSubscriptionIdPreference(final Optional<Integer> subscriptionId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DatabaseFactory.getRecipientDatabase(ConversationActivity.this)
                        .setDefaultSubscriptionId(recipient, subscriptionId.or(-1));
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onAttachmentDrawerStateChanged(QuickAttachmentDrawer.DrawerState drawerState) {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) throw new AssertionError();

        if (drawerState == QuickAttachmentDrawer.DrawerState.FULL_EXPANDED) {
            supportActionBar.hide();
        } else {
            supportActionBar.show();
        }

        if (drawerState == QuickAttachmentDrawer.DrawerState.COLLAPSED) {
            container.hideAttachedInput(true);
        }
    }

    @Override
    public void onImageCapture(@NonNull final byte[] imageBytes) {
        setMedia(PersistentBlobProvider.getInstance(this)
                        .create(this, imageBytes, MediaUtil.IMAGE_JPEG, null),
                MediaType.IMAGE);
        quickAttachmentDrawer.hide(false);
    }

    @Override
    public void onCameraFail() {
        Toast.makeText(this, R.string.ConversationActivity_quick_camera_unavailable, Toast.LENGTH_SHORT).show();
        quickAttachmentDrawer.hide(false);
        quickAttachmentToggle.disable();
    }

    @Override
    public void onCameraStart() {
    }

    @Override
    public void onCameraStop() {
    }

    @Override
    public void onRecorderPermissionRequired() {
        Permissions.with(this)
                .request(Manifest.permission.RECORD_AUDIO)
                .ifNecessary()
                .withRationaleDialog(getString(R.string.ConversationActivity_to_send_audio_messages_allow_signal_access_to_your_microphone), R.drawable.ic_mic_white_48dp)
                .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_requires_the_microphone_permission_in_order_to_send_audio_messages))
                .execute();
    }

    @Override
    public void onRecorderStarted() {
        Vibrator vibrator = ServiceUtil.getVibrator(this);
        vibrator.vibrate(20);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        audioRecorder.startRecording();
    }

    @Override
    public void onRecorderFinished() {
        Vibrator vibrator = ServiceUtil.getVibrator(this);
        vibrator.vibrate(20);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
        future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
            @Override
            public void onSuccess(final @NonNull Pair<Uri, Long> result) {
                boolean forceSms = sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
                int subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
                long expiresIn = recipient.getExpireMessages() * 1000L;
                boolean initiating = threadId == -1;
                AudioSlide audioSlide = new AudioSlide(ConversationActivity.this, result.first, result.second, MediaUtil.AUDIO_AAC, true);
                SlideDeck slideDeck = new SlideDeck();
                slideDeck.addSlide(audioSlide);

                sendMediaMessage(forceSms, "", slideDeck, Collections.emptyList(), expiresIn, subscriptionId, initiating).addListener(new AssertedSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void nothing) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                PersistentBlobProvider.getInstance(ConversationActivity.this).delete(ConversationActivity.this, result.first);
                                return null;
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                });
            }

            @Override
            public void onFailure(ExecutionException e) {
                Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_unable_to_record_audio, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRecorderCanceled() {
        Vibrator vibrator = ServiceUtil.getVibrator(this);
        vibrator.vibrate(50);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
        future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
            @Override
            public void onSuccess(final Pair<Uri, Long> result) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        PersistentBlobProvider.getInstance(ConversationActivity.this).delete(ConversationActivity.this, result.first);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            @Override
            public void onFailure(ExecutionException e) {
            }
        });
    }

    @Override
    public void onEmojiToggle() {
        if (!emojiDrawerStub.resolved()) {
            inputPanel.setEmojiDrawer(emojiDrawerStub.get());
            emojiDrawerStub.get().setEmojiEventListener(inputPanel);
        }

        if (container.getCurrentInput() == emojiDrawerStub.get()) {
            container.showSoftkey(composeText);
        } else {
            container.show(composeText, emojiDrawerStub.get());
        }
    }

    @Override
    public void onMediaSelected(@NonNull Uri uri, String contentType) {
        if (!TextUtils.isEmpty(contentType) && contentType.trim().equals("image/gif")) {
            setMedia(uri, MediaType.GIF);
        } else if (MediaUtil.isImageType(contentType)) {
            setMedia(uri, MediaType.IMAGE);
        } else if (MediaUtil.isVideoType(contentType)) {
            setMedia(uri, MediaType.VIDEO);
        } else if (MediaUtil.isAudioType(contentType)) {
            setMedia(uri, MediaType.AUDIO);
        }
    }

    private void silentlySetComposeText(String text) {
        typingTextWatcher.setEnabled(false);
        composeText.setText(text);
        typingTextWatcher.setEnabled(true);
    }

    // Listeners

    private class AttachmentTypeListener implements AttachmentTypeSelector.AttachmentClickedListener {
        @Override
        public void onClick(int type) {
            addAttachment(type);
        }

        @Override
        public void onQuickAttachment(Uri uri) {
            Intent intent = new Intent();
            intent.setData(uri);

            onActivityResult(PICK_GALLERY, RESULT_OK, intent);
        }
    }

    private class QuickCameraToggleListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Permissions.with(ConversationActivity.this)
                    .request(Manifest.permission.CAMERA)
                    .ifNecessary()
                    .withRationaleDialog(getString(R.string.ConversationActivity_to_capture_photos_and_video_allow_signal_access_to_the_camera), R.drawable.ic_photo_camera_white_48dp)
                    .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_the_camera_permission_to_take_photos_or_video))
                    .onAllGranted(() -> {
                        composeText.clearFocus();
                        startActivityForResult(CameraActivity.getIntent(ConversationActivity.this, sendButton.getSelectedTransport()), PICK_CAMERA);
                        overridePendingTransition(R.anim.camera_slide_from_bottom, R.anim.stationary);
                    })
                    .onAnyDenied(() -> Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_signal_needs_camera_permissions_to_take_photos_or_video, Toast.LENGTH_LONG).show())
                    .execute();
        }
    }

    private class SendButtonListener implements View.OnClickListener, TextView.OnEditorActionListener {
        @Override
        public void onClick(View v) {
            sendMessage();
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick();
                return true;
            }
            return false;
        }
    }

    private class AttachButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            handleAddAttachment();
        }
    }

    private class AttachButtonLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            return sendButton.performLongClick();
        }
    }

    private class ComposeKeyPressedListener implements View.OnKeyListener, View.OnClickListener, TextWatcher, View.OnFocusChangeListener {

        int beforeLength;

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (TextSecurePreferences.isEnterSendsEnabled(ConversationActivity.this)) {
                        sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                        sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            container.showSoftkey(composeText);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            beforeLength = composeText.getTextTrimmed().length();
        }

        @Override
        public void afterTextChanged(Editable s) {
            calculateCharactersRemaining();

            if (composeText.getTextTrimmed().length() == 0 || beforeLength == 0) {
                composeText.postDelayed(ConversationActivity.this::updateToggleButtonState, 50);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
        }
    }

    private class TypingStatusTextWatcher extends SimpleTextWatcher {

        private boolean enabled = true;

        @Override
        public void onTextChanged(String text) {
            if (enabled && threadId > 0 && isSecureText && !isSmsForced()) {
//                MuzimaApplication.getInstance(ConversationActivity.this).getTypingStatusSender().onTypingStarted(threadId);
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Override
    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    @Override
    public void handleReplyMessage(MessageRecord messageRecord) {
        SignalRecipient author;

        if (messageRecord.isOutgoing()) {
            author = SignalRecipient.from(this, SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(this)), true);
        } else {
            author = messageRecord.getIndividualRecipient();
        }

        if (messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getSharedContacts().isEmpty()) {
            Contact contact = ((MmsMessageRecord) messageRecord).getSharedContacts().get(0);
            String displayName = ContactUtil.getDisplayName(contact);
            String body = getString(R.string.ConversationActivity_quoted_contact_message, EmojiStrings.BUST_IN_SILHOUETTE, displayName);
            SlideDeck slideDeck = new SlideDeck();

            if (contact.getAvatarAttachment() != null) {
                slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, contact.getAvatarAttachment()));
            }

            inputPanel.setQuote(GlideApp.with(this),
                    messageRecord.getDateSent(),
                    author,
                    body,
                    slideDeck);
        } else {
            inputPanel.setQuote(GlideApp.with(this),
                    messageRecord.getDateSent(),
                    author,
                    messageRecord.getBody(),
                    messageRecord.isMms() ? ((MmsMessageRecord) messageRecord).getSlideDeck() : new SlideDeck());
        }
    }

    @Override
    public void onAttachmentChanged() {
        handleSecurityChange(isSecureText, isDefaultSms);
        updateToggleButtonState();
    }

    private class UnverifiedDismissedListener implements UnverifiedBannerView.DismissListener {
        @Override
        public void onDismissed(final List<IdentityRecord> unverifiedIdentities) {
            final IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(ConversationActivity.this);

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    synchronized (SESSION_LOCK) {
                        for (IdentityRecord identityRecord : unverifiedIdentities) {
                            identityDatabase.setVerified(identityRecord.getAddress(),
                                    identityRecord.getIdentityKey(),
                                    IdentityDatabase.VerifiedStatus.DEFAULT);
                        }
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    initializeIdentityRecords();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class UnverifiedClickedListener implements UnverifiedBannerView.ClickListener {
        @Override
        public void onClicked(final List<IdentityRecord> unverifiedIdentities) {
            Log.i(TAG, "onClicked: " + unverifiedIdentities.size());
            if (unverifiedIdentities.size() == 1) {
                Intent intent = new Intent(ConversationActivity.this, VerifyIdentityActivity.class);
                intent.putExtra(VerifyIdentityActivity.ADDRESS_EXTRA, unverifiedIdentities.get(0).getAddress());
                intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(unverifiedIdentities.get(0).getIdentityKey()));
                intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);

                startActivity(intent);
            } else {
                String[] unverifiedNames = new String[unverifiedIdentities.size()];

                for (int i = 0; i < unverifiedIdentities.size(); i++) {
                    unverifiedNames[i] = SignalRecipient.from(ConversationActivity.this, unverifiedIdentities.get(i).getAddress(), false).toShortString();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
                builder.setIconAttribute(R.attr.dialog_alert_icon);
                builder.setTitle("No longer verified");
                builder.setItems(unverifiedNames, (dialog, which) -> {
                    Intent intent = new Intent(ConversationActivity.this, VerifyIdentityActivity.class);
                    intent.putExtra(VerifyIdentityActivity.ADDRESS_EXTRA, unverifiedIdentities.get(which).getAddress());
                    intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(unverifiedIdentities.get(which).getIdentityKey()));
                    intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);

                    startActivity(intent);
                });
                builder.show();
            }
        }
    }

    private class QuoteRestorationTask extends AsyncTask<Void, Void, MessageRecord> {

        private final String serialized;
        private final SettableFuture<Boolean> future;

        QuoteRestorationTask(@NonNull String serialized, @NonNull SettableFuture<Boolean> future) {
            this.serialized = serialized;
            this.future = future;
        }

        @Override
        protected MessageRecord doInBackground(Void... voids) {
            QuoteId quoteId = QuoteId.deserialize(serialized);

            if (quoteId != null) {
                return DatabaseFactory.getMmsSmsDatabase(getApplicationContext()).getMessageFor(quoteId.getId(), quoteId.getAuthor());
            }

            return null;
        }

        @Override
        protected void onPostExecute(MessageRecord messageRecord) {
            if (messageRecord != null) {
                handleReplyMessage(messageRecord);
                future.set(true);
            } else {
                Log.e(TAG, "Failed to restore a quote from a draft. No matching message record.");
                future.set(false);
            }
        }
    }

}

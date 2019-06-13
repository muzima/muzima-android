package com.muzima.messaging;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.messaging.customcomponents.ConversationItem;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupReceiptDatabase;
import com.muzima.messaging.sqlite.database.GroupReceiptDatabase.GroupReceiptInfo;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.MmsSmsDatabase;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.sqlite.database.loaders.MessageDetailsLoader;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.utils.ExpirationUtil;
import com.muzima.messaging.MessageDetailsRecipientAdapter.RecipientDeliveryStatus;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.notifications.MessageNotifier;
import com.muzima.utils.DateUtils;
import com.muzima.utils.MaterialColor;

import org.whispersystems.libsignal.util.guava.Optional;

import java.lang.ref.WeakReference;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MessageDetailsActivity extends PassphraseRequiredActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>, RecipientModifiedListener {
    private final static String TAG = MessageDetailsActivity.class.getSimpleName();

    public final static String MESSAGE_ID_EXTRA = "message_id";
    public final static String THREAD_ID_EXTRA = "thread_id";
    public final static String IS_PUSH_GROUP_EXTRA = "is_push_group";
    public final static String TYPE_EXTRA = "type";
    public final static String ADDRESS_EXTRA = "address";

    private GlideRequests glideRequests;
    private long threadId;
    private boolean isPushGroup;
    private ConversationItem conversationItem;
    private ViewGroup itemParent;
    private View metadataContainer;
    private View expiresContainer;
    private TextView errorText;
    private View resendButton;
    private TextView sentDate;
    private TextView receivedDate;
    private TextView expiresInText;
    private View receivedContainer;
    private TextView transport;
    private TextView toFrom;
    private ListView recipientsList;
    private LayoutInflater inflater;

    private boolean running;

    @Override
    public void onCreate(Bundle bundle, boolean ready) {
        setContentView(R.layout.message_details_activity);
        running = true;

        initializeResources();
        initializeActionBar();
        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(R.string.menu_message_details);

        MessageNotifier.setVisibleThread(threadId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MessageNotifier.setVisibleThread(-1L);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        running = false;
    }

    private void initializeActionBar() {
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SignalRecipient recipient = SignalRecipient.from(this, getIntent().getParcelableExtra(ADDRESS_EXTRA), true);
        recipient.addListener(this);

        setActionBarColor(recipient.getColor());
    }

    private void setActionBarColor(MaterialColor color) {
        assert getSupportActionBar() != null;
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color.toStatusBarColor(this));
        }
    }

    @Override
    public void onModified(final SignalRecipient recipient) {
        Util.runOnMain(() -> setActionBarColor(recipient.getColor()));
    }

    private void initializeResources() {
        inflater = LayoutInflater.from(this);
        View header = inflater.inflate(R.layout.message_details_header, recipientsList, false);

        threadId = getIntent().getLongExtra(THREAD_ID_EXTRA, -1);
        isPushGroup = getIntent().getBooleanExtra(IS_PUSH_GROUP_EXTRA, false);
        glideRequests = GlideApp.with(this);
        itemParent = header.findViewById(R.id.item_container);
        recipientsList = findViewById(R.id.recipients_list);
        metadataContainer = header.findViewById(R.id.metadata_container);
        errorText = header.findViewById(R.id.error_text);
        resendButton = header.findViewById(R.id.resend_button);
        sentDate = header.findViewById(R.id.sent_time);
        receivedContainer = header.findViewById(R.id.received_container);
        receivedDate = header.findViewById(R.id.received_time);
        transport = header.findViewById(R.id.transport);
        toFrom = header.findViewById(R.id.tofrom);
        expiresContainer = header.findViewById(R.id.expires_container);
        expiresInText = header.findViewById(R.id.expires_in);
        recipientsList.setHeaderDividersEnabled(false);
        recipientsList.addHeaderView(header, null, false);
    }

    private void updateTransport(MessageRecord messageRecord) {
        final String transportText;
        if (messageRecord.isOutgoing() && messageRecord.isFailed()) {
            transportText = "-";
        } else if (messageRecord.isPending()) {
            transportText = getString(R.string.general_pending);
        } else if (messageRecord.isPush()) {
            transportText = getString(R.string.general_push_data);
        } else if (messageRecord.isMms()) {
            transportText = getString(R.string.general_mms);
        } else {
            transportText = getString(R.string.general_sms);
        }

        transport.setText(transportText);
    }

    private void updateTime(MessageRecord messageRecord) {
        sentDate.setOnLongClickListener(null);
        receivedDate.setOnLongClickListener(null);

        if (messageRecord.isPending() || messageRecord.isFailed()) {
            sentDate.setText("-");
            receivedContainer.setVisibility(View.GONE);
        } else {
            Locale dateLocale = getResources().getConfiguration().locale;
            SimpleDateFormat dateFormatter = DateUtils.getDetailedDateFormatter(this, dateLocale);
            sentDate.setText(dateFormatter.format(new Date(messageRecord.getDateSent())));
            sentDate.setOnLongClickListener(v -> {
                copyToClipboard(String.valueOf(messageRecord.getDateSent()));
                return true;
            });

            if (messageRecord.getDateReceived() != messageRecord.getDateSent() && !messageRecord.isOutgoing()) {
                receivedDate.setText(dateFormatter.format(new Date(messageRecord.getDateReceived())));
                receivedDate.setOnLongClickListener(v -> {
                    copyToClipboard(String.valueOf(messageRecord.getDateReceived()));
                    return true;
                });
                receivedContainer.setVisibility(View.VISIBLE);
            } else {
                receivedContainer.setVisibility(View.GONE);
            }
        }
    }

    private void updateExpirationTime(final MessageRecord messageRecord) {
        if (messageRecord.getExpiresIn() <= 0 || messageRecord.getExpireStarted() <= 0) {
            expiresContainer.setVisibility(View.GONE);
            return;
        }

        expiresContainer.setVisibility(View.VISIBLE);
        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - messageRecord.getExpireStarted();
                long remaining = messageRecord.getExpiresIn() - elapsed;

                String duration = ExpirationUtil.getExpirationDisplayValue(MessageDetailsActivity.this, Math.max((int) (remaining / 1000), 1));
                expiresInText.setText(duration);

                if (running) {
                    Util.runOnMainDelayed(this, 500);
                }
            }
        });
    }

    private void updateRecipients(MessageRecord messageRecord, SignalRecipient recipient, List<RecipientDeliveryStatus> recipients) {
        final int toFromRes;
        if (messageRecord.isMms() && !messageRecord.isPush() && !messageRecord.isOutgoing()) {
            toFromRes = R.string.general_with;
        } else if (messageRecord.isOutgoing()) {
            toFromRes = R.string.general_to;
        } else {
            toFromRes = R.string.general_from;
        }
        toFrom.setText(toFromRes);
        conversationItem.bind(messageRecord, Optional.absent(), Optional.absent(), glideRequests, getResources().getConfiguration().locale, new HashSet<>(), recipient, false);
        recipientsList.setAdapter(new MessageDetailsRecipientAdapter(this, glideRequests, messageRecord, recipients, isPushGroup));
    }

    private void inflateMessageViewIfAbsent(MessageRecord messageRecord) {
        if (conversationItem == null) {
            if (messageRecord.isGroupAction()) {
                conversationItem = (ConversationItem) inflater.inflate(R.layout.conversation_item_update, itemParent, false);
            } else if (messageRecord.isOutgoing()) {
                conversationItem = (ConversationItem) inflater.inflate(R.layout.conversation_item_sent, itemParent, false);
            } else {
                conversationItem = (ConversationItem) inflater.inflate(R.layout.conversation_item_received, itemParent, false);
            }
            itemParent.addView(conversationItem);
        }
    }

    private @Nullable
    MessageRecord getMessageRecord(Context context, Cursor cursor, String type) {
        switch (type) {
            case MmsSmsDatabase.SMS_TRANSPORT:
                SmsDatabase smsDatabase = DatabaseFactory.getSmsDatabase(context);
                SmsDatabase.Reader reader = smsDatabase.readerFor(cursor);
                return reader.getNext();
            case MmsSmsDatabase.MMS_TRANSPORT:
                MmsDatabase mmsDatabase = DatabaseFactory.getMmsDatabase(context);
                MmsDatabase.Reader mmsReader = mmsDatabase.readerFor(cursor);
                return mmsReader.getNext();
            default:
                throw new AssertionError("no valid message type specified");
        }
    }

    private void copyToClipboard(@NonNull String text) {
        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("text", text));
    }

    @Override
    public @NonNull
    Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new MessageDetailsLoader(this, getIntent().getStringExtra(TYPE_EXTRA),
                getIntent().getLongExtra(MESSAGE_ID_EXTRA, -1));
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        MessageRecord messageRecord = getMessageRecord(this, cursor, getIntent().getStringExtra(TYPE_EXTRA));

        if (messageRecord == null) {
            finish();
        } else {
            new MessageRecipientAsyncTask(this, messageRecord).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        recipientsList.setAdapter(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private class MessageRecipientAsyncTask extends AsyncTask<Void, Void, List<RecipientDeliveryStatus>> {

        private final WeakReference<Context> weakContext;
        private final MessageRecord messageRecord;

        MessageRecipientAsyncTask(@NonNull Context context, @NonNull MessageRecord messageRecord) {
            this.weakContext = new WeakReference<>(context);
            this.messageRecord = messageRecord;
        }

        protected Context getContext() {
            return weakContext.get();
        }

        @Override
        public List<RecipientDeliveryStatus> doInBackground(Void... voids) {
            Context context = getContext();

            if (context == null) {
                Log.w(TAG, "associated context is destroyed, finishing early");
                return null;
            }

            List<RecipientDeliveryStatus> recipients = new LinkedList<>();

            if (!messageRecord.getRecipient().isGroupRecipient()) {
                recipients.add(new RecipientDeliveryStatus(messageRecord.getRecipient(), getStatusFor(messageRecord.getDeliveryReceiptCount(), messageRecord.getReadReceiptCount(), messageRecord.isPending()), messageRecord.isUnidentified(), -1));
            } else {
                List<GroupReceiptInfo> receiptInfoList = DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageRecord.getId());

                if (receiptInfoList.isEmpty()) {
                    List<SignalRecipient> group = DatabaseFactory.getGroupDatabase(context).getGroupMembers(messageRecord.getRecipient().getAddress().toGroupString(), false);

                    for (SignalRecipient recipient : group) {
                        recipients.add(new RecipientDeliveryStatus(recipient, RecipientDeliveryStatus.Status.UNKNOWN, false, -1));
                    }
                } else {
                    for (GroupReceiptInfo info : receiptInfoList) {
                        recipients.add(new RecipientDeliveryStatus(SignalRecipient.from(context, info.getAddress(), true),
                                getStatusFor(info.getStatus(), messageRecord.isPending(), messageRecord.isFailed()),
                                info.isUnidentified(),
                                info.getTimestamp()));
                    }
                }
            }

            return recipients;
        }

        @Override
        public void onPostExecute(List<RecipientDeliveryStatus> recipients) {
            if (getContext() == null) {
                Log.w(TAG, "AsyncTask finished with a destroyed context, leaving early.");
                return;
            }

            inflateMessageViewIfAbsent(messageRecord);
            updateRecipients(messageRecord, messageRecord.getRecipient(), recipients);

            boolean isGroupNetworkFailure = messageRecord.isFailed() && !messageRecord.getNetworkFailures().isEmpty();
            boolean isIndividualNetworkFailure = messageRecord.isFailed() && !isPushGroup && messageRecord.getIdentityKeyMismatches().isEmpty();

            if (isGroupNetworkFailure || isIndividualNetworkFailure) {
                errorText.setVisibility(View.VISIBLE);
                resendButton.setVisibility(View.VISIBLE);
                resendButton.setOnClickListener(this::onResendClicked);
                metadataContainer.setVisibility(View.GONE);
            } else if (messageRecord.isFailed()) {
                errorText.setVisibility(View.VISIBLE);
                resendButton.setVisibility(View.GONE);
                resendButton.setOnClickListener(null);
                metadataContainer.setVisibility(View.GONE);
            } else {
                updateTransport(messageRecord);
                updateTime(messageRecord);
                updateExpirationTime(messageRecord);
                errorText.setVisibility(View.GONE);
                resendButton.setVisibility(View.GONE);
                resendButton.setOnClickListener(null);
                metadataContainer.setVisibility(View.VISIBLE);
            }
        }

        private RecipientDeliveryStatus.Status getStatusFor(int deliveryReceiptCount, int readReceiptCount, boolean pending) {
            if (readReceiptCount > 0) return RecipientDeliveryStatus.Status.READ;
            else if (deliveryReceiptCount > 0) return RecipientDeliveryStatus.Status.DELIVERED;
            else if (!pending) return RecipientDeliveryStatus.Status.SENT;
            else return RecipientDeliveryStatus.Status.PENDING;
        }

        private RecipientDeliveryStatus.Status getStatusFor(int groupStatus, boolean pending, boolean failed) {
            if (groupStatus == GroupReceiptDatabase.STATUS_READ)
                return RecipientDeliveryStatus.Status.READ;
            else if (groupStatus == GroupReceiptDatabase.STATUS_DELIVERED)
                return RecipientDeliveryStatus.Status.DELIVERED;
            else if (groupStatus == GroupReceiptDatabase.STATUS_UNDELIVERED && failed)
                return RecipientDeliveryStatus.Status.UNKNOWN;
            else if (groupStatus == GroupReceiptDatabase.STATUS_UNDELIVERED && !pending)
                return RecipientDeliveryStatus.Status.SENT;
            else if (groupStatus == GroupReceiptDatabase.STATUS_UNDELIVERED)
                return RecipientDeliveryStatus.Status.PENDING;
            else if (groupStatus == GroupReceiptDatabase.STATUS_UNKNOWN)
                return RecipientDeliveryStatus.Status.UNKNOWN;
            throw new AssertionError();
        }

        private void onResendClicked(View v) {
            MessageSender.resend(MessageDetailsActivity.this, messageRecord);
            resendButton.setVisibility(View.GONE);
        }
    }
}

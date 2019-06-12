package com.muzima.messaging.customcomponents;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.ViewUtil;

@SuppressLint("ViewConstructor")
public class UnknownSenderView extends FrameLayout {

    private final @NonNull SignalRecipient recipient;
    private final long threadId;

    public UnknownSenderView(@NonNull Context context, @NonNull SignalRecipient recipient, long threadId) {
        super(context);
        this.recipient = recipient;
        this.threadId = threadId;

        inflate(context, R.layout.unknown_sender_view, this);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        View block = ViewUtil.findById(this, R.id.block);
        View add = ViewUtil.findById(this, R.id.add_to_contacts);
        View profileAccess = ViewUtil.findById(this, R.id.share_profile);

        block.setOnClickListener(v -> handleBlock());
        add.setOnClickListener(v -> handleAdd());
        profileAccess.setOnClickListener(v -> handleProfileAccess());
    }

    private void handleBlock() {
        final Context context = getContext();

        new AlertDialog.Builder(getContext())
                .setIconAttribute(R.attr.dialog_alert_icon)
                .setTitle(getContext().getString(R.string.title_block_someone, recipient.toShortString()))
                .setMessage(R.string.message_block_someone)
                .setPositiveButton(R.string.general_block, (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            DatabaseFactory.getRecipientDatabase(context).setBlocked(recipient, true);
                            if (threadId != -1)
                                DatabaseFactory.getThreadDatabase(context).setHasSent(threadId, true);
                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void handleAdd() {
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);

        if (!TextUtils.isEmpty(recipient.getProfileName())) {
            intent.putExtra(ContactsContract.Intents.Insert.NAME, recipient.getProfileName());
        }

        if (recipient.getAddress().isEmail()) {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, recipient.getAddress().toEmailString());
        }

        if (recipient.getAddress().isPhone()) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, recipient.getAddress().toPhoneString());
        }

        getContext().startActivity(intent);
        if (threadId != -1)
            DatabaseFactory.getThreadDatabase(getContext()).setHasSent(threadId, true);
    }

    private void handleProfileAccess() {
        final Context context = getContext();

        new AlertDialog.Builder(getContext())
                .setIconAttribute(R.attr.dialog_info_icon)
                .setTitle(getContext().getString(R.string.title_share_profile, recipient.toShortString()))
                .setMessage(R.string.text_the_easiest_way_to_share_your_profile_information_is_to_add_the_sender_to_your_contacts)
                .setPositiveButton(R.string.general_share_profile, (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient, true);
                            if (threadId != -1)
                                DatabaseFactory.getThreadDatabase(context).setHasSent(threadId, true);
                            return null;
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}

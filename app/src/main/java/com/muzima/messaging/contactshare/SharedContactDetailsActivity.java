package com.muzima.messaging.contactshare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.CommunicationActions;
import com.muzima.messaging.PassphraseRequiredActionBarActivity;
import com.muzima.messaging.RecipientModifiedListener;
import com.muzima.messaging.jobs.DirectoryRefreshJob;
import com.muzima.messaging.mms.DecryptableStreamUriLoader;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.DynamicNoActionBarTheme;
import com.muzima.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedContactDetailsActivity extends PassphraseRequiredActionBarActivity implements RecipientModifiedListener {

    private static final int    CODE_ADD_EDIT_CONTACT = 2323;
    private static final String KEY_CONTACT           = "contact";

    private ContactFieldAdapter contactFieldAdapter;
    private TextView nameView;
    private TextView            numberView;
    private ImageView avatarView;
    private View addButtonView;
    private View                inviteButtonView;
    private ViewGroup engageContainerView;
    private View                messageButtonView;
    private View                callButtonView;

    private GlideRequests glideRequests;
    private Contact             contact;

    private final Map<String, SignalRecipient> activeRecipients = new HashMap<>();

    private ThemeUtils themeUtils = new DynamicNoActionBarTheme();

    @Override
    protected void onPreCreate() {
        themeUtils.onCreate(this);
    }

    public static Intent getIntent(@NonNull Context context, @NonNull Contact contact) {
        Intent intent = new Intent(context, SharedContactDetailsActivity.class);
        intent.putExtra(KEY_CONTACT, contact);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState, boolean ready) {
        setContentView(R.layout.activity_shared_contact_details);

        if (getIntent() == null) {
            throw new IllegalStateException("You must supply arguments to this activity. Please use the #getIntent() method.");
        }

        contact = getIntent().getParcelableExtra(KEY_CONTACT);
        if (contact == null) {
            throw new IllegalStateException("You must supply a contact to this activity. Please use the #getIntent() method.");
        }

        initToolbar();
        initViews();

        presentContact(contact);
        presentActionButtons(ContactUtil.getRecipients(this, contact));
        presentAvatar(contact.getAvatarAttachment() != null ? contact.getAvatarAttachment().getDataUri() : null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onCreate(this);
    }

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setLogo(null);
        getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int[]      attrs = {R.attr.shared_contact_details_titlebar};
            TypedArray array = obtainStyledAttributes(attrs);
            int        color = array.getResourceId(0, android.R.color.black);

            array.recycle();

            getWindow().setStatusBarColor(getResources().getColor(color));
        }
    }

    private void initViews() {
        nameView            = findViewById(R.id.contact_details_name);
        numberView          = findViewById(R.id.contact_details_number);
        avatarView          = findViewById(R.id.contact_details_avatar);
        addButtonView       = findViewById(R.id.contact_details_add_button);
        inviteButtonView    = findViewById(R.id.contact_details_invite_button);
        engageContainerView = findViewById(R.id.contact_details_engage_container);
        messageButtonView   = findViewById(R.id.contact_details_message_button);
        callButtonView      = findViewById(R.id.contact_details_call_button);

        contactFieldAdapter = new ContactFieldAdapter(getResources().getConfiguration().locale, glideRequests, false);

        RecyclerView list = findViewById(R.id.contact_details_fields);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(contactFieldAdapter);

        glideRequests = GlideApp.with(this);
    }

    @Override
    public void onModified(SignalRecipient recipient) {
        Util.runOnMain(() -> presentActionButtons(Collections.singletonList(recipient)));
    }

    @SuppressLint("StaticFieldLeak")
    private void presentContact(@Nullable Contact contact) {
        this.contact = contact;

        if (contact != null) {
            nameView.setText(ContactUtil.getDisplayName(contact));
            numberView.setText(ContactUtil.getDisplayNumber(contact, getResources().getConfiguration().locale));

            addButtonView.setOnClickListener(v -> {
                new AsyncTask<Void, Void, Intent>() {
                    @Override
                    protected Intent doInBackground(Void... voids) {
                        return ContactUtil.buildAddToContactsIntent(SharedContactDetailsActivity.this, contact);
                    }

                    @Override
                    protected void onPostExecute(Intent intent) {
                        startActivityForResult(intent, CODE_ADD_EDIT_CONTACT);
                    }
                }.execute();
            });

            contactFieldAdapter.setFields(this, null, contact.getPhoneNumbers(), contact.getEmails(), contact.getPostalAddresses());
        } else {
            nameView.setText("");
            numberView.setText("");
        }
    }

    public void presentAvatar(@Nullable Uri uri) {
        if (uri != null) {
            glideRequests.load(new DecryptableStreamUriLoader.DecryptableUri(uri))
                    .fallback(R.drawable.ic_contact_picture)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(avatarView);
        } else {
            glideRequests.load(R.drawable.ic_contact_picture)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(avatarView);
        }
    }

    private void presentActionButtons(@NonNull List<SignalRecipient> recipients) {
        for (SignalRecipient recipient : recipients) {
            activeRecipients.put(recipient.getAddress().serialize(), recipient);
        }

        List<SignalRecipient> pushUsers   = new ArrayList<>(recipients.size());
        List<SignalRecipient> systemUsers = new ArrayList<>(recipients.size());

        for (SignalRecipient recipient : activeRecipients.values()) {
            recipient.addListener(this);

            if (recipient.getRegistered() == RecipientDatabase.RegisteredState.REGISTERED) {
                pushUsers.add(recipient);
            } else if (recipient.isSystemContact()) {
                systemUsers.add(recipient);
            }
        }

        if (!pushUsers.isEmpty()) {
            engageContainerView.setVisibility(View.VISIBLE);
            inviteButtonView.setVisibility(View.GONE);

            messageButtonView.setOnClickListener(v -> {
                ContactUtil.selectRecipientThroughDialog(this, pushUsers, getResources().getConfiguration().locale, recipient -> {
                    CommunicationActions.startConversation(this, recipient, null);
                });
            });

            callButtonView.setOnClickListener(v -> {
                ContactUtil.selectRecipientThroughDialog(this, pushUsers, getResources().getConfiguration().locale, recipient -> CommunicationActions.startVoiceCall(this, recipient));
            });
        } else if (!systemUsers.isEmpty()) {
            inviteButtonView.setVisibility(View.VISIBLE);
            engageContainerView.setVisibility(View.GONE);

            inviteButtonView.setOnClickListener(v -> {
                ContactUtil.selectRecipientThroughDialog(this, systemUsers, getResources().getConfiguration().locale, recipient -> {
                    CommunicationActions.composeSmsThroughDefaultApp(this, recipient.getAddress(), getString(R.string.InviteActivity_lets_switch_to_signal, "https://sgnl.link/1KpeYmF"));
                });
            });
        } else {
            inviteButtonView.setVisibility(View.GONE);
            engageContainerView.setVisibility(View.GONE);
        }
    }

    private void clearView() {
        nameView.setText("");
        numberView.setText("");
        inviteButtonView.setVisibility(View.GONE);
        engageContainerView.setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CODE_ADD_EDIT_CONTACT && contact != null) {
            MuzimaApplication.getInstance(getApplicationContext())
                    .getJobManager()
                    .add(new DirectoryRefreshJob(getApplicationContext(), false));
        }
    }
}

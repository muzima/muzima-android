package com.muzima.messaging;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.muzima.R;
import com.muzima.adapters.messaging.SelectedRecipientsAdapter;
import com.muzima.messaging.contacts.RecipientsEditor;
import com.muzima.messaging.contacts.avatars.ContactColors;
import com.muzima.messaging.customcomponents.PushRecipientsPanel;
import com.muzima.messaging.fragments.ContactSelectionListFragment;
import com.muzima.messaging.group.GroupManager;
import com.muzima.messaging.group.GroupManager.GroupActionResult;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.GroupDatabase.GroupRecord;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.contacts.ContactsCursorLoader.DisplayMode;
import com.muzima.messaging.tasks.ProgressDialogAsyncTask;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.BitmapUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.ViewUtil;
import com.soundcloud.android.crop.Crop;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.util.InvalidNumberException;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GroupCreateActivity extends PassphraseRequiredActionBarActivity
        implements SelectedRecipientsAdapter.OnRecipientDeletedListener,
        PushRecipientsPanel.RecipientsPanelChangedListener {

    private final static String TAG = GroupCreateActivity.class.getSimpleName();

    public static final String GROUP_ADDRESS_EXTRA = "group_recipient";
    public static final String GROUP_THREAD_EXTRA = "group_thread";

    private static final int PICK_CONTACT = 1;
    public static final int AVATAR_SIZE = 210;

    private EditText groupName;
    private ListView lv;
    private ImageView avatar;
    private TextView creatingText;
    private Bitmap avatarBmp;

    @NonNull private Optional<GroupData> groupToUpdate = Optional.absent();
    private final ThemeUtils themeUtils = new ThemeUtils();

    @Override
    protected void onPreCreate() {
        themeUtils.onCreate(this);
    }

    @Override
    protected void onCreate(Bundle state, boolean ready) {
        setContentView(R.layout.group_create_activity);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initializeResources();
        initializeExistingGroup();
    }

    @Override
    public void onResume() {
        super.onResume();
        themeUtils.onCreate(this);
        updateViewState();
    }

    private boolean isSignalGroup() {
        return TextSecurePreferences.isPushRegistered(this) && !getAdapter().hasNonPushMembers();
    }

    private void disableSignalGroupViews(int reasonResId) {
        View pushDisabled = findViewById(R.id.push_disabled);
        pushDisabled.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.push_disabled_reason)).setText(reasonResId);
        avatar.setEnabled(false);
        groupName.setEnabled(false);
    }

    private void enableSignalGroupViews() {
        findViewById(R.id.push_disabled).setVisibility(View.GONE);
        avatar.setEnabled(true);
        groupName.setEnabled(true);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateViewState() {
        if (!TextSecurePreferences.isPushRegistered(this)) {
            disableSignalGroupViews(R.string.hint_not_registered_to_muzima);
            getSupportActionBar().setTitle(R.string.title_new_mms);
        } else if (getAdapter().hasNonPushMembers()) {
            disableSignalGroupViews(R.string.hint_contacts_dont_support_push);
            getSupportActionBar().setTitle(R.string.title_new_mms);
        } else {
            enableSignalGroupViews();
            getSupportActionBar().setTitle(groupToUpdate.isPresent()
                    ? R.string.menu_edit_group
                    : R.string.general_new_group);
        }
    }

    private static boolean isActiveInDirectory(SignalRecipient recipient) {
        return recipient.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED;
    }

    private void addSelectedContacts(@NonNull SignalRecipient... recipients) {
        new AddMembersTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipients);
    }

    private void addSelectedContacts(@NonNull Collection<SignalRecipient> recipients) {
        addSelectedContacts(recipients.toArray(new SignalRecipient[recipients.size()]));
    }

    private void initializeResources() {
        RecipientsEditor recipientsEditor = ViewUtil.findById(this, R.id.recipients_text);
        PushRecipientsPanel recipientsPanel = ViewUtil.findById(this, R.id.recipients);
        lv = ViewUtil.findById(this, R.id.selected_contacts_list);
        avatar = ViewUtil.findById(this, R.id.avatar);
        groupName = ViewUtil.findById(this, R.id.group_name);
        creatingText = ViewUtil.findById(this, R.id.creating_group_text);
        SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(this);
        adapter.setOnRecipientDeletedListener(this);
        lv.setAdapter(adapter);
        recipientsEditor.setHint(R.string.hint_add_members);
        recipientsPanel.setPanelChangeListener(this);
        findViewById(R.id.contacts_button).setOnClickListener(new AddRecipientButtonListener());
        avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_group_white_24dp).asDrawable(this, ContactColors.UNKNOWN_COLOR.toConversationColor(this)));
        avatar.setOnClickListener(view -> Crop.pickImage(GroupCreateActivity.this));
    }

    private void initializeExistingGroup() {
        final SignalAddress groupAddress = getIntent().getParcelableExtra(GROUP_ADDRESS_EXTRA);

        if (groupAddress != null) {
            new FillExistingGroupInfoAsyncTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, groupAddress.toGroupString());
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.group_create, menu);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_create_group:
                if (groupToUpdate.isPresent()) handleGroupUpdate();
                else handleGroupCreate();
                return true;
        }

        return false;
    }

    @Override
    public void onRecipientDeleted(SignalRecipient recipient) {
        getAdapter().remove(recipient);
        updateViewState();
    }

    @Override
    public void onRecipientsPanelUpdate(List<SignalRecipient> recipients) {
        if (recipients != null && !recipients.isEmpty()) addSelectedContacts(recipients);
    }

    private void handleGroupCreate() {
        if (getAdapter().getCount() < 1) {
            Log.i(TAG, getString(R.string.warning_no_members));
            Toast.makeText(getApplicationContext(), R.string.warning_no_members, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isSignalGroup()) {
            new CreateSignalGroupTask(this, avatarBmp, getGroupName(), getAdapter().getRecipients()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new CreateMmsGroupTask(this, getAdapter().getRecipients()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void handleGroupUpdate() {
        new UpdateSignalGroupTask(this, groupToUpdate.get().id, avatarBmp,
                getGroupName(), getAdapter().getRecipients()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleOpenConversation(long threadId, SignalRecipient recipient) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        startActivity(intent);
        finish();
    }

    private SelectedRecipientsAdapter getAdapter() {
        return (SelectedRecipientsAdapter) lv.getAdapter();
    }

    private @Nullable String getGroupName() {
        return groupName.getText() != null ? groupName.getText().toString() : null;
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, final Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));

        if (data == null || resultCode != Activity.RESULT_OK)
            return;

        switch (reqCode) {
            case PICK_CONTACT:
                List<String> selected = data.getStringArrayListExtra("contacts");

                for (String contact : selected) {
                    SignalAddress address = SignalAddress.fromExternal(this, contact);
                    SignalRecipient recipient = SignalRecipient.from(this, address, false);

                    addSelectedContacts(recipient);
                }
                break;

            case Crop.REQUEST_PICK:
                new Crop(data.getData()).output(outputFile).asSquare().start(this);
                break;
            case Crop.REQUEST_CROP:
                GlideApp.with(this)
                        .asBitmap()
                        .load(Crop.getOutput(data))
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .centerCrop()
                        .override(AVATAR_SIZE, AVATAR_SIZE)
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                setAvatar(Crop.getOutput(data), resource);
                            }
                        });
        }
    }

    private class AddRecipientButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(GroupCreateActivity.this, PushContactSelectionActivity.class);
            if (groupToUpdate.isPresent()) {
                intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_PUSH);
            } else {
                intent.putExtra(ContactSelectionListFragment.DISPLAY_MODE, DisplayMode.FLAG_PUSH | DisplayMode.FLAG_SMS);
            }
            startActivityForResult(intent, PICK_CONTACT);
        }
    }

    private static class CreateMmsGroupTask extends AsyncTask<Void, Void, GroupActionResult> {
        private final GroupCreateActivity activity;
        private final Set<SignalRecipient> members;

        public CreateMmsGroupTask(GroupCreateActivity activity, Set<SignalRecipient> members) {
            this.activity = activity;
            this.members = members;
        }

        @Override
        protected GroupActionResult doInBackground(Void... avoid) {
            List<SignalAddress> memberAddresses = new LinkedList<>();

            for (SignalRecipient recipient : members) {
                memberAddresses.add(recipient.getAddress());
            }

            String groupId = DatabaseFactory.getGroupDatabase(activity).getOrCreateGroupForMembers(memberAddresses, true);
            SignalRecipient groupRecipient = SignalRecipient.from(activity, SignalAddress.fromSerialized(groupId), true);
            long threadId = DatabaseFactory.getThreadDatabase(activity).getThreadIdFor(groupRecipient, ThreadDatabase.DistributionTypes.DEFAULT);

            return new GroupActionResult(groupRecipient, threadId);
        }

        @Override
        protected void onPostExecute(GroupActionResult result) {
            activity.handleOpenConversation(result.getThreadId(), result.getGroupRecipient());
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private abstract static class SignalGroupTask extends AsyncTask<Void, Void, Optional<GroupActionResult>> {

        protected GroupCreateActivity activity;
        protected Bitmap avatar;
        protected Set<SignalRecipient> members;
        protected String name;

        public SignalGroupTask(GroupCreateActivity activity,
                               Bitmap avatar,
                               String name,
                               Set<SignalRecipient> members) {
            this.activity = activity;
            this.avatar = avatar;
            this.name = name;
            this.members = members;
        }

        @Override
        protected void onPreExecute() {
            activity.findViewById(R.id.group_details_layout).setVisibility(View.GONE);
            activity.findViewById(R.id.creating_group_layout).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.menu_create_group).setVisibility(View.GONE);
            final int titleResId = activity.groupToUpdate.isPresent()
                    ? R.string.hint_updating_group
                    : R.string.hint_creating_group;
            activity.creatingText.setText(activity.getString(titleResId, activity.getGroupName()));
        }

        @Override
        protected void onPostExecute(Optional<GroupActionResult> groupActionResultOptional) {
            if (activity.isFinishing()) return;
            activity.findViewById(R.id.group_details_layout).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.creating_group_layout).setVisibility(View.GONE);
            activity.findViewById(R.id.menu_create_group).setVisibility(View.VISIBLE);
        }
    }

    private static class CreateSignalGroupTask extends SignalGroupTask {
        public CreateSignalGroupTask(GroupCreateActivity activity, Bitmap avatar, String name, Set<SignalRecipient> members) {
            super(activity, avatar, name, members);
        }

        @Override
        protected Optional<GroupActionResult> doInBackground(Void... aVoid) {
            return Optional.of(GroupManager.createGroup(activity, members, avatar, name, false));
        }

        @Override
        protected void onPostExecute(Optional<GroupActionResult> result) {
            if (result.isPresent() && result.get().getThreadId() > -1) {
                if (!activity.isFinishing()) {
                    activity.handleOpenConversation(result.get().getThreadId(), result.get().getGroupRecipient());
                }
            } else {
                super.onPostExecute(result);
                Toast.makeText(activity.getApplicationContext(),
                        R.string.warning_invalid_group_member_number, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class UpdateSignalGroupTask extends SignalGroupTask {
        private String groupId;

        public UpdateSignalGroupTask(GroupCreateActivity activity, String groupId,
                                     Bitmap avatar, String name, Set<SignalRecipient> members) {
            super(activity, avatar, name, members);
            this.groupId = groupId;
        }

        @Override
        protected Optional<GroupActionResult> doInBackground(Void... aVoid) {
            try {
                return Optional.of(GroupManager.updateGroup(activity, groupId, members, avatar, name));
            } catch (InvalidNumberException e) {
                return Optional.absent();
            }
        }

        @Override
        protected void onPostExecute(Optional<GroupActionResult> result) {
            if (result.isPresent() && result.get().getThreadId() > -1) {
                if (!activity.isFinishing()) {
                    Intent intent = activity.getIntent();
                    intent.putExtra(GROUP_THREAD_EXTRA, result.get().getThreadId());
                    intent.putExtra(GROUP_ADDRESS_EXTRA, result.get().getGroupRecipient().getAddress());
                    activity.setResult(RESULT_OK, intent);
                    activity.finish();
                }
            } else {
                super.onPostExecute(result);
                Toast.makeText(activity.getApplicationContext(),
                        R.string.warning_invalid_group_member_number, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static class AddMembersTask extends AsyncTask<SignalRecipient, Void, List<AddMembersTask.Result>> {
        static class Result {
            Optional<SignalRecipient> recipient;
            boolean isPush;
            String reason;

            public Result(@Nullable SignalRecipient recipient, boolean isPush, @Nullable String reason) {
                this.recipient = Optional.fromNullable(recipient);
                this.isPush = isPush;
                this.reason = reason;
            }
        }

        private GroupCreateActivity activity;
        private boolean failIfNotPush;

        public AddMembersTask(@NonNull GroupCreateActivity activity) {
            this.activity = activity;
            this.failIfNotPush = activity.groupToUpdate.isPresent();
        }

        @Override
        protected List<Result> doInBackground(SignalRecipient... recipients) {
            final List<Result> results = new LinkedList<>();

            for (SignalRecipient recipient : recipients) {
                boolean isPush = isActiveInDirectory(recipient);

                if (failIfNotPush && !isPush) {
                    results.add(new Result(null, false, activity.getString(R.string.warning_cannot_add_non_push_to_existing_group,
                            recipient.toShortString())));
                } else if (TextUtils.equals(TextSecurePreferences.getLocalNumber(activity), recipient.getAddress().serialize())) {
                    results.add(new Result(null, false, activity.getString(R.string.warning_already_in_the_group)));
                } else {
                    results.add(new Result(recipient, isPush, null));
                }
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<Result> results) {
            if (activity.isFinishing()) return;

            for (Result result : results) {
                if (result.recipient.isPresent()) {
                    activity.getAdapter().add(result.recipient.get(), result.isPush);
                } else {
                    Toast.makeText(activity, result.reason, Toast.LENGTH_SHORT).show();
                }
            }
            activity.updateViewState();
        }
    }

    private static class FillExistingGroupInfoAsyncTask extends ProgressDialogAsyncTask<String, Void, Optional<GroupData>> {
        private GroupCreateActivity activity;

        public FillExistingGroupInfoAsyncTask(GroupCreateActivity activity) {
            super(activity,
                    R.string.hint_loading_group_details,
                    R.string.please_wait);
            this.activity = activity;
        }

        @Override
        protected Optional<GroupData> doInBackground(String... groupIds) {
            final GroupDatabase db = DatabaseFactory.getGroupDatabase(activity);
            final List<SignalRecipient> recipients = db.getGroupMembers(groupIds[0], false);
            final Optional<GroupRecord> group = db.getGroup(groupIds[0]);
            final Set<SignalRecipient> existingContacts = new HashSet<>(recipients.size());
            existingContacts.addAll(recipients);

            if (group.isPresent()) {
                return Optional.of(new GroupData(groupIds[0],
                        existingContacts,
                        BitmapUtil.fromByteArray(group.get().getAvatar()),
                        group.get().getAvatar(),
                        group.get().getTitle()));
            } else {
                return Optional.absent();
            }
        }

        @Override
        protected void onPostExecute(Optional<GroupData> group) {
            super.onPostExecute(group);

            if (group.isPresent() && !activity.isFinishing()) {
                activity.groupToUpdate = group;

                activity.groupName.setText(group.get().name);
                if (group.get().avatarBmp != null) {
                    activity.setAvatar(group.get().avatarBytes, group.get().avatarBmp);
                }
                SelectedRecipientsAdapter adapter = new SelectedRecipientsAdapter(activity, group.get().recipients);
                adapter.setOnRecipientDeletedListener(activity);
                activity.lv.setAdapter(adapter);
                activity.updateViewState();
            }
        }
    }

    private <T> void setAvatar(T model, Bitmap bitmap) {
        avatarBmp = bitmap;
        GlideApp.with(this)
                .load(model)
                .circleCrop()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(avatar);
    }

    private static class GroupData {
        String id;
        Set<SignalRecipient> recipients;
        Bitmap avatarBmp;
        byte[] avatarBytes;
        String name;

        public GroupData(String id, Set<SignalRecipient> recipients, Bitmap avatarBmp, byte[] avatarBytes, String name) {
            this.id = id;
            this.recipients = recipients;
            this.avatarBmp = avatarBmp;
            this.avatarBytes = avatarBytes;
            this.name = name;
        }
    }
}

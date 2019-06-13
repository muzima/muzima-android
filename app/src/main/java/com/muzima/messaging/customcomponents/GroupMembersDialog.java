package com.muzima.messaging.customcomponents;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.muzima.R;
import com.muzima.messaging.RecipientPreferenceActivity;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.utils.Util;
import com.muzima.model.SignalRecipient;

import java.util.LinkedList;
import java.util.List;

public class GroupMembersDialog extends AsyncTask<Void, Void, List<SignalRecipient>> {

    private static final String TAG = GroupMembersDialog.class.getSimpleName();

    private final SignalRecipient  recipient;
    private final Context context;

    public GroupMembersDialog(Context context, SignalRecipient recipient) {
        this.recipient = recipient;
        this.context   = context;
    }

    @Override
    public void onPreExecute() {}

    @Override
    protected List<SignalRecipient> doInBackground(Void... params) {
        return DatabaseFactory.getGroupDatabase(context).getGroupMembers(recipient.getAddress().toGroupString(), true);
    }

    @Override
    public void onPostExecute(List<SignalRecipient> members) {
        GroupMembers groupMembers = new GroupMembers(members);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.general_group_members);
        builder.setIconAttribute(R.attr.group_members_dialog_icon);
        builder.setCancelable(true);
        builder.setItems(groupMembers.getRecipientStrings(), new GroupMembersOnClickListener(context, groupMembers));
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }

    public void display() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class GroupMembersOnClickListener implements DialogInterface.OnClickListener {
        private final GroupMembers groupMembers;
        private final Context      context;

        public GroupMembersOnClickListener(Context context, GroupMembers members) {
            this.context      = context;
            this.groupMembers = members;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int item) {
            SignalRecipient recipient = groupMembers.get(item);

            if (recipient.getContactUri() != null) {
                Intent intent = new Intent(context, RecipientPreferenceActivity.class);
                intent.putExtra(RecipientPreferenceActivity.ADDRESS_EXTRA, recipient.getAddress());

                context.startActivity(intent);
            } else {
                final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                if (recipient.getAddress().isEmail()) {
                    intent.putExtra(ContactsContract.Intents.Insert.EMAIL, recipient.getAddress().toEmailString());
                } else {
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE, recipient.getAddress().toPhoneString());
                }
                intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                context.startActivity(intent);
            }
        }
    }

    /**
     * Wraps a List of SignalRecipient (just like @class Recipients),
     * but with focus on the order of the Recipients.
     * So that the order of the RecipientStrings[] matches
     * the internal order.
     *
     * @author Christoph Haefner
     */
    private class GroupMembers {
        private final String TAG = GroupMembers.class.getSimpleName();

        private final LinkedList<SignalRecipient> members = new LinkedList<>();

        public GroupMembers(List<SignalRecipient> recipients) {
            for (SignalRecipient recipient : recipients) {
                if (isLocalNumber(recipient)) {
                    members.push(recipient);
                } else {
                    members.add(recipient);
                }
            }
        }

        public String[] getRecipientStrings() {
            List<String> recipientStrings = new LinkedList<>();

            for (SignalRecipient recipient : members) {
                if (isLocalNumber(recipient)) {
                    recipientStrings.add(context.getString(R.string.general_me));
                } else {
                    String name = recipient.toShortString();

                    if (recipient.getName() == null && !TextUtils.isEmpty(recipient.getProfileName())) {
                        name += " ~" + recipient.getProfileName();
                    }

                    recipientStrings.add(name);
                }
            }

            return recipientStrings.toArray(new String[members.size()]);
        }

        public SignalRecipient get(int index) {
            return members.get(index);
        }

        private boolean isLocalNumber(SignalRecipient recipient) {
            return Util.isOwnNumber(context, recipient.getAddress());
        }
    }
}

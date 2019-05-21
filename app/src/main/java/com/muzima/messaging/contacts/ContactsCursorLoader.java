package com.muzima.messaging.contacts;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.util.Log;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.ContactsDatabase;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.NumberUtil;
import com.muzima.utils.Permissions;

import java.util.ArrayList;
import java.util.List;

public class ContactsCursorLoader extends CursorLoader {

    private static final String TAG = ContactsCursorLoader.class.getSimpleName();

    public static final class DisplayMode {
        public static final int FLAG_PUSH   = 1;
        public static final int FLAG_SMS    = 1 << 1;
        public static final int FLAG_GROUPS = 1 << 2;
        public static final int FLAG_ALL    = FLAG_PUSH | FLAG_SMS | FLAG_GROUPS;
    }

    private static final String[] CONTACT_PROJECTION = new String[]{ContactsDatabase.NAME_COLUMN,
            ContactsDatabase.NUMBER_COLUMN,
            ContactsDatabase.NUMBER_TYPE_COLUMN,
            ContactsDatabase.LABEL_COLUMN,
            ContactsDatabase.CONTACT_TYPE_COLUMN};

    private static final int RECENT_CONVERSATION_MAX = 25;

    private final String  filter;
    private final int     mode;
    private final boolean recents;

    public ContactsCursorLoader(@NonNull Context context, int mode, String filter, boolean recents)
    {
        super(context);

        this.filter       = filter;
        this.mode         = mode;
        this.recents      = recents;
    }

    @Override
    public Cursor loadInBackground() {
        List<Cursor> cursorList = TextUtils.isEmpty(filter) ? getUnfilteredResults()
                : getFilteredResults();
        if (cursorList.size() > 0) {
            return new MergeCursor(cursorList.toArray(new Cursor[0]));
        }
        return null;
    }

    private List<Cursor> getUnfilteredResults() {
        ArrayList<Cursor> cursorList = new ArrayList<>();

        if (recents) {
            Cursor recentConversations = getRecentConversationsCursor();
            if (recentConversations.getCount() > 0) {
                cursorList.add(getRecentsHeaderCursor());
                cursorList.add(recentConversations);
                cursorList.add(getContactsHeaderCursor());
            }
        }
        cursorList.addAll(getContactsCursors());
        return cursorList;
    }

    private List<Cursor> getFilteredResults() {
        ArrayList<Cursor> cursorList = new ArrayList<>();

        if (groupsEnabled(mode)) {
            Cursor groups = getGroupsCursor();
            if (groups.getCount() > 0) {
                List<Cursor> contacts = getContactsCursors();
                if (!isCursorListEmpty(contacts)) {
                    cursorList.add(getContactsHeaderCursor());
                    cursorList.addAll(contacts);
                    cursorList.add(getGroupsHeaderCursor());
                }
                cursorList.add(groups);
            } else {
                cursorList.addAll(getContactsCursors());
            }
        } else {
            cursorList.addAll(getContactsCursors());
        }

        if (NumberUtil.isValidSmsOrEmail(filter)) {
            cursorList.add(getNewNumberCursor());
        }

        return cursorList;
    }

    private Cursor getRecentsHeaderCursor() {
        MatrixCursor recentsHeader = new MatrixCursor(CONTACT_PROJECTION);
        recentsHeader.addRow(new Object[]{ getContext().getString(R.string.ContactsCursorLoader_recent_chats),
                "",
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                "",
                ContactsDatabase.DIVIDER_TYPE });
        return recentsHeader;
    }

    private Cursor getContactsHeaderCursor() {
        MatrixCursor contactsHeader = new MatrixCursor(CONTACT_PROJECTION, 1);
        contactsHeader.addRow(new Object[] { getContext().getString(R.string.ContactsCursorLoader_contacts),
                "",
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                "",
                ContactsDatabase.DIVIDER_TYPE });
        return contactsHeader;
    }

    private Cursor getGroupsHeaderCursor() {
        MatrixCursor groupHeader = new MatrixCursor(CONTACT_PROJECTION, 1);
        groupHeader.addRow(new Object[]{ getContext().getString(R.string.ContactsCursorLoader_groups),
                "",
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                "",
                ContactsDatabase.DIVIDER_TYPE });
        return groupHeader;
    }


    private Cursor getRecentConversationsCursor() {
        ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(getContext());

        MatrixCursor recentConversations = new MatrixCursor(CONTACT_PROJECTION, RECENT_CONVERSATION_MAX);
        try (Cursor rawConversations = threadDatabase.getRecentConversationList(RECENT_CONVERSATION_MAX)) {
            ThreadDatabase.Reader reader = threadDatabase.readerFor(rawConversations);
            ThreadRecord threadRecord;
            while ((threadRecord = reader.getNext()) != null) {
                recentConversations.addRow(new Object[] { threadRecord.getRecipient().toShortString(),
                        threadRecord.getRecipient().getAddress().serialize(),
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                        "",
                        ContactsDatabase.RECENT_TYPE });
            }
        }
        return recentConversations;
    }

    private List<Cursor> getContactsCursors() {
        ContactsDatabase contactsDatabase = DatabaseFactory.getContactsDatabase(getContext());
        List<Cursor>     cursorList       = new ArrayList<>(2);

        if (!Permissions.hasAny(getContext(), Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) {
            return cursorList;
        }

        if (pushEnabled(mode)) {
            cursorList.add(contactsDatabase.queryTextSecureContacts(filter));
        }

        if (pushEnabled(mode) && smsEnabled(mode)) {
            cursorList.add(contactsDatabase.querySystemContacts(filter));
        } else if (smsEnabled(mode)) {
            cursorList.add(filterNonPushContacts(contactsDatabase.querySystemContacts(filter)));
        }
        return cursorList;
    }

    private Cursor getGroupsCursor() {
        MatrixCursor groupContacts = new MatrixCursor(CONTACT_PROJECTION);
        try (GroupDatabase.Reader reader = DatabaseFactory.getGroupDatabase(getContext()).getGroupsFilteredByTitle(filter)) {
            GroupDatabase.GroupRecord groupRecord;
            while ((groupRecord = reader.getNext()) != null) {
                groupContacts.addRow(new Object[] { groupRecord.getTitle(),
                        groupRecord.getEncodedId(),
                        ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                        "",
                        ContactsDatabase.NORMAL_TYPE });
            }
        }
        return groupContacts;
    }

    private Cursor getNewNumberCursor() {
        MatrixCursor newNumberCursor = new MatrixCursor(CONTACT_PROJECTION, 1);
        newNumberCursor.addRow(new Object[] { getContext().getString(R.string.contact_selection_list__unknown_contact),
                filter,
                ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
                "\u21e2",
                ContactsDatabase.NEW_TYPE });
        return newNumberCursor;
    }

    private @NonNull Cursor filterNonPushContacts(@NonNull Cursor cursor) {
        try {
            final long startMillis = System.currentTimeMillis();
            final MatrixCursor matrix = new MatrixCursor(CONTACT_PROJECTION);
            while (cursor.moveToNext()) {
                final String    number    = cursor.getString(cursor.getColumnIndexOrThrow(ContactsDatabase.NUMBER_COLUMN));
                final SignalRecipient recipient = SignalRecipient.from(getContext(), SignalAddress.fromExternal(getContext(), number), false);

                if (recipient.resolve().getRegistered() != RecipientDatabase.RegisteredState.REGISTERED) {
                    matrix.addRow(new Object[]{cursor.getString(cursor.getColumnIndexOrThrow(ContactsDatabase.NAME_COLUMN)),
                            number,
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsDatabase.NUMBER_TYPE_COLUMN)),
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsDatabase.LABEL_COLUMN)),
                            ContactsDatabase.NORMAL_TYPE});
                }
            }
            Log.i(TAG, "filterNonPushContacts() -> " + (System.currentTimeMillis() - startMillis) + "ms");
            return matrix;
        } finally {
            cursor.close();
        }
    }

    private static boolean isCursorListEmpty(List<Cursor> list) {
        int sum = 0;
        for (Cursor cursor : list) {
            sum += cursor.getCount();
        }
        return sum == 0;
    }

    private static boolean pushEnabled(int mode) {
        return (mode & DisplayMode.FLAG_PUSH) > 0;
    }

    private static boolean smsEnabled(int mode) {
        return (mode & DisplayMode.FLAG_SMS) > 0;
    }

    private static boolean groupsEnabled(int mode) {
        return (mode & DisplayMode.FLAG_GROUPS) > 0;
    }

}

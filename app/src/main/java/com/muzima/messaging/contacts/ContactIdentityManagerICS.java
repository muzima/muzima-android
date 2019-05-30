package com.muzima.messaging.contacts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.LinkedList;
import java.util.List;

public class ContactIdentityManagerICS extends ContactIdentityManager {

    public ContactIdentityManagerICS(Context context) {
        super(context);
    }

    @SuppressLint("NewApi")
    @Override
    public Uri getSelfIdentityUri() {
        String[] PROJECTION = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup._ID,
        };

        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI,
                    PROJECTION, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return ContactsContract.Contacts.getLookupUri(cursor.getLong(2), cursor.getString(1));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    @Override
    public boolean isSelfIdentityAutoDetected() {
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public List<Long> getSelfIdentityRawContactIds() {
        List<Long> results = new LinkedList<Long>();

        String[] PROJECTION = new String[] {
                ContactsContract.Profile._ID
        };

        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI,
                    PROJECTION, null, null, null);

            if (cursor == null || cursor.getCount() == 0)
                return null;

            while (cursor.moveToNext()) {
                results.add(cursor.getLong(0));
            }

            return results;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
}

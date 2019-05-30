package com.muzima.messaging.contacts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import com.muzima.messaging.TextSecurePreferences;

import java.util.ArrayList;
import java.util.List;

public class ContactIdentityManagerGingerbread extends ContactIdentityManager {

    public ContactIdentityManagerGingerbread(Context context) {
        super(context);
    }

    @Override
    public Uri getSelfIdentityUri() {
        String contactUriString = TextSecurePreferences.getIdentityContactUri(context);

        if      (hasLocalNumber())         return getContactUriForNumber(getLocalNumber());
        else if (contactUriString != null) return Uri.parse(contactUriString);

        return null;
    }

    @Override
    public boolean isSelfIdentityAutoDetected() {
        return hasLocalNumber() && getContactUriForNumber(getLocalNumber()) != null;
    }

    @Override
    public List<Long> getSelfIdentityRawContactIds() {
        long selfIdentityContactId = getSelfIdentityContactId();

        if (selfIdentityContactId == -1)
            return null;

        Cursor cursor                 = null;
        ArrayList<Long> rawContactIds = new ArrayList<Long>();

        try {
            cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
                    new String[] {ContactsContract.RawContacts._ID},
                    ContactsContract.RawContacts.CONTACT_ID + " = ?",
                    new String[] {selfIdentityContactId+""},
                    null);

            if (cursor == null || cursor.getCount() == 0)
                return null;

            while (cursor.moveToNext()) {
                rawContactIds.add(Long.valueOf(cursor.getLong(0)));
            }

            return rawContactIds;

        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private Uri getContactUriForNumber(String number) {
        String[] PROJECTION = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup._ID,
        };

        Uri uri       = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(uri, PROJECTION, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return ContactsContract.Contacts.getLookupUri(cursor.getLong(2), cursor.getString(1));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private long getSelfIdentityContactId() {
        Uri contactUri = getSelfIdentityUri();

        if (contactUri == null)
            return -1;

        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(contactUri,
                    new String[] {ContactsContract.Contacts._ID},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return -1;
            }

        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    @SuppressLint("MissingPermission")
    private String getLocalNumber() {
        return ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE))
                .getLine1Number();
    }

    private boolean hasLocalNumber() {
        String number = getLocalNumber();
        return (number != null) && (number.trim().length() > 0);
    }
}

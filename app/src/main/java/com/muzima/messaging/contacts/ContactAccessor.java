package com.muzima.messaging.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.GroupDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ContactAccessor {
    public static final String PUSH_COLUMN = "push";

        private static final ContactAccessor instance = new ContactAccessor();

        public static synchronized ContactAccessor getInstance() {
            return instance;
        }

        public Set<SignalAddress> getAllContactsWithNumbers(Context context) {
            Set<SignalAddress> results = new HashSet<>();

            try (Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER}, null ,null, null)) {
                while (cursor != null && cursor.moveToNext()) {
                    if (!TextUtils.isEmpty(cursor.getString(0))) {
                        results.add(SignalAddress.fromExternal(context, cursor.getString(0)));
                    }
                }
            }

            return results;
        }

        public Cursor getAllSystemContacts(Context context) {
            return context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[] {ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.LABEL, ContactsContract.CommonDataKinds.Phone.PHOTO_URI, ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY}, null, null, null);
        }

        public boolean isSystemContact(Context context, String number) {
            Uri uri        = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.LOOKUP_KEY,
                    ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER};
            Cursor   cursor     = context.getContentResolver().query(uri, projection, null, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    return true;
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            return false;
        }

        public Collection<ContactData> getContactsWithPush(Context context) {
            final ContentResolver resolver = context.getContentResolver();
            final String[] inProjection    = new String[]{ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME};

            final List<SignalAddress> registeredAddresses = DatabaseFactory.getRecipientDatabase(context).getRegistered();
            final Collection<ContactData> lookupData          = new ArrayList<>(registeredAddresses.size());

            for (SignalAddress registeredAddress : registeredAddresses) {
                Uri    uri          = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(registeredAddress.serialize()));
                Cursor lookupCursor = resolver.query(uri, inProjection, null, null, null);

                try {
                    if (lookupCursor != null && lookupCursor.moveToFirst()) {
                        final ContactData contactData = new ContactData(lookupCursor.getLong(0), lookupCursor.getString(1));
                        contactData.numbers.add(new NumberData("TextSecure", registeredAddress.serialize()));
                        lookupData.add(contactData);
                    }
                } finally {
                    if (lookupCursor != null)
                        lookupCursor.close();
                }
            }

            return lookupData;
        }

        public String getNameFromContact(Context context, Uri uri) {
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, new String[] {ContactsContract.Contacts.DISPLAY_NAME},
                        null, null, null);

                if (cursor != null && cursor.moveToFirst())
                    return cursor.getString(0);

            } finally {
                if (cursor != null)
                    cursor.close();
            }

            return null;
        }

        public ContactData getContactData(Context context, Uri uri) {
            return getContactData(context, getNameFromContact(context, uri),  Long.parseLong(uri.getLastPathSegment()));
        }

        private ContactData getContactData(Context context, String displayName, long id) {
            ContactData contactData = new ContactData(id, displayName);
            Cursor numberCursor     = null;

            try {
                numberCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[] {contactData.id + ""}, null);

                while (numberCursor != null && numberCursor.moveToNext()) {
                    int type         = numberCursor.getInt(numberCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                    String label     = numberCursor.getString(numberCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LABEL));
                    String number    = numberCursor.getString(numberCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String typeLabel = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), type, label).toString();

                    contactData.numbers.add(new NumberData(typeLabel, number));
                }
            } finally {
                if (numberCursor != null)
                    numberCursor.close();
            }

            return contactData;
        }

        public List<String> getNumbersForThreadSearchFilter(Context context, String constraint) {
            LinkedList<String> numberList = new LinkedList<>();
            Cursor cursor                 = null;

            try {
                cursor = context.getContentResolver().query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                        Uri.encode(constraint)),
                        null, null, null, null);

                while (cursor != null && cursor.moveToNext()) {
                    numberList.add(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                }

            } finally {
                if (cursor != null)
                    cursor.close();
            }

            GroupDatabase.Reader reader = null;
            GroupDatabase.GroupRecord record;

            try {
                reader = DatabaseFactory.getGroupDatabase(context).getGroupsFilteredByTitle(constraint);

                while ((record = reader.getNext()) != null) {
                    numberList.add(record.getEncodedId());
                }
            } finally {
                if (reader != null)
                    reader.close();
            }

            return numberList;
        }

        public CharSequence phoneTypeToString(Context mContext, int type, CharSequence label) {
            return ContactsContract.CommonDataKinds.Phone.getTypeLabel(mContext.getResources(), type, label);
        }

public static class NumberData implements Parcelable {

    public static final Creator<NumberData> CREATOR = new Creator<NumberData>() {
        public NumberData createFromParcel(Parcel in) {
            return new NumberData(in);
        }

        public NumberData[] newArray(int size) {
            return new NumberData[size];
        }
    };

    public final String number;
    public final String type;

    public NumberData(String type, String number) {
        this.type = type;
        this.number = number;
    }

    public NumberData(Parcel in) {
        number = in.readString();
        type   = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(number);
        dest.writeString(type);
    }
}

public static class ContactData implements Parcelable {

    public static final Creator<ContactData> CREATOR = new Creator<ContactData>() {
        public ContactData createFromParcel(Parcel in) {
            return new ContactData(in);
        }

        public ContactData[] newArray(int size) {
            return new ContactData[size];
        }
    };

    public final long id;
    public final String name;
    public final List<NumberData> numbers;

    public ContactData(long id, String name) {
        this.id      = id;
        this.name    = name;
        this.numbers = new LinkedList<NumberData>();
    }

    public ContactData(Parcel in) {
        id      = in.readLong();
        name    = in.readString();
        numbers = new LinkedList<NumberData>();
        in.readTypedList(numbers, NumberData.CREATOR);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeTypedList(numbers);
    }
}

    /***
     * If the code below looks shitty to you, that's because it was taken
     * directly from the Android source, where shitty code is all you get.
     */

    public Cursor getCursorForRecipientFilter(CharSequence constraint,
                                              ContentResolver mContentResolver)
    {
        final String SORT_ORDER = ContactsContract.Contacts.TIMES_CONTACTED + " DESC," +
                ContactsContract.Contacts.DISPLAY_NAME + "," +
                ContactsContract.Contacts.Data.IS_SUPER_PRIMARY + " DESC," +
                ContactsContract.CommonDataKinds.Phone.TYPE;

        final String[] PROJECTION_PHONE = {
                ContactsContract.CommonDataKinds.Phone._ID,                  // 0
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,           // 1
                ContactsContract.CommonDataKinds.Phone.TYPE,                 // 2
                ContactsContract.CommonDataKinds.Phone.NUMBER,               // 3
                ContactsContract.CommonDataKinds.Phone.LABEL,                // 4
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,         // 5
        };

        String phone = "";
        String cons  = null;

        if (constraint != null) {
            cons = constraint.toString();

            if (RecipientsAdapter.usefulAsDigits(cons)) {
                phone = PhoneNumberUtils.convertKeypadLettersToDigits(cons);
                if (phone.equals(cons) && !PhoneNumberUtils.isWellFormedSmsAddress(phone)) {
                    phone = "";
                } else {
                    phone = phone.trim();
                }
            }
        }
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(cons));
        String selection = String.format("%s=%s OR %s=%s OR %s=%s",
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MMS);

        Cursor phoneCursor = mContentResolver.query(uri,
                PROJECTION_PHONE,
                null,
                null,
                SORT_ORDER);

        if (phone.length() > 0) {
            ArrayList result = new ArrayList();
            result.add(Integer.valueOf(-1));                    // ID
            result.add(Long.valueOf(-1));                       // CONTACT_ID
            result.add(Integer.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM));     // TYPE
            result.add(phone);                                  // NUMBER

            /*
             * The "\u00A0" keeps Phone.getDisplayLabel() from deciding
             * to display the default label ("Home") next to the transformation
             * of the letters into numbers.
             */
            result.add("\u00A0");                               // LABEL
            result.add(cons);                                   // NAME

            ArrayList<ArrayList> wrap = new ArrayList<ArrayList>();
            wrap.add(result);

            ArrayListCursor translated = new ArrayListCursor(PROJECTION_PHONE, wrap);

            return new MergeCursor(new Cursor[] { translated, phoneCursor });
        } else {
            return phoneCursor;
        }
    }
}

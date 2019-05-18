package com.muzima.messaging.sqlite.database;

import android.content.Context;
import android.support.annotation.NonNull;

import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.crypto.AttachmentSecret;
import com.muzima.messaging.crypto.AttachmentSecretProvider;
import com.muzima.messaging.crypto.DatabaseSecret;
import com.muzima.messaging.crypto.DatabaseSecretProvider;
import com.muzima.messaging.crypto.MasterSecret;
import com.muzima.messaging.sqlite.database.helpers.SQLCipherOpenHelper;

import net.sqlcipher.database.SQLiteDatabase;

public class DatabaseFactory {
    private static final Object lock = new Object();

    private static DatabaseFactory instance;

    private final SQLCipherOpenHelper databaseHelper;
    private final SmsDatabase sms;
    private final MmsDatabase mms;
    private final AttachmentDatabase attachments;
    private final MediaDatabase media;
    private final ThreadDatabase thread;
    private final MmsSmsDatabase mmsSmsDatabase;
    private final IdentityDatabase identityDatabase;
    private final DraftDatabase draftDatabase;
    private final PushDatabase pushDatabase;
    private final GroupDatabase groupDatabase;
    private final RecipientDatabase recipientDatabase;
    private final ContactsDatabase contactsDatabase;
    private final GroupReceiptDatabase groupReceiptDatabase;
    private final OneTimePreKeyDatabase preKeyDatabase;
    private final SignedPreKeyDatabase signedPreKeyDatabase;
    private final SessionDatabase sessionDatabase;
    private final SearchDatabase searchDatabase;

    public static DatabaseFactory getInstance(Context context) {
        synchronized (lock) {
            if (instance == null)
                instance = new DatabaseFactory(context.getApplicationContext());

            return instance;
        }
    }

    public static MmsSmsDatabase getMmsSmsDatabase(Context context) {
        return getInstance(context).mmsSmsDatabase;
    }

    public static ThreadDatabase getThreadDatabase(Context context) {
        return getInstance(context).thread;
    }

    public static SmsDatabase getSmsDatabase(Context context) {
        return getInstance(context).sms;
    }

    public static MmsDatabase getMmsDatabase(Context context) {
        return getInstance(context).mms;
    }

    public static AttachmentDatabase getAttachmentDatabase(Context context) {
        return getInstance(context).attachments;
    }

    public static MediaDatabase getMediaDatabase(Context context) {
        return getInstance(context).media;
    }

    public static IdentityDatabase getIdentityDatabase(Context context) {
        return getInstance(context).identityDatabase;
    }

    public static DraftDatabase getDraftDatabase(Context context) {
        return getInstance(context).draftDatabase;
    }

    public static PushDatabase getPushDatabase(Context context) {
        return getInstance(context).pushDatabase;
    }

    public static GroupDatabase getGroupDatabase(Context context) {
        return getInstance(context).groupDatabase;
    }

    public static RecipientDatabase getRecipientDatabase(Context context) {
        return getInstance(context).recipientDatabase;
    }

    public static ContactsDatabase getContactsDatabase(Context context) {
        return getInstance(context).contactsDatabase;
    }

    public static GroupReceiptDatabase getGroupReceiptDatabase(Context context) {
        return getInstance(context).groupReceiptDatabase;
    }

    public static OneTimePreKeyDatabase getPreKeyDatabase(Context context) {
        return getInstance(context).preKeyDatabase;
    }

    public static SignedPreKeyDatabase getSignedPreKeyDatabase(Context context) {
        return getInstance(context).signedPreKeyDatabase;
    }

    public static SessionDatabase getSessionDatabase(Context context) {
        return getInstance(context).sessionDatabase;
    }

    public static SearchDatabase getSearchDatabase(Context context) {
        return getInstance(context).searchDatabase;
    }

    public static SQLiteDatabase getBackupDatabase(Context context) {
        return getInstance(context).databaseHelper.getReadableDatabase();
    }

    public static void upgradeRestored(Context context, SQLiteDatabase database) {
        getInstance(context).databaseHelper.onUpgrade(database, database.getVersion(), -1);
        getInstance(context).databaseHelper.markCurrent(database);
    }

    private DatabaseFactory(@NonNull Context context) {
        SQLiteDatabase.loadLibs(context);

        DatabaseSecret databaseSecret = new DatabaseSecretProvider(context).getOrCreateDatabaseSecret();
        AttachmentSecret attachmentSecret = AttachmentSecretProvider.getInstance(context).getOrCreateAttachmentSecret();

        this.databaseHelper = new SQLCipherOpenHelper(context, databaseSecret);
        this.sms = new SmsDatabase(context, databaseHelper);
        this.mms = new MmsDatabase(context, databaseHelper);
        this.attachments = new AttachmentDatabase(context, databaseHelper, attachmentSecret);
        this.media = new MediaDatabase(context, databaseHelper);
        this.thread = new ThreadDatabase(context, databaseHelper);
        this.mmsSmsDatabase = new MmsSmsDatabase(context, databaseHelper);
        this.identityDatabase = new IdentityDatabase(context, databaseHelper);
        this.draftDatabase = new DraftDatabase(context, databaseHelper);
        this.pushDatabase = new PushDatabase(context, databaseHelper);
        this.groupDatabase = new GroupDatabase(context, databaseHelper);
        this.recipientDatabase = new RecipientDatabase(context, databaseHelper);
        this.groupReceiptDatabase = new GroupReceiptDatabase(context, databaseHelper);
        this.contactsDatabase = new ContactsDatabase(context);
        this.preKeyDatabase = new OneTimePreKeyDatabase(context, databaseHelper);
        this.signedPreKeyDatabase = new SignedPreKeyDatabase(context, databaseHelper);
        this.sessionDatabase = new SessionDatabase(context, databaseHelper);
        this.searchDatabase = new SearchDatabase(context, databaseHelper);
    }

//    public void onApplicationLevelUpgrade(@NonNull Context context, @NonNull MasterSecret masterSecret,
//                                          int fromVersion, DatabaseUpgradeActivity.DatabaseUpgradeListener listener) {
//        databaseHelper.getWritableDatabase();
//
//        ClassicOpenHelper legacyOpenHelper = null;
//
//        if (fromVersion < DatabaseUpgradeActivity.ASYMMETRIC_MASTER_SECRET_FIX_VERSION) {
//            legacyOpenHelper = new ClassicOpenHelper(context);
//            legacyOpenHelper.onApplicationLevelUpgrade(context, masterSecret, fromVersion, listener);
//        }
//
//        if (fromVersion < DatabaseUpgradeActivity.SQLCIPHER && TextSecurePreferences.getNeedsSqlCipherMigration(context)) {
//            if (legacyOpenHelper == null) {
//                legacyOpenHelper = new ClassicOpenHelper(context);
//            }
//
//            SQLCipherMigrationHelper.migrateCiphertext(context, masterSecret,
//                    legacyOpenHelper.getWritableDatabase(),
//                    databaseHelper.getWritableDatabase(),
//                    listener);
//        }
//    }
}

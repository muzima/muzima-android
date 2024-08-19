package com.muzima.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.muzima.db.daos.CredentialDao;
import com.muzima.db.daos.UserDao;
import com.muzima.db.entities.Credential;
import com.muzima.db.entities.Person;
import com.muzima.db.entities.PersonName;
import com.muzima.db.entities.User;

import net.sqlcipher.database.SupportFactory;

@Database(entities = {Credential.class, User.class, Person.class, PersonName.class,}, version = 1, exportSchema = false)
public abstract class MuzimaDatabase  extends RoomDatabase {

    private static volatile MuzimaDatabase INSTANCE;
    public abstract CredentialDao credentialDao();
    public abstract UserDao userDao();

    public static MuzimaDatabase getDatabase(final Context context, byte[] passphrase) {
        if (INSTANCE == null) {
            synchronized (MuzimaDatabase.class) {
                SupportSQLiteOpenHelper.Factory factory = new SupportFactory(passphrase);
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    MuzimaDatabase.class, "muzima.db")
                            .openHelperFactory(factory)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public void clearAllTables() {}

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(@NonNull DatabaseConfiguration databaseConfiguration) {
        return null;
    }
}
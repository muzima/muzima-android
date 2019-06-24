package com.muzima.messaging.jobmanager.persistence;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.muzima.messaging.jobmanager.EncryptionKeys;
import com.muzima.messaging.jobmanager.Job;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PersistentStorage {
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "queue";
    private static final String ID = "_id";
    private static final String ITEM = "item";
    private static final String ENCRYPTED = "encrypted";

    private static final String DATABASE_CREATE = String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, %s INTEGER DEFAULT 0);",
            TABLE_NAME, ID, ITEM, ENCRYPTED);

    private final DatabaseHelper databaseHelper;
    private final JobSerializer  jobSerializer;

    public PersistentStorage(Context context, String name, JobSerializer serializer) {
        this.databaseHelper = new DatabaseHelper(context, "_jobqueue-" + name);
        this.jobSerializer = serializer;
    }

    public List<Job> getAllUnencrypted() {
        return getJobs(null, ENCRYPTED + " = 0");
    }

    private List<Job> getJobs(EncryptionKeys keys, String where) {
        List<Job> results = new LinkedList<>();
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, null, where, null, null, null, ID + " ASC", null);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
                String item = cursor.getString(cursor.getColumnIndexOrThrow(ITEM));
                boolean encrypted = cursor.getInt(cursor.getColumnIndexOrThrow(ENCRYPTED)) == 1;

                try{
                    Job job = jobSerializer.deserialize(keys, encrypted, item);
                    results.add(job);
                } catch (IOException e) {
                    Log.w("PersistentStore", e);
                    remove(id);
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return results;
    }

    public void remove(long id) {
        databaseHelper.getWritableDatabase()
                .delete(TABLE_NAME, ID + " = ?", new String[] {String.valueOf(id)});
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context, String name) {
            super(context, name, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}

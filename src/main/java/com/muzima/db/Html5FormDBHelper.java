package com.muzima.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.muzima.db.FormContract.Html5FormEntry;

public class Html5FormDBHelper extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "MuzimaHtml5Form.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Html5FormEntry.TABLE_NAME + " (" +
                    Html5FormEntry._ID + " INTEGER PRIMARY KEY," +
                    Html5FormEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + COMMA_SEP +
                    Html5FormEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    Html5FormEntry.COLUMN_NAME_DESCRIPTION + TEXT_TYPE +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Html5FormEntry.TABLE_NAME;

    public Html5FormDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(getClass().getName(),
                "Upgrading database: "+ DATABASE_NAME + " from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}

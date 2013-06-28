package com.muzima.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.muzima.domain.Html5Form;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.db.FormContract.Html5FormEntry;
import static com.muzima.utils.StringUtils.getCommaSeparatedStringFromList;
import static com.muzima.utils.StringUtils.getListFromCommaSeparatedString;

public class Html5FormDataSource {
    private SQLiteDatabase database;
    private Html5FormDBHelper dbHelper;
    private String[] allColumns = {Html5FormEntry.COLUMN_NAME_ENTRY_ID,
            Html5FormEntry.COLUMN_NAME_NAME,
            Html5FormEntry.COLUMN_NAME_DESCRIPTION,
            Html5FormEntry.COLUMN_NAME_TAGS};

    public Html5FormDataSource(Html5FormDBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public List<Html5Form> getAllForms() {
        List<Html5Form> forms = new ArrayList<Html5Form>();

        Cursor cursor = database.query(Html5FormEntry.TABLE_NAME,
                allColumns, null, null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                Html5Form form = getFormFromCursor(cursor);
                forms.add(form);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return forms;
    }

    public void saveForms(List<Html5Form> forms){
        for (Html5Form form : forms) {
            saveForm(form);
        }
    }

    public boolean hasForms(){
        long numOfForms = DatabaseUtils.queryNumEntries(database, Html5FormEntry.TABLE_NAME);
        Log.w("DEBUG", "numOfForms" + numOfForms);
        return numOfForms != 0;
    }

    public void saveForm(Html5Form form) {
        ContentValues values = new ContentValues();
        values.put(Html5FormEntry.COLUMN_NAME_ENTRY_ID, form.getId());
        values.put(Html5FormEntry.COLUMN_NAME_NAME, form.getName());
        values.put(Html5FormEntry.COLUMN_NAME_DESCRIPTION, form.getDescription());
        values.put(Html5FormEntry.COLUMN_NAME_TAGS, getCommaSeparatedStringFromList(form.getTags()));
        database.insert(Html5FormEntry.TABLE_NAME, null,
                values);

    }

    public void deleteAllForms(){
        database.delete(Html5FormEntry.TABLE_NAME, null, null);
    }

    private Html5Form getFormFromCursor(Cursor cursor) {
        return new Html5Form(cursor.getString(0), cursor.getString(1), cursor.getString(2), getListFromCommaSeparatedString(cursor.getString(3)));
    }
}

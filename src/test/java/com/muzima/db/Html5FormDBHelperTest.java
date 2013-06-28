package com.muzima.db;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.muzima.testSupport.CustomTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(CustomTestRunner.class)
public class Html5FormDBHelperTest {

    private Html5FormDBHelper html5FormDBHelper;
    private SQLiteDatabase sqLiteDatabase;

    @Before
    public void setUp() throws Exception {
        sqLiteDatabase = mock(SQLiteDatabase.class);
        Context context  = new Activity();
        html5FormDBHelper = new Html5FormDBHelper(context);
    }

    @Test
    public void onCreate_shouldExecCreateQuery(){
        html5FormDBHelper.onCreate(sqLiteDatabase);
        verify(sqLiteDatabase).execSQL(Html5FormDBHelper.SQL_CREATE_ENTRIES);
    }

    @Test
    public void onUpgrade_shouldDeleteAndRecreateTable(){
        html5FormDBHelper.onUpgrade(sqLiteDatabase, 1, 2);

        InOrder inOrder = inOrder(sqLiteDatabase);
        inOrder.verify(sqLiteDatabase).execSQL(Html5FormDBHelper.SQL_DELETE_ENTRIES);
        inOrder.verify(sqLiteDatabase).execSQL(Html5FormDBHelper.SQL_CREATE_ENTRIES);
    }
}

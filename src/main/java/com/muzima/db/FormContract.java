package com.muzima.db;

import android.provider.BaseColumns;

public class FormContract {
    public static abstract class Html5FormEntry implements BaseColumns {
        public static final String TABLE_NAME = "html5form";
        public static final String COLUMN_NAME_ENTRY_ID = "id";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "description";
    }
}

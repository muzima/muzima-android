package com.muzima.utils;

public class Constants {
    public static final String SYNC_PREF = "SyncMetaData";
    public static final String COHORT_PREFIX_PREF = "CohortPrefixPref";
    public static final String COHORT_PREFIX_PREF_KEY = "CohortPrefixPrefKey";
    public static final String FORM_TAG_PREF = "FormTagPref";
    public static final String FORM_TAG_PREF_KEY = "FormTagPrefKey";
    public static final String CONCEPT_PREF = "ConceptPref";
    public static final String CONCEPT_PREF_KEY = "ConceptPrefKey";
    public static final String STATUS_INCOMPLETE = "incomplete";
    public static final String STATUS_COMPLETE = "complete";

    public static class DataSyncServiceConstants {
        public static final String SYNC_TYPE = "sync_type";
        public static final String CREDENTIALS = "credentials";
        public static final String SYNC_STATUS = "sync_status";
        public static final String DOWNLOAD_COUNT = "donwload_count";
        public static final String FROM_IDS = "formIds";

        public static final int SYNC_FORMS = 0;
        public static final int SYNC_TEMPLATES = 1;

        public static class SyncStatusConstants {
            public static final int DOWNLOAD_ERROR = 0;
            public static final int SAVE_ERROR = 1;
            public static final int AUTHENTICATION_ERROR = 2;
            public static final int DELETE_ERROR = 3;
            public static final int SUCCESS = 4;
            public static final int CANCELLED = 5;
            public static final int CONNECTION_ERROR = 6;
            public static final int PARSING_ERROR = 7;
            public static final int AUTHENTICATION_SUCCESS = 8;
            public static final int REPLACE_ERROR = 9;
            public static final int UNKNOWN_ERROR = 10;
        }
    }
}

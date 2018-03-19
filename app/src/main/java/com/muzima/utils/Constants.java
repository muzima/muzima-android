/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.os.Environment;

//TODO: This class should be burnt and flushed. Constants in an anti-pattern and and a sure sign that your abstractions are wrong: Zabil
//TODO: Burnt?? Maybe or may not be - Prasanna
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
    public static final String STATUS_UPLOADED = "uploaded";
    public static final String SEARCH_STRING_BUNDLE_KEY = "SearchString";
    public static final String LOCAL_PATIENT = "LocalPatient";

    public static final String FORM_XML_DISCRIMINATOR_ENCOUNTER = "xml-encounter";
    public static final String FORM_JSON_DISCRIMINATOR_ENCOUNTER = "json-encounter";
    public static final String FORM_DISCRIMINATOR_REGISTRATION = "xml-registration";
    public static final String FORM_JSON_DISCRIMINATOR_REGISTRATION = "json-registration";
    public static final String FORM_JSON_DISCRIMINATOR_GENERIC_REGISTRATION = "json-generic-registration";
    public static final String FORM_JSON_DISCRIMINATOR_CONSULTATION = "json-consultation";
    public static final String FORM_DISCRIMINATOR_CONSULTATION = "consultation";
    public static final String FORM_JSON_DISCRIMINATOR_DEMOGRAPHICS_UPDATE = "json-demographics-update";

    private static final String APP_EXTERNAL_DIR_ROOT =  Environment.getExternalStorageDirectory().getPath() + "/muzima";
    public static final String APP_MEDIA_DIR = APP_EXTERNAL_DIR_ROOT + "/media";
    public static final String APP_IMAGE_DIR = APP_MEDIA_DIR + "/image";
    public static final String APP_AUDIO_DIR = APP_MEDIA_DIR + "/audio";
    public static final String APP_VIDEO_DIR = APP_MEDIA_DIR + "/video";
    public static final String TMP_FILE_PATH = APP_EXTERNAL_DIR_ROOT + "/.cache";

    public static final int PATIENT_LOAD_PAGE_SIZE = 10;

    public static class DataSyncServiceConstants {
        public static final String SYNC_TYPE = "sync_type";
        public static final String CREDENTIALS = "credentials";
        public static final String SYNC_STATUS = "sync_status";
        public static final String DOWNLOAD_COUNT_PRIMARY = "download_count_primary";
        public static final String DOWNLOAD_COUNT_SECONDARY = "download_count_secondary";
        public static final String DELETED_COUNT_PRIMARY = "deleted_count_primary";
        public static final String FORM_IDS = "formIds";
        public static final String COHORT_IDS = "cohortIds";
        public static final String PATIENT_UUID_FOR_DOWNLOAD = "patientUUIDForDownload";

        public static final int SYNC_FORMS = 0;
        public static final int SYNC_TEMPLATES = 1;
        public static final int SYNC_COHORTS = 2;
        public static final int SYNC_PATIENTS_FULL_DATA = 3;
        public static final int SYNC_OBSERVATIONS = 4;
        public static final int SYNC_ENCOUNTERS = 5;
        public static final int SYNC_PATIENTS_ONLY = 6;
        public static final int SYNC_PATIENTS_DATA_ONLY = 7;
        public static final int SYNC_UPLOAD_FORMS = 8;
        public static final int DOWNLOAD_PATIENT_ONLY = 9;
        public static final int SYNC_NOTIFICATIONS = 10;
        public static final int SYNC_REAL_TIME_UPLOAD_FORMS =11;

        public static class SyncStatusConstants {
            public static final int DOWNLOAD_ERROR = 0;
            public static final int SAVE_ERROR = 1;
            public static final int AUTHENTICATION_ERROR = 2;
            public static final int DELETE_ERROR = 3;
            public static final int SUCCESS = 4;
            public static final int CANCELLED = 5;
            public static final int LOCAL_CONNECTION_ERROR = 6;
            public static final int SERVER_CONNECTION_ERROR = 7;
            public static final int PARSING_ERROR = 8;
            public static final int AUTHENTICATION_SUCCESS = 9;
            public static final int REPLACE_ERROR = 10;
            public static final int LOAD_ERROR = 11;
            public static final int UNKNOWN_ERROR = 12;
            public static final int UPLOAD_ERROR = 13;
            public static final int MALFORMED_URL_ERROR = 14;
            public static final int INVALID_CREDENTIALS_ERROR = 15;
            public static final int INVALID_CHARACTER_IN_USERNAME = 16;
            public static final String INVALID_CHARACTER_FOR_USERNAME = ",;.-/@#$%&*+='\"|~`<>";
        }
    }

    public static class NotificationStatusConstants {
        public static final String NOTIFICATION_READ = "read";
        public static final String NOTIFICATION_UNREAD = "unread";
        public static final String RECEIVER_UUID = "receiverUuid";
    }

    public static class ProgressDialogConstants {
        public static final String PROGRESS_UPDATE_MESSAGE = "progressUpdateMessage";
        public static final String PROGRESS_UPDATE_ACTION = "progressUpdateAction";
    }

    public enum SERVER_CONNECTIVITY_STATUS {
        SERVER_ONLINE,SERVER_OFFLINE, INTERNET_FAILURE
    }

    public static class SetupLogConstants{
        public static final String ACTION_SUCCESS_STATUS_LOG = "OK";
        public static final String ACTION_FAILURE_STATUS_LOG = "FAIL";
    }

    public static class Shr{
        public static class KenyaEmr{
            public static class IdentifierType{
                public static class CARD_SERIAL_NUMBER{
                    public static final String name = "SMART CARD SERIAL NUMBER";
                    public static final String uuid = "8f842498-1c5b-11e8-accf-0ed5f89f718b";
                    public static final String shr_name = "CARD_SERIAL_NUMBER";
                }
                public static class CCC_NO{
                    public static final String name = "UNIQUE PATIENT NUMBER/CCC No";
                    public static final String uuid = "05ee9cf4-7242-4a17-b4d4-00f707265c8a";
                }
                public static class GODS_NUMBER{
                    public static final String name = "GODS NUMBER";
                    public static final String uuid = "9aedb9ae-1cbd-11e8-accf-0ed5f89f718b";
                }
                public static class HEI_UNIQUE_NUMBER{
                    public static final String name = "HEI UNIQUE NUMBER";
                    public static final String uuid = "0691f522-dd67-4eeb-92c8-af5083baf338";
                }
                public static class HTS_NUMBER{
                    public static final String name = "HTS NUMBER";
                    public static final String uuid = "e6af3782-1cb3-11e8-accf-0ed5f89f718b";
                }
                public static class NATIONAL_ID{
                    public static final String name = "NATIONAL ID";
                    public static final String uuid = "49af6cdc-7968-4abb-bf46-de10d7f4859f";
                }
            }

            public static class Concept{
                public static class ANC_NUMBER{
                    public static final String name = "ANC NUMBER";
                    public static final String uuid = "161655AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
                }
            }
        }
    }

}

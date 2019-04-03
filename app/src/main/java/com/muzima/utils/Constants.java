/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import android.os.Environment;

import com.muzima.BuildConfig;

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
    public static final String FORM_JSON_DISCRIMINATOR_SHR_ENCOUNTER = "json-SHR-encounter";
    public static final String FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION = "json-SHR-registration";
    public static final String FORM_JSON_DISCRIMINATOR_SHR_DEMOGRAPHICS_UPDATE = "json-SHR-demographics-update";

    public static final String FORM_DISCRIMINATOR_PROVIDER_REPORT = "provider-report";
    public static final String FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS = "json-individual-obs";

    private static final String APP_EXTERNAL_DIR_ROOT =  Environment.getExternalStorageDirectory().getPath() + "/muzima";
    private static final String APP_MEDIA_DIR = APP_EXTERNAL_DIR_ROOT + "/media";
    public static final String APP_IMAGE_DIR = APP_MEDIA_DIR + "/image";
    public static final String APP_AUDIO_DIR = APP_MEDIA_DIR + "/audio";
    public static final String APP_VIDEO_DIR = APP_MEDIA_DIR + "/video";
    public static final String TMP_FILE_PATH = APP_EXTERNAL_DIR_ROOT + "/.cache";

    public static final int PATIENT_LOAD_PAGE_SIZE = 10;

    public static final String STANDARD_DATE_FORMAT = "dd-MM-yyyy";
    public static final String STANDARD_DATE_LOCALE_FORMAT = "dd-MM-yyyy hh:mm";

    public static class MuzimaGPSLocationConstants {
        public static final int LOCATION_ACCESS_PERMISSION_REQUEST_CODE = 9111;
        public static final int LOCATION_SERVICES_SWITCH_REQUEST_CODE = 9122;
    }

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
        public static final int SYNC_SHR =12;

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

        public static class MuzimaJobSchedularConstants {
            public static final String MUZIMA_JOB_SCHEDULE_INTENT = BuildConfig.APPLICATION_ID+" muzima scheduled job";
            public static final String WORK_DURATION_KEY = "work duration";
            public static final int MESSAGE_SYNC_JOB_ID = 22;
            public static final int MSG_INDICATOR_START = 18;
            public static final int MSG_INDICATOR_STOP = 19;
            public static final int MSG_COLOR_START = 20;
            public static final int JOB_INDICATOR_STOP = 21;
            public static final long MUZIMA_JOB_PERIODIC = 5000;
        }
    }

    public static class NotificationStatusConstants {
        public static final String NOTIFICATION_READ = "read";
        public static final String NOTIFICATION_UNREAD = "unread";
        public static final String RECEIVER_UUID = "receiverUuid";
        public static final String NOTIFICATION_UPLOADED = "uploaded";
        public static final String NOTIFICATION_NOT_UPLOADED = "notUploaded";
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

    //This is a hack. Better to use configuration file from server side to obtain SHR definition
    public static class Shr {

        public static class KenyaEmr{

            public static final String SMART_CARD_RECORD_TYPE = "KenyaEmrSHR";
            public static class DEFAULT_SHR_USER{
                public static final String id = "SHR_user";
            }
            public static class DEFAULT_SHR_FACILITY {
                public static final String MFL_CODE = "10829";
            }

            public static class LocationAttributeType{
                public static class MASTER_FACILITY_CODE {
                    public static final String name = "Master Facility Code";
                    public static final String uuid = "4c4b11f6-44b0-4345-816b-bddfa093c583";
                }
            }

            public static class PersonIdentifierType {
                public static class CARD_SERIAL_NUMBER{
                    public static final String name = "SMART CARD SERIAL NUMBER";
                    public static final String uuid = "8f842498-1c5b-11e8-accf-0ed5f89f718b";
                    public static final String shr_name = "CARD_SERIAL_NUMBER";
                }
                public static class CCC_NUMBER{
                    public static final String name = "UNIQUE PATIENT NUMBER/CCC No";
                    public static final String uuid = "05ee9cf4-7242-4a17-b4d4-00f707265c8a";
                    public static final String shr_name = "CCC_NUMBER";
                }
                public static class GODS_NUMBER{
                    public static final String name = "GODS NUMBER";
                    public static final String uuid = "9aedb9ae-1cbd-11e8-accf-0ed5f89f718b";
                    public static final String shr_name = "GODS_NUMBER";
                }
                public static class HEI_NUMBER{
                    public static final String name = "HEI UNIQUE NUMBER";
                    public static final String uuid = "0691f522-dd67-4eeb-92c8-af5083baf338";
                    public static final String shr_name = "HEI_NUMBER";
                }
                public static class HTS_NUMBER{
                    public static final String name = "HTS NUMBER";
                    public static final String uuid = "e6af3782-1cb3-11e8-accf-0ed5f89f718b";
                    public static final String shr_name = "HTS_NUMBER";
                }
                public static class NATIONAL_ID{
                    public static final String name = "NATIONAL ID";
                    public static final String uuid = "49af6cdc-7968-4abb-bf46-de10d7f4859f";
                    public static final String shr_name = "NATIONAL_ID";
                }
            }

            public static class CONCEPTS {
                public static class ANC_NUMBER {
                    public static final String name = "ANC NUMBER";
                    public static final String uuid = "161655AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
                    public static final String shr_name = "ANC_NUMBER";
                }

                public static class HIV_TESTS {
                    public static class TEST_RESULT {
                        public static final String name = "FINAL HIV RESULTS";
                        public static final int concept_id = 159427;
                        public static final String type = "Coded";

                        public static class ANSWERS {
                            public static class POSITIVE {
                                public static final String name = "POSITIVE";
                                public static final int concept_id = 703;
                            }

                            public static class NEGATIVE {
                                public static final String name = "NEGATIVE";
                                public static final int concept_id = 664;
                            }

                            public static class INCONCLUSIVE {
                                public static final String name = "INCONCLUSIVE";
                                public static final int concept_id = 1138;
                            }
                        }
                    }

                    public static class TEST_TYPE {
                        public static final String name = "TYPE OF TEST";
                        public static final int concept_id = 162084;
                        public static final String type = "Coded";

                        public static class ANSWERS {
                            public static class SCREENING {
                                public static final String name = "SCREENING";
                                public static final int concept_id = 162080;
                            }

                            public static class CONFIRMATORY {
                                public static final String name = "CONFIRMATORY";
                                public static final int concept_id = 162082;
                            }
                        }
                    }

                    public static class TEST_STRATEGY {
                        public static final String name = "HIV TESTING SERVICES STRATEGY";
                        public static final int concept_id = 164956;
                        public static final String type = "Coded";

                        public static class ANSWERS {
                            public static class HP {
                                public static final String name = "HP";
                                public static final int concept_id = 164163;
                            }

                            public static class NP {
                                public static final String name = "NP";
                                public static final int concept_id = 164953;
                            }

                            public static class VI {
                                public static final String name = "VI";
                                public static final int concept_id = 164954;
                            }

                            public static class VS {
                                public static final String name = "VS";
                                public static final int concept_id = 164955;
                            }

                            public static class HB {
                                public static final String name = "HB";
                                public static final int concept_id = 159938;
                            }

                            public static class MO {
                                public static final String name = "MO";
                                public static final int concept_id = 159939;
                            }
                        }
                    }

                    public static class TEST_FACILITY {
                        public static final String name = "TEST FACILITY";
                        public static final int concept_id = 162724;
                        public static final String type = "Text";
                    }

                    public static class PROVIDER_DETAILS {
                        public static class NAME {
                            public static final String name = "NAME";
                            public static final int concept_id = 1473;
                            public static final String type = "Text";
                        }

                        public static class ID {
                            public static final String name = "ID";
                            public static final int concept_id = 163161;
                            public static final String type = "Text";
                        }
                    }

                    public static class ENCOUNTER {
                        public static final String ENCOUNTER_TYPE_UUID = "9bc15e94-2794-11e8-b467-0ed5f89f718b";
                    }

                    public static class FORM {
                        public static final String FORM_UUID = "9bc157d2-2794-11e8-b467-0ed5f89f718b";
                    }
                }
                public static class IMMUNIZATION {
                    public static class GROUP {
                        public static final String name = "IMMUNIZATION";
                        public static final int concept_id = 1421;
                    }
                    public static class VACCINE {
                        public static final String name = "VACCINE";
                        public static final int concept_id = 984;
                        public static class ANSWERS {
                            public static class BCG {
                                public static final String name = "BCG";
                                public static final int concept_id = 886;
                            }
                            public static class OPV_AT_BIRTH {
                                public static final String name = "OPV_AT_BIRTH";
                                public static final int concept_id = 783;
                                public static final int sequence = 0;
                            }
                            public static class OPV1 {
                                public static final String name = "OPV1";
                                public static final int concept_id = 783;
                                public static final int sequence = 1;
                            }
                            public static class OPV2 {
                                public static final String name = "OPV2";
                                public static final int concept_id = 783;
                                public static final int sequence = 2;
                            }
                            public static class OPV3 {
                                public static final String name = "OPV3";
                                public static final int concept_id = 783;
                                public static final int sequence = 3;
                            }
                            public static class IPV {
                                public static final String name = "IPV";
                                public static final int concept_id = 1422;
                                public static final int sequence = 1;
                            }
                            public static class PCV10_1 {
                                public static final String name = "PCV10-1";
                                public static final int concept_id = 162342;
                                public static final int sequence = 1;
                            }
                            public static class PCV10_2 {
                                public static final String name = "PCV10-2";
                                public static final int concept_id = 162342;
                                public static final int sequence = 2;
                            }
                            public static class PCV10_3 {
                                public static final String name = "PCV10-3";
                                public static final int concept_id = 162342;
                                public static final int sequence = 3;
                            }
                            public static class ROTA1 {
                                public static final String name = "ROTA1";
                                public static final int concept_id = 83531;
                                public static final int sequence = 1;
                            }
                            public static class ROTA2 {
                                public static final String name = "ROTA2";
                                public static final int concept_id = 83531;
                                public static final int sequence = 2;
                            }
                            public static class MEASLES6 {
                                public static final String name = "MEASLES6";
                                public static final int concept_id = 36;
                                public static final int sequence = 1;
                            }
                            public static class MEASLES9 {
                                public static final String name = "MEASLES9";
                                public static final int concept_id = 162586;
                                public static final int sequence = 1;
                            }
                            public static class MEASLES18 {
                                public static final String name = "MEASLES18";
                                public static final int concept_id = 162586;
                                public static final int sequence = 2;
                            }
                            public static class PENTA1 {
                                public static final String name = "PENTA1";
                                public static final int concept_id = 159694;
                                public static final int sequence = 1;
                            }
                            public static class PENTA2 {
                                public static final String name = "PENTA2";
                                public static final int concept_id = 159694;
                                public static final int sequence = 2;
                            }
                            public static class PENTA3 {
                                public static final String name = "PENTA3";
                                public static final int concept_id = 159694;
                                public static final int sequence = 3;
                            }
                        }
                    }

                    public static class SEQUENCE {
                        public static final String name = "SEQUENCE";
                        public static final int concept_id = 1418;
                    }

                    static class ENCOUNTER {
                        public static final String ENCOUNTER_TYPE_UUID = "9bc15e94-2794-11e8-b467-0ed5f89f718b";
                    }

                    public static class FORM {
                        public static final String FORM_UUID = "9bc157d2-2794-11e8-b467-0ed5f89f718b";
                    }
                }
            }

            public static class REGISTRATION {
                public static class FORM {
                    public static final String FORM_UUID = "d84e4db1-149b-43a6-bb9a-d66317b5aceb";
                }
            }
        }
    }

}

/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.preferences.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Location;
import com.muzima.controller.LocationController;
import com.muzima.service.FormDuplicateCheckPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.tasks.ValidateURLTask;
import com.muzima.util.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.barcode.BarcodeCaptureActivity;
import com.muzima.view.preferences.SettingsActivity;

import org.apache.commons.lang.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//ToDO: android.preference.PreferenceFragment was depreciated in API 29 - the current target SDK version. There's need to migrate to androidx.preference
public class SettingsPreferenceFragment extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Integer SESSION_TIMEOUT_MINIMUM = 0;
    private static final Integer SESSION_TIMEOUT_MAXIMUM = 500;
    private static final Integer SESSION_TIMEOUT_INVALID_VALUE = -1;

    private static final Integer AUTO_SAVE_INTERVAL_MINIMUM = 0;
    private static final Integer AUTO_SAVE_INTERVAL_MAXIMUM = 120;
    private static final Integer AUTO_SAVE_INTERVAL_INVALID_VALUE = -1;

    private SwitchPreference encounterProviderPreference;
    private SwitchPreference duplicateFormCheckCheckBoxPreference;
    private Activity mActivity;
    private static final int RC_BARCODE_CAPTURE = 9001;


    private String newURL;
    private EditTextPreference serverPreferences;
    private final Map<String, SettingsPreferenceFragment.PreferenceChangeHandler> actions = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        setUpServerPreference();
        setUpUsernamePreference();
        setUpPasswordPreference();
        setUpTimeoutPreference();
        setUpAutosaveIntervalPreference();
        setUpDuplicateFormDataWarningPreference();
        setUpFontSizePreference();
        setUpLightModePreference();
        setUpDefaultEncounterProviderPreference();
        setUpDefaultEncounterLocationPreference();
        setUpLocalePreference();
        setUpFormDuplicateCheckPreference();
    }

    private void setUpServerPreference(){
        String serverPreferenceKey = getResources().getString(R.string.preference_server);
        final EditTextPreference serverPreference = (EditTextPreference) getPreferenceScreen().findPreference(serverPreferenceKey);
        serverPreferences = serverPreference;
        serverPreference.setSummary(serverPreference.getText());
        serverPreference.getEditText().setSingleLine();
        serverPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
              @Override
              public boolean onPreferenceClick(Preference preference) {
                  AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                  builder
                          .setCancelable(true)
                          .setIcon(getIconWarning())
                          .setTitle(getResources().getString(R.string.title_qrcode))
                          .setMessage(getResources().getString(R.string.info_scan_qrcode))
                          .setPositiveButton(R.string.general_yes, positiveBarcodeClickListener())
                          .setNegativeButton(R.string.general_no, null).create().show();
                  return true;
              }
        });


        serverPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                newURL = newValue.toString();
                if (!serverPreference.getText().equalsIgnoreCase(newURL)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder
                            .setCancelable(true)
                            .setIcon(getIconWarning())
                            .setTitle(getResources().getString(R.string.general_caution))
                            .setMessage(getResources().getString(R.string.warning_switch_server))
                            .setPositiveButton(R.string.general_yes, positiveClickListener())
                            .setNegativeButton(R.string.general_no, null).create().show();
                }
                return false;
            }
        });

        registerTextPreferenceChangeHandler(serverPreferenceKey, serverPreference);
    }

    private void setUpUsernamePreference(){
        String usernamePreferenceKey = getResources().getString(R.string.preference_username);
        EditTextPreference usernamePreference = (EditTextPreference) getPreferenceScreen().findPreference(usernamePreferenceKey);
        usernamePreference.setSummary(usernamePreference.getText());
        usernamePreference.setEnabled(false);
        usernamePreference.setSelectable(false);

        registerTextPreferenceChangeHandler(usernamePreferenceKey, usernamePreference);
    }

    private void setUpTimeoutPreference(){
        String timeoutPreferenceKey = getResources().getString(R.string.preference_timeout);
        EditTextPreference timeoutPreference = (EditTextPreference) getPreferenceScreen().findPreference(timeoutPreferenceKey);
        timeoutPreference.setSummary(timeoutPreference.getText());
        timeoutPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Integer timeOutInMin = extractSessionTimoutValue(o);
                if (timeOutInMin != SESSION_TIMEOUT_INVALID_VALUE) {
                    ((MuzimaApplication) getActivity().getApplication()).resetTimer(timeOutInMin);
                    return true;
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder
                            .setCancelable(true)
                            .setIcon(getIconWarning())
                            .setTitle(getResources().getString(R.string.general_caution))
                            .setMessage(getResources().getString(R.string.warning_session_timeout))
                            .setPositiveButton(getResources().getText(R.string.general_ok), null).create().show();
                }
                return false;
            }

            private Integer extractSessionTimoutValue(Object o) {
                try {
                    Integer timeOutInMin = Integer.valueOf(o.toString());
                    if (timeOutInMin > SESSION_TIMEOUT_MINIMUM && timeOutInMin <= SESSION_TIMEOUT_MAXIMUM) {
                        return timeOutInMin;
                    }
                    return SESSION_TIMEOUT_INVALID_VALUE;
                } catch (NumberFormatException nfe) {
                    return SESSION_TIMEOUT_INVALID_VALUE;
                }
            }
        });

        registerTextPreferenceChangeHandler(timeoutPreferenceKey, timeoutPreference);
    }

    private void setUpAutosaveIntervalPreference(){
        String autoSavePreferenceKey = getResources().getString(R.string.preference_auto_save_interval);
        EditTextPreference autoSaveIntervalPreference = (EditTextPreference) getPreferenceScreen().findPreference(autoSavePreferenceKey);
        autoSaveIntervalPreference.setSummary(autoSaveIntervalPreference.getText());
        autoSaveIntervalPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Integer autoSaveInMin = extractAutoSaveIntervalValue(o);
                if (autoSaveInMin != AUTO_SAVE_INTERVAL_INVALID_VALUE) {
                    return true;
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder
                            .setCancelable(true)
                            .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                            .setTitle(getResources().getString(R.string.general_caution))
                            .setMessage(getResources().getString(R.string.warning_auto_save_interval))
                            .setPositiveButton(getResources().getText(R.string.general_ok), null).create().show();
                }
                return false;
            }

            private Integer extractAutoSaveIntervalValue(Object o) {
                try {
                    Integer saveIntervalInMin = Integer.valueOf(o.toString());
                    if (saveIntervalInMin > AUTO_SAVE_INTERVAL_MINIMUM && saveIntervalInMin <= AUTO_SAVE_INTERVAL_MAXIMUM) {
                        return saveIntervalInMin;
                    }
                    return AUTO_SAVE_INTERVAL_INVALID_VALUE;
                } catch (NumberFormatException nfe) {
                    return AUTO_SAVE_INTERVAL_INVALID_VALUE;
                }
            }
        });

        registerTextPreferenceChangeHandler(autoSavePreferenceKey, autoSaveIntervalPreference);
    }

    private void setUpPasswordPreference(){
        String passwordPreferenceKey = getResources().getString(R.string.preference_password);
        EditTextPreference passwordPreference = (EditTextPreference) getPreferenceScreen().findPreference(passwordPreferenceKey);
        if (passwordPreference.getText() != null) {
            passwordPreference.setSummary(passwordPreference.getText().replaceAll(".", "*"));
        }
        passwordPreference.setEnabled(false);
        passwordPreference.setSelectable(false);

        registerTextPreferenceChangeHandler(passwordPreferenceKey, passwordPreference);
    }


    private void setUpDefaultEncounterProviderPreference(){
        String encounterProviderPreferenceKey = getResources().getString(R.string.preference_encounter_provider_key);
        encounterProviderPreference = (SwitchPreference) getPreferenceScreen().findPreference(encounterProviderPreferenceKey);
        encounterProviderPreference.setSummary(encounterProviderPreference.getSummary());

        encounterProviderPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (o.equals(Boolean.TRUE)) {
                    String loggedInUserSystemId = ((MuzimaApplication) getActivity().getApplication()).getAuthenticatedUser().getSystemId();
                    if (((MuzimaApplication) getActivity().getApplication()).getProviderController().getProviderBySystemId(loggedInUserSystemId) == null) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder
                                .setCancelable(true)
                                .setIcon(getIconWarning())
                                .setTitle(getResources().getString(R.string.title_provider_not_set))
                                .setMessage(getResources().getString(R.string.hint_provider_not_set))
                                .setPositiveButton(getResources().getText(R.string.general_ok), null).create().show();
                        return false;
                    } else {
                        return true;
                    }
                }
                return true;
            }
        });
    }

    private void setUpDuplicateFormDataWarningPreference(){
        String duplicateFormDataPreferenceKey = getResources().getString(R.string.preference_duplicate_form_data_key);
        SwitchPreference duplicateFormDataPreference = (SwitchPreference) getPreferenceScreen().findPreference(duplicateFormDataPreferenceKey);
        duplicateFormDataPreference.setSummary(duplicateFormDataPreference.getSummary());
    }

    private void setUpFontSizePreference(){
        String fontSizePreferenceKey = getResources().getString(R.string.preference_font_size);
        ListPreference fontSizePreference = (ListPreference) getPreferenceScreen().findPreference(fontSizePreferenceKey);
        fontSizePreference.setSummary(fontSizePreference.getValue());
        registerListPreferenceChangeHandler(fontSizePreferenceKey, fontSizePreference);
    }

    private void setUpLightModePreference(){
        String lightModePreferenceKey = getResources().getString(R.string.preference_light_mode);
        final SwitchPreference lightModePreference = (SwitchPreference) getPreferenceScreen()
                .findPreference(lightModePreferenceKey);
        lightModePreference.setSummary(lightModePreference.getSummary());
    }

    private void setUpDefaultEncounterLocationPreference(){
        ListPreference listPreferenceCategory = (ListPreference) findPreference(getResources().getString(R.string.preference_default_encounter_location));
        if (listPreferenceCategory != null) {

            LocationController locationController = ((MuzimaApplication) getActivity().getApplication()).getLocationController();
            List<Location> locations = new ArrayList<>();
            try {
                locations = locationController.getAllLocations();
            } catch (LocationController.LocationLoadException e) {
                Log.e(getClass().getSimpleName(),e.getMessage());
            }
            CharSequence entries[] = new String[locations.size()+1];
            CharSequence entryValues[] = new String[locations.size()+1];

            entries[0] = getResources().getString(R.string.no_default_encounter_location);
            entryValues[0] = getResources().getString(R.string.no_default_encounter_location);
            int i = 1;
            for (Location location : locations) {
                entries[i] = location.getName();
                entryValues[i] = Integer.toString(location.getId());
                i++;
            }
            listPreferenceCategory.setEntries(entries);
            listPreferenceCategory.setEntryValues(entryValues);
        }

        String defaultEncounterLocationkey = getResources().getString(R.string.preference_default_encounter_location);
        ListPreference defaultEncounterLocationPreference = (ListPreference) getPreferenceScreen().findPreference(defaultEncounterLocationkey);
        LocationController locationController = ((MuzimaApplication) getActivity().getApplication()).getLocationController();
        List<Location> locations = new ArrayList<>();
        try {
            locations = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Log.e(getClass().getSimpleName(),e.getMessage());
        }

        String locationName = getResources().getString(R.string.no_default_encounter_location);
        for (Location location : locations) {
            if(Integer.toString(location.getId()).equals(defaultEncounterLocationPreference.getValue())){
                locationName=location.getName();
            }
        }
        defaultEncounterLocationPreference.setSummary(locationName);
        registerListPreferenceChangeHandlerForDefaultLocation(defaultEncounterLocationkey, defaultEncounterLocationPreference);

    }

    private void setUpLocalePreference(){
        String localePreferenceKey = getResources().getString(R.string.preference_app_language);
        ListPreference localePreference = (ListPreference) getPreferenceScreen().findPreference(localePreferenceKey);
        localePreference.setSummary(localePreference.getValue());
        registerListPreferenceChangeHandler(localePreferenceKey, localePreference);

    }

    private void setUpFormDuplicateCheckPreference(){
        String enableFormDuplicateCheckKey = getResources().getString(R.string.preference_duplicate_form_data_key);
        duplicateFormCheckCheckBoxPreference = (SwitchPreference) getPreferenceScreen()
                .findPreference(enableFormDuplicateCheckKey);
        duplicateFormCheckCheckBoxPreference.setOnPreferenceChangeListener(
                new ServerSideSettingPreferenceChangeListener(new DownloadFormDuplicateCheckSettingAsyncTask()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            mActivity =(Activity) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        resetEncounterProviderPreference();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (key.equalsIgnoreCase("wizardFinished") || key.equalsIgnoreCase("encounterProviderPreference")) {
            return;
        }
        PreferenceChangeHandler preferenceChangeHandler = actions.get(key);
        if(preferenceChangeHandler!=null) {
            preferenceChangeHandler.handle(sharedPreferences);
        }
    }

    private interface PreferenceChangeHandler {
        void handle(SharedPreferences sharedPreferences);
    }


    private void registerTextPreferenceChangeHandler(final String key, final EditTextPreference preference) {

        actions.put(key, new PreferenceChangeHandler() {
            @Override
            public void handle(SharedPreferences sharedPreferences) {
                preference.setSummary(sharedPreferences.getString(key, StringUtils.EMPTY));
            }
        });
    }

    private void registerListPreferenceChangeHandler(final String key, final ListPreference preference) {

        actions.put(key, new PreferenceChangeHandler() {
            @Override
            public void handle(SharedPreferences sharedPreferences) {
                preference.setSummary(sharedPreferences.getString(key, StringUtils.EMPTY));
            }
        });
    }

    private void registerListPreferenceChangeHandlerForDefaultLocation(final String key, final ListPreference preference) {

        actions.put(key, new PreferenceChangeHandler() {
            @Override
            public void handle(SharedPreferences sharedPreferences) {
                if(NumberUtils.isNumber(sharedPreferences.getString(key, StringUtils.EMPTY))){
                    LocationController locationController = ((MuzimaApplication) mActivity.getApplicationContext()).getLocationController();
                    List<Location> locations = new ArrayList<>();
                    try {
                        locations = locationController.getAllLocations();
                    } catch (LocationController.LocationLoadException e) {
                        Log.e(getClass().getSimpleName(),e.getMessage());
                    }
                    String locationName = "";
                    for (Location location : locations) {
                        if(Integer.toString(location.getId()).equals(sharedPreferences.getString(key, StringUtils.EMPTY))){
                            locationName=location.getName();
                        }
                    }
                    preference.setSummary(locationName);
                }else {
                    preference.setSummary(sharedPreferences.getString(key, StringUtils.EMPTY));
                }
            }
        });
    }

    private Dialog.OnClickListener positiveClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeServerURL(dialog);
            }

        };
    }

    private Dialog.OnClickListener positiveBarcodeClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                launchScanner();
            }

        };
    }

    private void changeServerURL(DialogInterface dialog) {
        dialog.dismiss();
        if (NetworkUtils.isConnectedToNetwork(getActivity())) {
            new ValidateURLTask(this).execute(newURL);
        }
    }

    private void launchScanner(){
        Intent intent;
        intent = new Intent(this.mActivity, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        startActivityForResult(intent, RC_BARCODE_CAPTURE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    serverPreferences.getEditText().setText(barcode.displayValue);
                } else {
                    Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null");
                }
            } else {
                Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null "+CommonStatusCodes.getStatusCodeString(resultCode));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void resetEncounterProviderPreference() {
        if(encounterProviderPreference.isChecked()){
            if(((MuzimaApplication) getActivity().getApplication()).getAuthenticatedUser() == null){
                encounterProviderPreference.setChecked(false);
            }else {
                String loggedInUserSystemId = ((MuzimaApplication) getActivity().getApplication()).getAuthenticatedUser().getSystemId();
                if (((MuzimaApplication) getActivity().getApplication()).getProviderController().getProviderBySystemId(loggedInUserSystemId) == null) {
                    encounterProviderPreference.setChecked(false);
                }
            }
        }
    }

    public void validationURLResult(boolean result) {
        if (result) {
            new SyncFormDataTask(this).execute();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setCancelable(true)
                    .setIcon(getIconWarning())
                    .setTitle(R.string.general_failure)
                    .setMessage(R.string.warning_invalid_url_provided)
                    .setPositiveButton(R.string.general_ok, null);
            builder.create().show();
        }
    }

    public void handleSyncedFormDataResult(boolean isFormDataSyncSuccessful) {
        if (isFormDataSyncSuccessful) {
            new ResetDataTask((SettingsActivity)getActivity(), newURL).execute();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setCancelable(true)
                    .setIcon(getIconWarning())
                    .setTitle(R.string.general_failure)
                    .setMessage(getString(R.string.info_form_data_upload_fail))
                    .setPositiveButton(R.string.general_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new ResetDataTask((SettingsActivity)getActivity(), newURL).execute();
                        }
                    })
                    .setNegativeButton(R.string.general_no, null);
            builder.create().show();
        }
    }

    private Drawable getIconWarning(){
        return ThemeUtils.getIconWarning(getActivity());
    }

    private Drawable getIconRefresh(){
        return ThemeUtils.getIconRefresh(getActivity());

    }

    abstract class SettingDownloadAsyncTask extends MuzimaAsyncTask<Void, Void,int[] > {
        abstract  SettingDownloadAsyncTask newInstance();
    }

    class ServerSideSettingPreferenceChangeListener implements Preference.OnPreferenceChangeListener{

        private SettingDownloadAsyncTask settingDownloadAsyncTask;

        ServerSideSettingPreferenceChangeListener(SettingDownloadAsyncTask settingDownloadAsyncTask){
            this.settingDownloadAsyncTask = settingDownloadAsyncTask;
        }
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder
                    .setCancelable(true)
                    .setIcon(getIconRefresh())
                    .setTitle(getString(R.string.title_setting_refresh))
                    .setMessage(getString(R.string.hint_setting_refresh))
                    .setNegativeButton(getResources().getText(R.string.general_cancel), null)
                    .setPositiveButton(getResources().getText(R.string.general_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            settingDownloadAsyncTask.newInstance().execute();
                        }
                    }).create().show();
            return false;
        }
    }

    public class DownloadFormDuplicateCheckSettingAsyncTask extends SettingDownloadAsyncTask {
        public DownloadFormDuplicateCheckSettingAsyncTask(){ }

        @Override
        protected void onPreExecute() {

        }

        DownloadFormDuplicateCheckSettingAsyncTask newInstance(){
            return new DownloadFormDuplicateCheckSettingAsyncTask();
        }

        @Override
        public int[] doInBackground(Void... params){
            MuzimaSyncService syncService = ((MuzimaApplication) mActivity
                    .getApplication()).getMuzimaSyncService();
            return syncService.downloadSetting(Constants.ServerSettings.FORM_DUPLICATE_CHECK_ENABLED_SETTING);
        }

        @Override
        protected void onPostExecute(int[] result) {
            if(result[0] == com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                FormDuplicateCheckPreferenceService formDuplicateCheckPreferenceService
                        = ((MuzimaApplication) mActivity.getApplication()).getFormDuplicateCheckPreferenceService();
                formDuplicateCheckPreferenceService.updateFormDuplicateCheckPreferenceSettings();
                if(duplicateFormCheckCheckBoxPreference != null) {
                    duplicateFormCheckCheckBoxPreference
                            .setChecked(formDuplicateCheckPreferenceService.isFormDuplicateCheckSettingEnabled());
                }
                if(getActivity() != null) {
                    Toast.makeText(getActivity(), getString(R.string.info_setting_download_success), Toast.LENGTH_SHORT).show();
                }
            } else {
                if(getActivity() != null) {
                    Toast.makeText(getActivity(), getString(R.string.warning_setting_download_failure), Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}

/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.controller.SmartCardController;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentResult;
import com.muzima.view.custom.PagerSlidingTabStrip;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.List;

import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_READ_REQUEST_CODE;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_WRITE_REQUEST_CODE;

public class SHRObservationsDataActivity extends BroadcastListenerActivity {

    public static final boolean DEFAULT_SHR_STATUS = false;

    private ViewPager viewPager;
    private ObservationsPagerAdapter observationsPagerAdapter;
    private Patient patient;
    private AlertDialog writeSHRDataOptionDialog;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shr__observations__data_);

        // Show the Up button in the action bar.
        setupActionBar();
        initPager();
        initPagerIndicator();
        logEvent("VIEW_SHR","{\"patientuuid\":\""+patient.getUuid()+"\"}");

    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        if (!isSHREnabled()){
            onBackPressed();
        }
    }

    private void initPagerIndicator() {
        PagerSlidingTabStrip pagerTabsLayout = findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(pagerTabsLayout.getIndicatorTextColor());
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setViewPager(viewPager);
        viewPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
    }

    private void initPager() {
        viewPager = findViewById(R.id.pager);

        Boolean isSHRData = true;
//        observationsPagerAdapter = new ObservationsPagerAdapter(getApplicationContext(), getSupportFragmentManager(), isSHRData, patient);
//        observationsPagerAdapter.initPagerViews();
//        viewPager.setAdapter(observationsPagerAdapter);
    }

    /**
     * Set up the {@link androidx.appcompat.app.ActionBar}.
     */
    private void setupActionBar() {
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        getActionBar().setTitle(patient.getSummary());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shr_observations_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setQueryHint(getString(R.string.info_observation_search));
        searchView.setOnQueryTextListener(observationsPagerAdapter);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.obs_SHR_card_write:
                prepareWriteToCardOptionDialog(getApplicationContext());
                writeSHRDataOptionDialog.show();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        observationsPagerAdapter.cancelBackgroundQueryTasks();
    }

    private void invokeSHRApplication() {
        SmartCardController smartCardController = ((MuzimaApplication) getApplicationContext()).getSmartCardController();
        SmartCardRecord smartCardRecord = null;
        try {
            smartCardRecord = smartCardController.getSmartCardRecordByPersonUuid(patient.getUuid());
        } catch (SmartCardController.SmartCardRecordFetchException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true)
                    .setMessage(getString(R.string.failure_obtain_smartcard_record) + e.getMessage())
                    .show();
            Log.e(getClass().getSimpleName(), "Could not obtain smartcard record for writing to card", e);
        }
        if (smartCardRecord != null) {
            SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(this);
            SHRIntegrator.initiateCardWrite(smartCardRecord.getPlainPayload());
            Toast.makeText(getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                SmartCardIntentResult cardReadIntentResult = null;
                try {
                    cardReadIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                    if (cardReadIntentResult.isSuccessResult()) {
                        Toast.makeText(this, "Successfully written to card. ", Toast.LENGTH_LONG).show();
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        List<String> errors = cardReadIntentResult.getErrors();
                        if (errors != null && errors.size() > 0) {
                            for (String error : errors) {
                                stringBuilder.append(error);
                            }
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setCancelable(true)
                                .setMessage("Could not write to card. " + errors)
                                .show();
                    }
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Could not get result", e);
                }
                break;

            case SMARTCARD_WRITE_REQUEST_CODE:
                SmartCardIntentResult cardWriteIntentResult = null;
                try {
                    cardWriteIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                    List<String> writeErrors = cardWriteIntentResult.getErrors();

                    if (writeErrors == null) {
                        Snackbar.make(findViewById(R.id.client_summary_view), R.string.success_writing_smartcard, Snackbar.LENGTH_LONG)
                                .show();

                    } else if (writeErrors != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Snackbar.make(findViewById(R.id.client_summary_view), getString(R.string.failure_writing_smartcard) + writeErrors.get(0), Snackbar.LENGTH_LONG)
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark, null))
                                    .setAction(R.string.general_retry, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            invokeSHRApplication();
                                        }
                                    })
                                    .show();
                        } else {

                            Snackbar.make(findViewById(R.id.client_summary_view), getString(R.string.failure_writing_smartcard) + writeErrors.get(0), Snackbar.LENGTH_LONG)
                                    .setActionTextColor(getResources().getColor(android.R.color.holo_red_dark))
                                    .setAction(R.string.general_retry, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            invokeSHRApplication();
                                        }
                                    })
                                    .show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                writeSHRDataOptionDialog.dismiss();
                writeSHRDataOptionDialog.cancel();
                break;
        }
    }

    private void prepareWriteToCardOptionDialog(Context context) {

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.write_to_card_option_dialog_layout, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(SHRObservationsDataActivity.this);

        writeSHRDataOptionDialog = alertBuilder
                .setView(dialogView)
                .create();

        writeSHRDataOptionDialog.setCancelable(true);
        TextView searchDialogTextView = dialogView.findViewById(R.id.patent_dialog_message_textview);
        Button yesOptionSHRSearchButton = dialogView.findViewById(R.id.yes_SHR_search_dialog);
        Button noOptionSHRSearchButton = dialogView.findViewById(R.id.no_SHR_search_dialog);
        searchDialogTextView.setText(getString(R.string.hint_write_SHR_to_card));

        yesOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokeSHRApplication();
            }

        });

        noOptionSHRSearchButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                writeSHRDataOptionDialog.cancel();
                writeSHRDataOptionDialog.dismiss();
            }
        });
    }

    private boolean isSHREnabled(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return preferences.getBoolean(getResources().getString(R.string.preference_enable_shr_key), SHRObservationsDataActivity.DEFAULT_SHR_STATUS);
    }
}

/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;

public class HelpActivity extends BaseActivity {

    public static final String HELP_TYPE = "HELP_TYPE";
    public static final int COHORT_WIZARD_HELP = 1;
    public static final int COHORT_PREFIX_HELP = 2;
    public static final int CUSTOM_CONCEPT_HELP = 3;
    public static final String MUZIMA_INITAL_SETUP_GUIDE = "file:///android_asset/help-content/mUzima_initial_setup.html";
    public static final String PATIENT_FILLING_FORM = "file:///android_asset/help-content/filling-forms-for-a-patient.html";
    public static final String INTRODUCTION_VIDEO = "https://www.youtube.com/watch?v=xnFACOHGzKg";
    private TextView helpContentView;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        setHelpContent();
    }

    private void setHelpContent() {
        helpContentView = (TextView) findViewById(R.id.helpContent);
        linearLayout = (LinearLayout) findViewById(R.id.helpInfoMenu);
        int helpType = getIntent().getIntExtra(HELP_TYPE, 0);
        switch (helpType) {
            case COHORT_WIZARD_HELP:
                showHelpContentView();
                helpContentView.setText(getResources().getText(R.string.cohort_wizard_help));
                setTitle(R.string.cohort_wizard_help_title);
                break;
            case COHORT_PREFIX_HELP:
                showHelpContentView();
                helpContentView.setText(getResources().getText(R.string.cohort_prefix_help));
                setTitle(R.string.cohort_prefix_help_title);
                break;
            case CUSTOM_CONCEPT_HELP:
                showHelpContentView();
                helpContentView.setText(getResources().getText(R.string.custom_concept_help));
                setTitle(R.string.custom_concept_help_title);
                break;
            default:
                helpContentView.setVisibility(View.GONE);
                linearLayout.setVisibility(View.VISIBLE);
                setTitle(R.string.title_activity_help);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.help, menu);
        super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        return true;
    }

    public void viewPatientFormFillingHelpContent(View view) {
        startHelpContentDisplayActivity(PATIENT_FILLING_FORM,getText(R.string.filling_patient_forms_help).toString());
    }

    public void viewMuzimaInitialSetupGuide(View view) {
        startHelpContentDisplayActivity(MUZIMA_INITAL_SETUP_GUIDE,getText(R.string.muzima_initial_setup_guide).toString());
    }

    public void viewIntroductionVideo(View view) {
        Intent introductionVideo = new Intent(Intent.ACTION_VIEW);
        introductionVideo.setData(Uri.parse(INTRODUCTION_VIDEO));
        startActivity(introductionVideo);
    }

    private void startHelpContentDisplayActivity(String filePath, String title) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.HELP_FILE_PATH_PARAM, filePath);
        intent.putExtra(WebViewActivity.HELP_TITLE,title);
        startActivity(intent);
    }

    private void showHelpContentView() {
        helpContentView.setVisibility(View.VISIBLE);
        linearLayout.setVisibility(View.GONE);
    }

    private void removeHelpMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_help);
        menuSettings.setVisible(false);
    }
}

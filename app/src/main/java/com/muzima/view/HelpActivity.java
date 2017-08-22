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
import android.widget.ExpandableListView;
import android.widget.TextView;
import com.muzima.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//import androidhive.dashboard.R;

import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.Toast;

public class HelpActivity extends BaseActivity {

    public static final String HELP_TYPE = "HELP_TYPE";
    public static final int COHORT_WIZARD_HELP = 1;
    public static final int COHORT_PREFIX_HELP = 2;
    public static final int CUSTOM_CONCEPT_HELP = 3;
    public static final int CUSTOM_LOCATION_HELP = 4;
    public static final int CUSTOM_PROVIDER_HELP = 5;
    public static final String MUZIMA_INITAL_SETUP_GUIDE = "file:///android_asset/www/help-content/mUzima_initial_setup.html";
    public static final String ABOUT_DASHBOARD_FORM = "file:///android_asset/www/help-content/About-dashboard.html";
    public static final String MUZIMA_SETTINGS = "file:///android_asset/www/help-content/Settings.html";
    public static final String FILL_PATIENT_FORMS = "file:///android_asset/www/help-content/filling-forms-for-a-patient.html";
    public static final String INTRODUCTION_VIDEO = "https://www.youtube.com/watch?v=xnFACOHGzKg";
    public static final String SETTING_UP_MUZIMA_VIDEO = "https://www.youtube.com/watch?v=nn7k1TL1qG0&feature=youtu.be";
    public static final String TAGGING_FORMS_VIDEO = "https://www.youtube.com/watch?v=Ls4qpSYRep8&feature=youtu.be";
    public static final String DOWNLOADING_COHORTS_VIDEO = "https://www.youtube.com/watch?v=uvVT9tRpCxY&feature=youtu.be";
    public static final String CHANGE_SETTING_VIDEO = "https://www.youtube.com/watch?v=4VtkXUEP11k&feature=youtu.be";
    public static final String DOWNLOADING_FORMS_VIDEO = "https://www.youtube.com/watch?v=8uNCq1EK8V8&feature=youtu.be";
    private TextView helpContentView;
    private View scrollView;

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_new);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);
        // expListView.expandGroup(0);

        expListView.setOnGroupExpandListener(new OnGroupExpandListener() {
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                if(groupPosition != previousGroup)
                    expListView.collapseGroup(previousGroup);
                previousGroup = groupPosition;
            }
        });

        expListView.expandGroup(0);

        // Listview on child click listener
        expListView.setOnChildClickListener(new OnChildClickListener()
        {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int group_position, int child_position, long id)
            {
                if(group_position==0 && child_position==0){
                    startHelpContentDisplayActivity(MUZIMA_INITAL_SETUP_GUIDE,(String)listAdapter.getChild(group_position, child_position));
                }
                else if(group_position==0 && child_position==1){
                    startHelpContentDisplayActivity(ABOUT_DASHBOARD_FORM,(String)listAdapter.getChild(group_position, child_position));
                }
                else if(group_position==0 && child_position==2){
                    startHelpContentDisplayActivity(MUZIMA_SETTINGS,(String)listAdapter.getChild(group_position, child_position));
                }
                else if(group_position==0 && child_position==3){
                    startHelpContentDisplayActivity(FILL_PATIENT_FORMS,(String)listAdapter.getChild(group_position, child_position));
                }
                //video links
                else if(group_position==1 && child_position==0){
                    viewVideo(INTRODUCTION_VIDEO);
                }
                else if(group_position==1 && child_position==1){
                    viewVideo(SETTING_UP_MUZIMA_VIDEO);
                }
                else if(group_position==1 && child_position==2){
                    viewVideo(TAGGING_FORMS_VIDEO);
                }
                else if(group_position==1 && child_position==3){
                    viewVideo(DOWNLOADING_COHORTS_VIDEO);
                }
                else if(group_position==1 && child_position==4){
                    viewVideo(CHANGE_SETTING_VIDEO);
                }
                else if(group_position==1 && child_position==5){
                    viewVideo(DOWNLOADING_FORMS_VIDEO);
                }
                /**String name = (String)listAdapter.getChild(group_position, child_position);
                Toast.makeText(getApplicationContext(),name,Toast.LENGTH_SHORT).show();*/
                return false;
            }
        });
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        // Adding child data
        listDataHeader.add("Help Center");
        listDataHeader.add("mUzima Video Links");
        //listDataHeader.add("Coming Soon..");

        // Adding child data
        List<String> howTo = new ArrayList<String>();
        howTo.add("Initial setup for mUzima");
        howTo.add("About Dashboard");
        howTo.add("mUzima Settings");
        howTo.add("Fill out forms for a patient");

        List<String> videoLinks = new ArrayList<String>();
        videoLinks.add("Introduction to mUzima");
        videoLinks.add("Setting up mUzima");
        videoLinks.add("Tagging Forms");
        videoLinks.add("Downloading Cohorts");
        videoLinks.add("Change Settings");
        videoLinks.add("Downloading Forms");


        listDataChild.put(listDataHeader.get(0), howTo); // Header, Child data
        listDataChild.put(listDataHeader.get(1), videoLinks);
        //listDataChild.put(listDataHeader.get(2), comingSoon);
    }

    private void startHelpContentDisplayActivity(String filePath, String title) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.HELP_FILE_PATH_PARAM, filePath);
        intent.putExtra(WebViewActivity.HELP_TITLE,title);
        startActivity(intent);
    }

    private void viewVideo(String videoUrl){
        Intent playVideoIntent = new Intent(Intent.ACTION_VIEW);
        playVideoIntent.setData(Uri.parse(videoUrl));
        startActivity(playVideoIntent);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help, menu);
        super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        return true;
    }

    public void viewPatientFormFillingHelpContent(View view) {
        startHelpContentDisplayActivity(ABOUT_DASHBOARD_FORM,getText(R.string.title_fill_patient_forms_help).toString());
    }

    public void viewMuzimaInitialSetupGuide(View view) {
        startHelpContentDisplayActivity(MUZIMA_INITAL_SETUP_GUIDE,getText(R.string.title_initial_setup_guide).toString());
    }

    public void viewMuzimaUserGuide(View view) {
        startHelpContentDisplayActivity(MUZIMA_SETTINGS,getText(R.string.title_server_side_setup).toString());
    }

    public void viewMuzimaTrainingManual(View view) {
        startHelpContentDisplayActivity(FILL_PATIENT_FORMS,getText(R.string.title_muzima_training_manual).toString());
    }

    private void startHelpContentDisplayActivity(String filePath, String title) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.HELP_FILE_PATH_PARAM, filePath);
        intent.putExtra(WebViewActivity.HELP_TITLE,title);
        startActivity(intent);
    }

    private void showHelpContentView() {
        helpContentView.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);
    }

    private void removeHelpMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_help);
        menuSettings.setVisible(false);
    }

    public void viewIntroductionVideo(View view) {
        viewVideo(INTRODUCTION_VIDEO);
    }

    public void viewSettingUpMuzimaVideo(View view) {
        viewVideo(SETTING_UP_MUZIMA_VIDEO);
    }

    public void viewTaggingFormsVideo(View view) {
        viewVideo(TAGGING_FORMS_VIDEO);
    }

    public void viewDownloadingCohortsVideo(View view) {
        viewVideo(DOWNLOADING_COHORTS_VIDEO);
    }

    public void viewChangeSettingsVideo(View view) {
        viewVideo(CHANGE_SETTING_VIDEO);
    }

    public void viewDownloadingFormsVideo(View view) {
        viewVideo(DOWNLOADING_FORMS_VIDEO);
    }

    private void viewVideo(String videoUrl){
        Intent playVideoIntent = new Intent(Intent.ACTION_VIEW);
        playVideoIntent.setData(Uri.parse(videoUrl));
        startActivity(playVideoIntent);
    }*/
}

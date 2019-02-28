/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

public class HelpActivity extends BaseActivity {

    public static final String HELP_TYPE = "HELP_TYPE";
    public static final int COHORT_WIZARD_HELP = 1;
    public static final int COHORT_PREFIX_HELP = 2;
    public static final int CUSTOM_CONCEPT_HELP = 3;
    public static final int CUSTOM_LOCATION_HELP = 4;
    public static final int CUSTOM_PROVIDER_HELP = 5;
    private static final String MUZIMA_INITAL_SETUP_GUIDE = "file:///android_asset/www/help-content/mUzima_initial_setup.html";
    private static final String ABOUT_DASHBOARD_FORM = "file:///android_asset/www/help-content/About-dashboard.html";
    private static final String MUZIMA_SETTINGS = "file:///android_asset/www/help-content/Settings.html";
    private static final String FILL_PATIENT_FORMS = "file:///android_asset/www/help-content/filling-forms-for-a-patient.html";
    private static final String GUIDED_SETUP = "https://youtu.be/aSa4CcGtGdo";
    private static final String ADVANCED_SETUP = "https://youtu.be/3yFieYJQEbs";
    private static final String COHORTS_DOWNLOAD_ON_DEMAND = "https://youtu.be/CcQZy32O8JQ";
    private static final String CONCEPTS_DOWNLOAD_ON_DEMAND = "https://youtu.be/Q6QIE7z6_O0";
    private static final String FORMS_DOWNLOAD_ON_DEMAND = "https://youtu.be/1cPF58jhLiA";
    private static final String CLIENT_REGISTRATION = "https://youtu.be/fmjhF-juq4k";
    private static final String LOCAL_CLIENT_SEARCH = "https://youtu.be/630daM1wxGE";
    private static final String SERVER_CLIENT_SEARCH = "https://youtu.be/C1xDs8wWjNM";
    private TextView helpContentView;
    private View scrollView;

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_new);

        // get the listview
        expListView = findViewById(R.id.lvExp);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

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
                    viewVideo(GUIDED_SETUP);
                }
                else if(group_position==1 && child_position==1){
                    viewVideo(ADVANCED_SETUP);
                }
                else if(group_position==1 && child_position==2){
                    viewVideo(COHORTS_DOWNLOAD_ON_DEMAND);
                }
                else if(group_position==1 && child_position==3){
                    viewVideo(CONCEPTS_DOWNLOAD_ON_DEMAND);
                }
                else if(group_position==1 && child_position==4){
                    viewVideo(FORMS_DOWNLOAD_ON_DEMAND);
                }
                else if(group_position==1 && child_position==5){
                    viewVideo(CLIENT_REGISTRATION);
                }
                else if(group_position==1 && child_position==6){
                    viewVideo(LOCAL_CLIENT_SEARCH);
                }
                else if(group_position==1 && child_position==7){
                    viewVideo(SERVER_CLIENT_SEARCH);
                }
                return false;
            }
        });
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        // Adding child data
        listDataHeader.add(getString(R.string.title_html_help));
        listDataHeader.add(getString(R.string.title_help_video_links));

        // Adding child data
        List<String> howTo = new ArrayList<>();
        howTo.add(getString(R.string.title_advanced_setup_help_html));
        howTo.add(getString(R.string.title_about_dashboard_help_html));
        howTo.add(getString(R.string.title_settings_help_html));
        howTo.add(getString(R.string.title_fill_patient_forms_help));

        List<String> videoLinks = new ArrayList<>();
        videoLinks.add(getString(R.string.title_guided_setup_help_video));
        videoLinks.add(getString(R.string.title_advanced_setup_help_video));
        videoLinks.add(getString(R.string.title_cohort_download_help_video));
        videoLinks.add(getString(R.string.title_concept_download_help_video));
        videoLinks.add(getString(R.string.title_form_download_help_video));
        videoLinks.add(getString(R.string.title_client_registration_help_video));
        videoLinks.add(getString(R.string.title_local_client_search_help_video));
        videoLinks.add(getString(R.string.title_server_client_search_help_video));

        listDataChild.put(listDataHeader.get(0), howTo); // Header, Child data
        listDataChild.put(listDataHeader.get(1), videoLinks);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean returnValue = super.onCreateOptionsMenu(menu);
        removeHelpMenu(menu);
        return returnValue;
    }

    private void removeHelpMenu(Menu menu) {
        MenuItem menuHelp = menu.findItem(R.id.action_help);
        if (menuHelp != null) menuHelp.setVisible(false);
    }
}

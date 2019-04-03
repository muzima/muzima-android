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
import android.os.Build;
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

public class HelpActivity extends BaseHelpActivity {

    public static final int YOUTUBE_API_RESULT = 100;
    public static final String YOUTUBE_API_CANCEL_CASE = "YOUTUBE_API_CANCEL_CASE";
    public static final String YOUTUBE_INITIALIZATION_FAILURE= "INITIALIZATION_FAILURE";
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
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
        expListView.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int group_position, int child_position, long id) {
                if (group_position == 0 && child_position == 0) {
                    startHelpContentDisplayActivity(MUZIMA_INITAL_SETUP_GUIDE, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 0 && child_position == 1) {
                    startHelpContentDisplayActivity(ABOUT_DASHBOARD_FORM, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 0 && child_position == 2) {
                    startHelpContentDisplayActivity(MUZIMA_SETTINGS, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 0 && child_position == 3) {
                    startHelpContentDisplayActivity(FILL_PATIENT_FORMS, (String) listAdapter.getChild(group_position, child_position));
                }
                //video links
                else if (group_position == 1 && child_position == 0) {
                    startVideoContentDisplayActivity(GUIDED_SETUP, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 1) {
                    startVideoContentDisplayActivity(ADVANCED_SETUP, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 2) {
                    startVideoContentDisplayActivity(COHORTS_DOWNLOAD_ON_DEMAND, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 3) {
                    startVideoContentDisplayActivity(CONCEPTS_DOWNLOAD_ON_DEMAND, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 4) {
                    startVideoContentDisplayActivity(FORMS_DOWNLOAD_ON_DEMAND, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 5) {
                    startVideoContentDisplayActivity(CLIENT_REGISTRATION, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 6) {
                    startVideoContentDisplayActivity(LOCAL_CLIENT_SEARCH, (String) listAdapter.getChild(group_position, child_position));
                } else if (group_position == 1 && child_position == 7) {
                    startVideoContentDisplayActivity(SERVER_CLIENT_SEARCH, (String) listAdapter.getChild(group_position, child_position));
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
        intent.putExtra(WebViewActivity.HELP_TITLE, title);
        startActivity(intent);
    }

    private void startVideoContentDisplayActivity(String filePath, String title) {
        Intent intent = new Intent(this, YouTubeVideoViewActivity.class);
        intent.putExtra(YouTubeVideoViewActivity.VIDEO_PATH, filePath);
        intent.putExtra(YouTubeVideoViewActivity.VIDEO_TITLE, title);
        startActivityForResult(intent, YOUTUBE_API_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if youtube doen't work, try webView
        if (requestCode == YOUTUBE_API_RESULT) {
            if (resultCode == RESULT_CANCELED) {
                if (data == null) {
                    return;
                }
                if (data.hasExtra(YOUTUBE_API_CANCEL_CASE) && data.getStringExtra(YOUTUBE_API_CANCEL_CASE).equals(YOUTUBE_INITIALIZATION_FAILURE)) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                        startVideoWebViewActivity(data.getStringExtra(VIDEO_PATH), data.getStringExtra(VIDEO_TITLE));
                    } else {
                        viewVideo(data.getStringExtra(VIDEO_PATH));
                    }
                }
            }
        }
    }

    private void startVideoWebViewActivity(String filePath, String title) {
        Intent intent = new Intent(this, VideoWebViewActivity.class);
        intent.putExtra(VideoWebViewActivity.VIDEO_PATH, filePath);
        intent.putExtra(VideoWebViewActivity.VIDEO_TITLE, title);
        startActivity(intent);
    }

    private void viewVideo(String videoUrl) {
        Intent playVideoIntent = new Intent(Intent.ACTION_VIEW);
        playVideoIntent.setData(Uri.parse(videoUrl));
        startActivity(playVideoIntent);
    }
}

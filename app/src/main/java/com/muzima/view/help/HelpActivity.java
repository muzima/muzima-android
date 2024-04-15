/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import com.muzima.R;
import com.muzima.adapters.ExpandableListAdapter;
import com.muzima.utils.LanguageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class HelpActivity extends BaseHelpActivity {

    public static final int YOUTUBE_API_RESULT = 100;
    public static final String YOUTUBE_API_CANCEL_CASE = "YOUTUBE_API_CANCEL_CASE";
    public static final String YOUTUBE_INITIALIZATION_FAILURE = "INITIALIZATION_FAILURE";
    public static final String VIDEO_PATH = "VIDEO_PATH";
    public static final String VIDEO_TITLE = "VIDEO_TITLE";
    private static final String MUZIMA_INITAL_SETUP_GUIDE = "mUzima_initial_setup.html";
    private static final String ABOUT_DASHBOARD_FORM = "About-dashboard.html";
    private static final String MUZIMA_SETTINGS = "Settings.html";
    private static final String FILL_PATIENT_FORMS = "filling-forms-for-a-patient.html";
    private static final String GUIDED_SETUP = "https://youtu.be/aSa4CcGtGdo";
    private static final String COHORTS_DOWNLOAD_ON_DEMAND = "https://youtu.be/CcQZy32O8JQ";
    private static final String CONCEPTS_DOWNLOAD_ON_DEMAND = "https://youtu.be/Q6QIE7z6_O0";
    private static final String FORMS_DOWNLOAD_ON_DEMAND = "https://youtu.be/1cPF58jhLiA";
    private static final String CLIENT_REGISTRATION = "https://youtu.be/fmjhF-juq4k";
    private static final String LOCAL_CLIENT_SEARCH = "https://youtu.be/630daM1wxGE";
    private static final String SERVER_CLIENT_SEARCH = "https://youtu.be/C1xDs8wWjNM";

    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChildTitlesMap;
    private HashMap<String, List<String>> helpContentResourcesMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_new);

        expListView = findViewById(R.id.lvExp);
        prepareListData();

        listAdapter = new ExpandableListAdapter(this, listDataHeader, listDataChildTitlesMap);
        expListView.setAdapter(listAdapter);

        expListView.setOnGroupExpandListener(new OnGroupExpandListener() {
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                if (groupPosition != previousGroup)
                    expListView.collapseGroup(previousGroup);
                previousGroup = groupPosition;
            }
        });

        expListView.expandGroup(0);

        // Listview on child click listener
        expListView.setOnChildClickListener(new OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                startHelpContentDisplayActivity(
                        helpContentResourcesMap.get(listDataHeader.get(groupPosition)).get(childPosition),
                        (String) listAdapter.getChild(groupPosition, childPosition));
                return false;
            }
        });
        logEvent("VIEW_HELP");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {
        listDataHeader = new ArrayList<>();
        listDataChildTitlesMap = new HashMap<>();

        //set root directory
        LanguageUtil languageUtil = new LanguageUtil();
        Locale locale = languageUtil.getSelectedLocale(getApplicationContext());
        String LOCAL_HELP_CONTENT_ROOT_DIRECTORY = "file:///android_asset/www/help-content/"+locale.getLanguage()+"/";

        // Adding child data
        listDataHeader.add(getString(R.string.title_html_help));
        listDataHeader.add(getString(R.string.title_help_video_links));

        // Adding child data
        List<String> howToTitles = new ArrayList<>();
        howToTitles.add(getString(R.string.title_advanced_setup_help_html));
        howToTitles.add(getString(R.string.title_about_dashboard_help_html));
        howToTitles.add(getString(R.string.title_settings_help_html));
        howToTitles.add(getString(R.string.title_fill_patient_forms_help));

        List<String> howToContent = new ArrayList<>();
        howToContent.add(LOCAL_HELP_CONTENT_ROOT_DIRECTORY+MUZIMA_INITAL_SETUP_GUIDE);
        howToContent.add(LOCAL_HELP_CONTENT_ROOT_DIRECTORY+ABOUT_DASHBOARD_FORM);
        howToContent.add(LOCAL_HELP_CONTENT_ROOT_DIRECTORY+MUZIMA_SETTINGS);
        howToContent.add(LOCAL_HELP_CONTENT_ROOT_DIRECTORY+FILL_PATIENT_FORMS);

        List<String> videoLinkTitles = new ArrayList<>();
        videoLinkTitles.add(getString(R.string.title_guided_setup_help_video));
        videoLinkTitles.add(getString(R.string.title_cohort_download_help_video));
        videoLinkTitles.add(getString(R.string.title_concept_download_help_video));
        videoLinkTitles.add(getString(R.string.title_form_download_help_video));
        videoLinkTitles.add(getString(R.string.title_client_registration_help_video));
        videoLinkTitles.add(getString(R.string.title_local_client_search_help_video));
        videoLinkTitles.add(getString(R.string.title_server_client_search_help_video));

        List<String> videoLinks = new ArrayList<>();
        videoLinks.add(GUIDED_SETUP);
        videoLinks.add(COHORTS_DOWNLOAD_ON_DEMAND);
        videoLinks.add(CONCEPTS_DOWNLOAD_ON_DEMAND);
        videoLinks.add(FORMS_DOWNLOAD_ON_DEMAND);
        videoLinks.add(CLIENT_REGISTRATION);
        videoLinks.add(LOCAL_CLIENT_SEARCH);
        videoLinks.add(SERVER_CLIENT_SEARCH);

        listDataChildTitlesMap.put(listDataHeader.get(0), howToTitles); // Header, Child data
        listDataChildTitlesMap.put(listDataHeader.get(1), videoLinkTitles);

        helpContentResourcesMap = new HashMap<>();
        helpContentResourcesMap.put(listDataHeader.get(0),howToContent);
        helpContentResourcesMap.put(listDataHeader.get(1),videoLinks);
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
                    startVideoWebViewActivity(data.getStringExtra(VIDEO_PATH), data.getStringExtra(VIDEO_TITLE));
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

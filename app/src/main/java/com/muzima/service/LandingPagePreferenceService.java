package com.muzima.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import com.muzima.R;
import com.muzima.utils.StringUtils;
import com.muzima.view.MainActivity;
import com.muzima.view.patients.PatientsListActivity;

public class LandingPagePreferenceService extends PreferenceService{
    private final String clientListLandingPageString = "Client List";

    private final SharedPreferences settings;

    public LandingPagePreferenceService(Context context){
        super(context);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Intent getLandingPageActivityLauchIntent(){
        String landingPageKey = context.getResources().getString(R.string.preference_landing_page);
        String defaultLandingPage = context.getString(R.string.general_client_list);
        String preferredLandingPage = settings.getString(landingPageKey,defaultLandingPage);
        Intent intent;
        if(StringUtils.equals(preferredLandingPage,context.getString(R.string.general_client_list))){
           intent = new Intent(context, PatientsListActivity.class);
        } else {
            intent = new Intent(context, MainActivity.class);
        }
        return intent;
    }
}

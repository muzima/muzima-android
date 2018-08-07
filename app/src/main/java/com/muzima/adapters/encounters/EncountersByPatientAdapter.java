/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.encounters;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Patient;
import com.muzima.controller.EncounterController;
import com.muzima.utils.DateUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EncountersByPatientAdapter extends EncountersAdapter {
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final String patientUuid;


    public EncountersByPatientAdapter(Activity activity, int textViewResourceId, EncounterController encounterController, Patient patient) {
        super(activity, textViewResourceId, encounterController);
        patientUuid = patient.getUuid();
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Encounter encounter=getItem(position);
        Context context = getContext();
        EncountersByPatientViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_encounter, parent, false);
            convertView.setClickable(false);
            convertView.setFocusable(false);
            holder = new EncountersByPatientViewHolder();
            holder.encounterDate = convertView.findViewById(R.id.encounterDate);
            holder.encounterFormName = convertView.findViewById(R.id.encounterFormName);
            holder.encounterLocation = convertView.findViewById(R.id.encounterLocation);
            holder.encounterProvider = convertView.findViewById(R.id.encounterProvider);
            convertView.setTag(holder);
        }else {
            holder = (EncountersByPatientViewHolder) convertView.getTag();
        }
        String location = encounter.getLocation().getName();
        String providerName = encounter.getProvider().getDisplayName();
        String date = DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime());
        String encounterType = encounter.getEncounterType().getName();
        encounterType = String.valueOf(encounterType.charAt(0)).toUpperCase()+encounterType.substring(1,encounterType.length()).toLowerCase();

        holder.encounterLocation.setText(location);
        holder.encounterProvider.setText(providerName);
        holder.encounterFormName.setText(encounterType);
        holder.encounterDate.setText(date);

        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    private class EncountersByPatientViewHolder extends ViewHolder {
        TextView encounterProvider;
        TextView encounterDate;
        TextView encounterLocation;
        TextView encounterFormName;
    }

    private class BackgroundQueryTask extends AsyncTask<String, Void, List<Encounter>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<Encounter> doInBackground(String... params) {
            List<Encounter> encounters = null;
            try {
               encounters = encounterController.getEncountersByPatientUuid(patientUuid);

            }catch(EncounterController.DownloadEncounterException e){
                Log.e(this.getClass().getSimpleName(),"Could not get patient encounters",e);
            }

            Collections.sort(encounters, encountersDateTimeComparator);
            return encounters;
        }

        private final Comparator<Encounter> encountersDateTimeComparator = new Comparator<Encounter>() {
            @Override
            public int compare(Encounter lhs, Encounter rhs) {
                if (lhs.getEncounterDatetime()==null)
                    return -1;
                if (rhs.getEncounterDatetime()==null)
                    return 1;
                return -(lhs.getEncounterDatetime()
                        .compareTo(rhs.getEncounterDatetime()));
            }
        };


        @Override
        protected void onPostExecute(List<Encounter> encounters){
            if(encounters==null){
                Toast.makeText(getContext(),getContext().getString(R.string.error_encounter_load),Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(encounters);
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }
}
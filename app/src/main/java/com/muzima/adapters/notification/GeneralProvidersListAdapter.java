/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.notification;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.providers.ProvidersAdapter;
import com.muzima.api.model.Provider;
import com.muzima.api.model.User;
import com.muzima.controller.ProviderController;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to populate all notification fetched from DB in the PatientNotificationsListFragment page.
 */
public class GeneralProvidersListAdapter extends ProvidersAdapter {
    private final MuzimaApplication muzimaApplication;
    private final ProviderController providerController;

    public GeneralProvidersListAdapter(Context context, int textViewResourceId, MuzimaApplication muzimaApplication) {
        super(context, textViewResourceId, muzimaApplication);
        this.muzimaApplication = muzimaApplication;
        this.providerController = muzimaApplication.getProviderController();
    }

    @Override
    public void reloadData() {
        new LoadBackgroundQueryTask().execute();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    /**
     * Responsible to define contract to PatientNotificationsBackgroundQueryTask.
     */
    abstract class ProvidersListBackgroundQueryTask extends AsyncTask<Void, Void, List<Provider>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Provider> allProviders) {
            if (allProviders == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_notification_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            //Removes the current items from the list.
            GeneralProvidersListAdapter.this.clear();
            //Adds recently fetched items to the list.
            for (Provider provider:allProviders) {
                if(provider.getPerson()!=null &&  !(muzimaApplication.getAuthenticatedUser().getPerson().getUuid().equals(provider.getPerson().getUuid()))) {
                    add(provider);
                }
            }
            //Send a data change request to the list, so the page can be reloaded.
            notifyDataSetChanged();

            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        protected abstract List<Provider> doInBackground(Void... voids);
    }

    /**
     * Responsible to load notifications from database. Runs in Background.
     */
    protected class LoadBackgroundQueryTask extends ProvidersListBackgroundQueryTask {

        @Override
        protected List<Provider> doInBackground(Void... voids) {
            List<Provider> allProvider = null;
            List<Provider> filteredNotifications = new ArrayList<>();
            try {
                Log.i(getClass().getSimpleName(), "Fetching general notifications from Database...");
                User authenticatedUser = ((MuzimaApplication) getContext().getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    allProvider = providerController.getAllProviders();
                    filteredNotifications.addAll(allProvider);
                }
                Log.d(getClass().getSimpleName(), "#Retrieved " + (allProvider != null ? allProvider.size() : 0) + " notifications from Database." +
                        " And filtered " + (filteredNotifications != null ? filteredNotifications.size() : 0) + " general notifications");
            } catch (ProviderController.ProviderLoadException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching the notifications", e);
            }
            return filteredNotifications;
        }
    }
}

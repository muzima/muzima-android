/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.concept;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Provider;
import com.muzima.controller.ProviderController;
import com.muzima.view.preferences.ProviderPreferenceActivity;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vikas on 16/03/15.
 */
public class SelectedProviderAdapter extends ListAdapter<Provider> {

    private final ProviderController providerController;

    public SelectedProviderAdapter(ProviderPreferenceActivity context, int textViewResourceId, ProviderController providerController) {
        super(context, textViewResourceId);
        this.providerController = providerController;
    }

    public boolean doesProviderAlreadyExist(Provider selectedProvider) {
        try {
            return providerController.getAllProviders().contains(selectedProvider);
        } catch (ProviderController.ProviderLoadException e) {
            Log.e(getClass().getSimpleName(), "Error while loading providers", e);
        }
        return false;
    }

    private class ViewHolder {
        private final CheckedTextView name;

        private ViewHolder(View providerView) {
            name = providerView.findViewById(R.id.provider_name);
        }
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_provider_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        Provider provider = getItem(position);
        if (provider != null) {
            holder.name.setText(provider.getName());
        }
        return convertView;
    }

    @Override
    public void remove(Provider provider) {
        super.remove(provider);
        try {
            providerController.deleteProvider(provider);
        } catch (ProviderController.ProviderDeleteException e) {
            Log.e(getClass().getSimpleName(), "Error while deleting the provider", e);
        }
    }

    public void removeAll(List<Provider> providersToDelete) {
        List<Provider> allProviders = null;
        try {
            allProviders = providerController.getAllProviders();
            allProviders.removeAll(providersToDelete);
            try {
                providerController.deleteProviders(providersToDelete);
            } catch (ProviderController.ProviderDeleteException e) {
                Log.e(getClass().getSimpleName(), "Error while deleting the providers", e);
            }
            this.clear();
            this.addAll(allProviders);
        } catch (ProviderController.ProviderLoadException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching the providers", e);
        }
    }

    @Override
    public void reloadData() {
        new BackgroundSaveAndQueryTask().execute();
    }

    /**
     * Responsible to save the providers into DB on selection from AutoComplete. And also fetches to Providers from DB to display in the page.
     */
    class BackgroundSaveAndQueryTask extends AsyncTask<Provider, Void, List<Provider>> {

        @Override
        protected List<Provider> doInBackground(Provider... providers) {
            List<Provider> selectedProviders = null;
            List<Provider> providersList = Arrays.asList(providers);
            try {
                if (providers.length > 0) {
                    // Called with Provider which is selected in the AutoComplete menu.
                    providerController.saveProviders(providersList);
                }
                if(providerController.newProviders().size() > 0){
                    // called when new providers are downloaded as part of new form template
                    return providerController.newProviders();
                }
                try {
                    selectedProviders = providerController.getAllProviders();
                } catch (ProviderController.ProviderLoadException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching providers from local data repository!", e);
                }
            } catch (ProviderController.ProviderSaveException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while saving provider to local data repository!", e);
            }
            return selectedProviders;
        }

        @Override
        protected void onPostExecute(List<Provider> providers) {
            if (providers == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_provider_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(providers);
            notifyDataSetChanged();
        }
    }

    public void addProvider(Provider provider) {
        new BackgroundSaveAndQueryTask().execute(provider);
    }

    public void clearSelectedProviders() {
        notifyDataSetChanged();
    }
}

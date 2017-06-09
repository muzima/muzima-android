/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.concept;

import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Location;
import com.muzima.controller.LocationController;
import com.muzima.view.preferences.LocationPreferenceActivity;

import java.util.Arrays;
import java.util.List;

/**
 * Responsible to display Locations in the Settings page.
 */
public class SelectedLocationAdapter extends ListAdapter<Location> {
    private final String TAG = SelectedLocationAdapter.class.getSimpleName();
    protected LocationController locationController;

    public SelectedLocationAdapter(LocationPreferenceActivity context, int textViewResourceId, LocationController locationController) {
        super(context, textViewResourceId);
        this.locationController = locationController;
    }

    public boolean doesLocationAlreadyExist(Location selectedLocation) {
        try {
            return locationController.getAllLocations().contains(selectedLocation);
        } catch (LocationController.LocationLoadException e) {
            Log.e(TAG, "Error while loading locations", e);
        }
        return false;
    }

    private class ViewHolder {
        private CheckedTextView name;

        private ViewHolder(View locationView) {
            name = (CheckedTextView) locationView.findViewById(R.id.location_name);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_location_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        Location location = getItem(position);
        if (location != null) {
            holder.name.setText(location.getName());
        }
        return convertView;
    }

    @Override
    public void remove(Location location) {
        super.remove(location);
        try {
            locationController.deleteLocation(location);
        } catch (LocationController.LocationDeleteException e) {
            Log.e(TAG, "Error while deleting the location", e);
        }
    }

    public void removeAll(List<Location> locationsToDelete) {
        List<Location> allLocations = null;
        try {
            allLocations = locationController.getAllLocations();
            allLocations.removeAll(locationsToDelete);
            try {
                locationController.deleteLocations(locationsToDelete);
            } catch (LocationController.LocationDeleteException e) {
                Log.e(TAG, "Error while deleting the locations", e);
            }
            this.clear();
            this.addAll(allLocations);
        } catch (LocationController.LocationLoadException e) {
            Log.e(TAG, "Error while fetching the locations", e);
        }
    }

    @Override
    public void reloadData() {
        new BackgroundSaveAndQueryTask().execute();
    }

    /**
     * Responsible to save the locations into DB on selection from AutoComplete. And also fetches to Locations from DB to display in the page.
     */
    public class BackgroundSaveAndQueryTask extends AsyncTask<Location, Void, List<Location>> {

        @Override
        protected List<Location> doInBackground(Location... locations) {
            List<Location> selectedLocations = null;
            List<Location> locationsList = Arrays.asList(locations);
            try {
                if (locations.length > 0) {
                    // Called with Location which is selected in the AutoComplete menu.
                    locationController.saveLocations(locationsList);
                }
                if(locationController.newLocations().size() > 0){
                    // called when new locations are downloaded as part of new form template
                    return locationController.newLocations();
                }
                try {
                    selectedLocations = locationController.getAllLocations();
                } catch (LocationController.LocationLoadException e) {
                    Log.w(TAG, "Exception occurred while fetching locations from local data repository!", e);
                }
            } catch (LocationController.LocationSaveException e) {
                Log.w(TAG, "Exception occurred while saving location to local data repository!", e);
            }
            return selectedLocations;
        }

        @Override
        protected void onPostExecute(List<Location> locations) {
            if (locations == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_location_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(locations);
            notifyDataSetChanged();
        }
    }

    public void addLocation(Location location) {
        new BackgroundSaveAndQueryTask().execute(location);
    }

    public void clearSelectedLocations() {
        notifyDataSetChanged();
    }
}

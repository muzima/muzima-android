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

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import com.muzima.api.model.Location;
import com.muzima.controller.LocationController;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to display and select auto-complete menu of Location while adding new Location.
 */

public class AutoCompleteLocationAdapter extends AutoCompleteBaseAdapter<Location> {


    public AutoCompleteLocationAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteLocationTextView) {
        super(context, textViewResourceId, autoCompleteLocationTextView);
    }

    @Override
    protected List<Location> getOptions(CharSequence constraint) {
        LocationController locationController = getMuzimaApplicationContext().getLocationController();
        try {
            return locationController.downloadLocationFromServerByName(constraint.toString());
        } catch (LocationController.LocationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Unable to download locations!", e);
        }
        return new ArrayList<>();
    }

    @Override
    protected String getOptionName(Location location) {
        return location.getName();
    }
}
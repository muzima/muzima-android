/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.encounters;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ObservationController;

import java.util.ArrayList;
import java.util.List;

abstract class ObservationsAdapter extends ListAdapter<Observation> {
    final ObservationController observationController;

    ObservationsAdapter(Context context, int textViewResourceId, ObservationController observationController){
        super(context, textViewResourceId);
        this.observationController = observationController;
    }

    abstract class ViewHolder {

        final LayoutInflater inflater;
        final List<LinearLayout> observationViewHolders;

        ViewHolder() {
            observationViewHolders = new ArrayList<>();
            inflater = LayoutInflater.from(getContext());
        }
    }
}

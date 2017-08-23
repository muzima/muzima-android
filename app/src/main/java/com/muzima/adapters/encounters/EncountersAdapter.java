/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import com.muzima.api.model.Encounter;
import com.muzima.controller.EncounterController;

import java.util.ArrayList;
import java.util.List;

public abstract class EncountersAdapter extends ListAdapter<Encounter> {
    private static final String TAG = "EncountersAdapter";
    protected EncounterController encounterController;

    public EncountersAdapter(Context context, int textViewResourceId, EncounterController encounterController) {
        super(context, textViewResourceId);
        this.encounterController = encounterController;

    }

    protected abstract class ViewHolder {

        protected LayoutInflater inflater;
        List<LinearLayout> encounterViewHolders;

        protected ViewHolder() {
            encounterViewHolders = new ArrayList<LinearLayout>();
            inflater = LayoutInflater.from(getContext());
        }
    }
}

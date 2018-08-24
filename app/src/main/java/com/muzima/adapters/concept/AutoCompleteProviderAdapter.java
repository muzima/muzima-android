/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import com.muzima.api.model.Provider;
import com.muzima.controller.ProviderController;

import java.util.ArrayList;
import java.util.List;


public class AutoCompleteProviderAdapter extends AutoCompleteBaseAdapter<Provider> {

    public AutoCompleteProviderAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteProviderTextView) {
        super(context, textViewResourceId, autoCompleteProviderTextView);
    }

    @Override
    protected List<Provider> getOptions(CharSequence constraint) {
        ProviderController providerController = getMuzimaApplicationContext().getProviderController();
        try {
            return providerController.downloadProviderFromServerByName(constraint.toString());
        } catch (ProviderController.ProviderDownloadException e) {
            Log.e(getClass().getSimpleName(), "Unable to download providers!", e);
        }
        return new ArrayList<>();
    }

    @Override
    protected String getOptionName(Provider provider) {
        return provider.getName();
    }
}

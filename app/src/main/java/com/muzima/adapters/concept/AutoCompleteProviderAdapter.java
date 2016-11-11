package com.muzima.adapters.concept;

import android.content.Context;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import com.muzima.api.model.Provider;
import com.muzima.controller.ProviderController;

import java.util.ArrayList;
import java.util.List;


public class AutoCompleteProviderAdapter extends AutoCompleteBaseAdapter<Provider> {
    private static final String TAG = AutoCompleteProviderAdapter.class.getSimpleName();

    public AutoCompleteProviderAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteProviderTextView) {
        super(context, textViewResourceId, autoCompleteProviderTextView);
    }

    @Override
    protected List<Provider> getOptions(CharSequence constraint) {
        ProviderController providerController = getMuzimaApplicationContext().getProviderController();
        try {
            return providerController.downloadProviderFromServerByName(constraint.toString());
        } catch (ProviderController.ProviderDownloadException e) {
            Log.e(TAG, "Unable to download providers!", e);
        }
        return new ArrayList<Provider>();
    }

    @Override
    protected String getOptionName(Provider provider) {
        return provider.getName();
    }
}

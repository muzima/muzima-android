package com.muzima.adapters.messaging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ArrayAdapter;

public class CountrySpinnerAdapter extends ArrayAdapter<String> {
    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     */
    public CountrySpinnerAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }
}

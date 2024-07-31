/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.concept;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.service.MuzimaSyncService;
import com.muzima.utils.StringUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.muzima.utils.Constants;

/**
 * Responsible to display auto-complete menu for Models.
 * @param <T> T can be of Type Concept, Cohort, Provider, Person e.t.c. Objects displayed in the auto-complete menu.
 */
public abstract class AutoCompleteBaseAdapter<T> extends ArrayAdapter<T> {

    protected WeakReference<MuzimaApplication> muzimaApplicationWeakReference;
    private final MuzimaSyncService muzimaSyncService;
    private String previousConstraint = null;
    private List<T> previousResult = null;
    private final AutoCompleteTextView autoCompleteTextView;

    public AutoCompleteBaseAdapter(Context context, int textViewResourceId, AutoCompleteTextView autoCompleteTextView) {
        super(context, textViewResourceId);
        this.autoCompleteTextView = autoCompleteTextView;
        muzimaApplicationWeakReference = new WeakReference<>((MuzimaApplication) context.getApplicationContext());
        muzimaSyncService = getMuzimaApplicationContext().getMuzimaSyncService();
    }

    public MuzimaApplication getMuzimaApplicationContext() {
        if (muzimaApplicationWeakReference == null) {
            return null;
        } else {
            return muzimaApplicationWeakReference.get();
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null && constraint.length() > 2) {

                    List<T> options;
                    if (hasResultStored(constraint)) {
                        options = filterOptionsLocally(constraint);
                    } else {
                        options = downloadOptions(constraint);
                    }
                    filterResults.values = options;
                    filterResults.count = options.size();
                }
                return filterResults;
            }

            private List<T> downloadOptions(CharSequence constraint) {
                List<T> options = new ArrayList<T>();
                Credentials credentials = new Credentials(getContext());
                MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();
                try {
                    if (muzimaSyncService.authenticate(credentials.getCredentialsArray(),false) == Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                        options = getOptions(constraint);
                        previousConstraint = constraint.toString();
                        previousResult = options;
                    } else {
                        Toast.makeText(getMuzimaApplicationContext(), muzimaApplicationContext.getString(R.string.error_authentication_fail), Toast.LENGTH_SHORT).show();
                    }

                    Log.i(getClass().getSimpleName(), "Downloaded: " + options.size());
                } catch (Throwable t) {
                    Log.e(getClass().getSimpleName(), "Unable to download options!", t);
                } finally {
                    muzimaApplicationContext.getMuzimaContext().closeSession();
                }
                return options;
            }

            protected List<T> filterOptionsLocally(CharSequence constraint) {
                List<T> result = new ArrayList<T>();
                for (T t : previousResult) {
                    if (getOptionName(t).toLowerCase().contains(constraint.toString().toLowerCase())) {
                        result.add(t);
                    }
                }
                return result;
            }

            private boolean hasResultStored(CharSequence constraint) {
                return previousConstraint != null && previousResult != null &&
                        constraint.toString().toLowerCase().contains(previousConstraint.toLowerCase());
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults results) {
                if (constraint != null && constraint.toString().equals(autoCompleteTextView.getText().toString())) {
                    List<T> optionList = (List<T>) results.values;
                    if (optionList != null && optionList.size() > 0) {
                        clear();
                        for (T c : optionList) {
                            add(c);
                        }
                        notifyDataSetChanged();
                    }
                    filterComplete(optionList != null ? optionList.size() : 0);
                }else{
                    clear();
                }
            }

            @Override
            public CharSequence convertResultToString(Object result) {
                if(result != null) {
                    return getOptionName((T) result);
                }

                return super.convertResultToString(null);
            }
        };
    }

    protected abstract List<T> getOptions(CharSequence constraint);

    private class ViewHolder {
        TextView name;
        TextView name_extra;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_option_autocomplete, parent, false);
            holder = new ViewHolder();
            holder.name = convertView.findViewById(R.id.option_autocomplete_name);
            holder.name_extra = convertView.findViewById(R.id.option_autocomplete_name_extra);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        T option = getItem(position);
        holder.name.setText(getOptionName(option));
        if(!StringUtils.isEmpty(getOptionNameExtra(option))) {
            holder.name_extra.setVisibility(View.VISIBLE);
            holder.name_extra.setText(getOptionNameExtra(option));
        }
        return convertView;
    }

    protected abstract String getOptionName(T option);

    protected abstract String getOptionNameExtra(T option);

    protected abstract void filterComplete(int count);

    protected void clearPreviousResult(){
        previousConstraint = null;
    }
}

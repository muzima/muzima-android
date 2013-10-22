/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.muzima.adapters.concept;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.service.MuzimaSyncService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;

public abstract class AutoCompleteBaseAdapter<T> extends ArrayAdapter<T> {

    private static final String TAG = AutoCompleteBaseAdapter.class.getSimpleName();
    protected WeakReference<MuzimaApplication> muzimaApplicationWeakReference;
    private final MuzimaSyncService muzimaSyncService;

    public AutoCompleteBaseAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        muzimaApplicationWeakReference = new WeakReference<MuzimaApplication>((MuzimaApplication) context);
        muzimaSyncService = getMuzimaApplicationContext().getMuzimaSyncService();
    }

    public MuzimaApplication getMuzimaApplicationContext() {
        if (muzimaApplicationWeakReference == null) {
            return null;
        } else {
            return muzimaApplicationWeakReference.get();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if (constraint != null && constraint.length() > 2) {
                    Credentials credentials = new Credentials(getContext());

                    MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();
                    List<T> options = new ArrayList<T>();
                    try {
                        if (muzimaSyncService.authenticate(credentials.getCredentialsArray()) == AUTHENTICATION_SUCCESS) {
                            options = getOptions(constraint);
                        } else {
                            Toast.makeText(getMuzimaApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                        }

                        Log.i(TAG, "Downloaded: " + options.size());
                    } catch (Throwable t) {
                        Log.e(TAG, "Unable to download cohorts!", t);
                    } finally {
                        muzimaApplicationContext.getMuzimaContext().closeSession();
                    }
                    filterResults.values = options;
                    filterResults.count = options.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults results) {
                List<T> cohortList = (List<T>) results.values;
                if (cohortList != null && cohortList.size() > 0) {
                    clear();
                    for (T c : cohortList) {
                        add(c);
                    }
                    notifyDataSetChanged();
                }
            }
        };
    }

    protected abstract List<T> getOptions(CharSequence constraint);

    private class ViewHolder {
        TextView name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_concept_autocomplete, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.concept_autocomplete_name);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        T option = getItem(position);
        holder.name.setText(getOptionDisplay(option));
        return convertView;
    }

    protected abstract String getOptionDisplay(T option);
}

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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.search.api.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Write brief description about the class here.
 */
public class AutoCompleteConceptAdapter extends ArrayAdapter<Concept> {

    private static final String TAG = AutoCompleteConceptAdapter.class.getSimpleName();
    protected WeakReference<MuzimaApplication> muzimaApplicationWeakReference;

    public AutoCompleteConceptAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        muzimaApplicationWeakReference = new WeakReference<MuzimaApplication>((MuzimaApplication) context);
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
                if (constraint != null && constraint.length() > 3) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                    String usernameKey = getContext().getResources().getString(R.string.preference_username);
                    String passwordKey = getContext().getResources().getString(R.string.preference_password);
                    String serverKey = getContext().getResources().getString(R.string.preference_server);
                    MuzimaApplication muzimaApplicationContext = getMuzimaApplicationContext();
                    List<Concept> concepts = new ArrayList<Concept>();
                    try {
                        muzimaApplicationContext.getMuzimaContext().openSession();
                        if (!muzimaApplicationContext.getMuzimaContext().isAuthenticated()) {
                            muzimaApplicationContext.getMuzimaContext().authenticate(
                                    settings.getString(usernameKey, StringUtil.EMPTY),
                                    settings.getString(passwordKey, StringUtil.EMPTY),
                                    settings.getString(serverKey, StringUtil.EMPTY));
                        }
                        ConceptController conceptController = muzimaApplicationContext.getConceptController();
                        List<Concept> downloadedConcepts = conceptController.downloadConceptsByName(constraint.toString());
                        List<Concept> savedConcepts = conceptController.getConceptByName(constraint.toString());
                        for (Concept savedConcept : savedConcepts) {
                            if (!downloadedConcepts.contains(savedConcept)) {
                                concepts.add(savedConcept);
                            }
                        }
                        concepts.addAll(downloadedConcepts);
                        Log.i(TAG, "Downloaded: " + concepts.size());
                    } catch (Throwable t) {
                        Log.e(TAG, "Unable to download concepts!", t);
                    } finally {
                        muzimaApplicationContext.getMuzimaContext().closeSession();
                    }
                    filterResults.values = concepts;
                    filterResults.count = concepts.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(final CharSequence constraint, final FilterResults results) {
                List<Concept> conceptList = (List<Concept>) results.values;
                if (conceptList != null && conceptList.size() > 0) {
                    clear();
                    for (Concept c : conceptList) {
                        add(c);
                    }
                    notifyDataSetChanged();
                }
            }
        };
    }

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
        Concept concept = getItem(position);
        holder.name.setText(concept.getName());
        return convertView;
    }
}

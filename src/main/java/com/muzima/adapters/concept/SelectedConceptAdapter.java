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
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.controller.ConceptController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.PreferenceHelper;
import com.muzima.utils.StringUtils;

import java.util.*;

import static com.muzima.utils.Constants.CONCEPT_PREF;
import static com.muzima.utils.Constants.CONCEPT_PREF_KEY;

/**
 * TODO: Write brief description about the class here.
 */
public class SelectedConceptAdapter extends ArrayAdapter<Concept> {
    private final String TAG = SelectedConceptAdapter.class.getSimpleName();
    protected ConceptController conceptController;

    public SelectedConceptAdapter(Context context, int textViewResourceId, ConceptController conceptController) {
        super(context, textViewResourceId);
        this.conceptController = conceptController;
    }

    private class ViewHolder {
        TextView name;
        TextView synonyms;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_concept_list, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.concept_name);
            holder.synonyms = (TextView) convertView.findViewById(R.id.concept_synonyms);
            convertView.setTag(holder);
        }
        holder = (ViewHolder) convertView.getTag();
        Concept concept = getItem(position);
        if (concept != null) {
            holder.name.setText(concept.getName());
            // get a synonym and then display the more text if there are more synonyms
            int counter = 0;
            List<String> synonyms = new ArrayList<String>();
            List<ConceptName> conceptNames = concept.getConceptNames();
            while (counter < conceptNames.size() && synonyms.size() < 1) {
                ConceptName conceptName = conceptNames.get(counter++);
                String name = conceptName.getName();
                if (!synonyms.contains(name) && !StringUtil.equals(name, concept.getName())) {
                    synonyms.add(name);
                }
            }
            StringBuilder synonymBuilder = new StringBuilder();
            synonymBuilder.append(StringUtils.getCommaSeparatedStringFromList(synonyms));
            if (conceptNames.size() > 2) {
                synonymBuilder.append(" (").append(conceptNames.size() - 2).append(" more.)");
            }
            holder.synonyms.setText(synonymBuilder.toString());
        }
        return convertView;
    }

    @Override
    public void add(final Concept concept) {
        super.add(concept);
        new BackgroundSaveTask().execute(concept);
    }

    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    public class BackgroundSaveTask extends AsyncTask<Concept, Void, Void> {

        @Override
        protected Void doInBackground(Concept... concepts) {
            List<Concept> conceptList = Arrays.asList(concepts);
            try {
                conceptController.saveConcepts(conceptList);
            } catch (ConceptController.ConceptSaveException e) {
                Log.w(TAG, "Exception occurred while saving concept to local data repository!", e);
            }

            new PreferenceHelper(getContext()).addConcepts(conceptList);

            return null;
        }
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Concept>> {

        @Override
        protected List<Concept> doInBackground(Void... voids) {
            List<Concept> concepts = new ArrayList<Concept>();
            SharedPreferences conceptPrefixPref = getContext().getSharedPreferences(CONCEPT_PREF, Context.MODE_PRIVATE);
            Set<String> stringSet = conceptPrefixPref.getStringSet(CONCEPT_PREF_KEY, new LinkedHashSet<String>());
            for (String conceptUuid : stringSet) {
                try {
                    concepts.add(conceptController.getConceptByUuid(conceptUuid));
                } catch (ConceptController.ConceptFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local concept!", e);
                }
            }
            Log.i(TAG, "#Concepts: " + concepts.size());
            return concepts;
        }

        @Override
        protected void onPostExecute(List<Concept> concepts) {
            if (concepts == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching concepts from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            clear();
            for (Concept concept : concepts) {
                add(concept);
            }
            notifyDataSetChanged();
        }
    }
}

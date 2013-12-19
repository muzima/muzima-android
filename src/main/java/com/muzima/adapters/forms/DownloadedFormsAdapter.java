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

package com.muzima.adapters.forms;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import com.muzima.R;
import com.muzima.controller.FormController;
import com.muzima.model.DownloadedForm;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;
import com.muzima.view.CheckedLinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to list all the downloaded forms.
 */
public class DownloadedFormsAdapter extends FormsAdapter<DownloadedForm> {
    private static final String TAG = "DownloadedFormsAdapter";
    private List<String> selectedFormsUuid = new ArrayList<String>();

    public DownloadedFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        highlightIfSelected(convertView, getItem(position));
        hideTagScroller(convertView);
        return convertView;
    }

    private void hideTagScroller(View convertView) {
        convertView.findViewById(R.id.tags_scroller).setVisibility(View.GONE);
    }

    private void highlightIfSelected(View convertView, DownloadedForm form) {
        if (selectedFormsUuid.contains(form.getFormUuid())) {
            setSelected(convertView, true);
        } else {
            setSelected(convertView, false);
        }
    }

    private void setSelected(View convertView, boolean selected) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            convertView.findViewById(R.id.form_name_layout).setActivated(selected);
            convertView.findViewById(R.id.form_description).setActivated(selected);
            convertView.findViewById(R.id.form_name).setActivated(selected);
        }
        ((CheckedLinearLayout) convertView.findViewById(R.id.form_name_layout)).setChecked(selected);
        ((CheckedTextView)convertView.findViewById(R.id.form_name)).setChecked(selected);
        ((CheckedTextView)convertView.findViewById(R.id.form_description)).setChecked(selected);

        convertView.findViewById(R.id.form_name_layout).setSelected(selected);
        convertView.findViewById(R.id.form_name).setSelected(selected);
        convertView.findViewById(R.id.form_description).setSelected(selected);
    }

    @Override
    protected int getFormItemLayout() {
        return R.layout.item_forms_list_selectable;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public void onListItemClick(int position) {
        DownloadedForm form = getItem(position);
        if (selectedFormsUuid.contains(form.getFormUuid())) {
            selectedFormsUuid.remove(form.getFormUuid());
        } else {
            selectedFormsUuid.add(form.getFormUuid());
        }
        notifyDataSetChanged();
    }

    public List<String> getSelectedForms() {
        return selectedFormsUuid;
    }

    public void clearSelectedForms() {
        selectedFormsUuid.clear();
    }

    /**
     * Responsible to fetch all the
     */
    public class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<DownloadedForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected List<DownloadedForm> doInBackground(Void... params) {
            DownloadedForms downloadedForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    downloadedForms = formsAdapter.getFormController().getAllDownloadedForms();
                    Log.i(TAG, "#Forms with templates: " + downloadedForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return downloadedForms;
        }
    }
}

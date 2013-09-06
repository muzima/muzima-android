package com.muzima.adapters.forms;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.ArrayList;
import java.util.List;

public class NewFormsAdapter extends FormsAdapter {
    private static final String TAG = "NewFormsAdapter";
    private List<String> selectedFormsUuid;

    public NewFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        selectedFormsUuid = new ArrayList<String>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        highlightIfSelected(convertView, getItem(position));
        return convertView;
    }

    private void highlightIfSelected(View convertView, Form form) {
        if (selectedFormsUuid.contains(form.getUuid())) {
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.listitem_state_pressed));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
        }
    }

    public void onListItemClick(int position) {
        Form form = getItem(position);
        if (selectedFormsUuid.contains(form.getUuid())) {
            selectedFormsUuid.remove(form.getUuid());
        } else {
            selectedFormsUuid.add(form.getUuid());
        }
        notifyDataSetChanged();
    }

    public List<String> getSelectedForms() {
        return selectedFormsUuid;
    }

    public void clearSelectedForms() {
        selectedFormsUuid.clear();
        notifyDataSetChanged();
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public static class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected List<Form> doInBackground(Void... voids) {
            List<Form> allForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    allForms = formsAdapter.getFormController().getAllFormByTags(getSelectedTagUuids());
                    Log.i(TAG, "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            return allForms;
        }
    }

}

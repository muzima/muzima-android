package com.muzima.adapters.forms;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.QueryTask;

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

        ViewHolder holder = (ViewHolder) convertView.getTag();
        Form form = getItem(position);
        addTags(holder, form);
        highlightIfSelected(convertView, form);
//        highlightIfDownloaded(convertView,form);
        return convertView;
    }

    private void addTags(ViewHolder holder, Form form) {
        Tag[] tags = form.getTags();
        if (tags.length > 0) {
            holder.tagsScroller.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            //add update tags
            for (int i = 0; i < tags.length; i++) {
                TextView textView = null;
                if (holder.tags.size() <= i) {
                    textView = newTextview(layoutInflater);
                    holder.addTag(textView);
                }
                textView = holder.tags.get(i);
                textView.setBackgroundColor(formController.getTagColor(tags[i].getUuid()));
                List<Tag> selectedTags = formController.getSelectedTags();
                if (selectedTags.isEmpty() || selectedTags.contains(tags[i])) {
                    textView.setText(tags[i].getName());
                } else {
                    textView.setText(StringUtil.EMPTY);
                }
            }

            //remove existing extra tags which are present because of recycled list view
            if (tags.length < holder.tags.size()) {
                List<TextView> tagsToRemove = new ArrayList<TextView>();
                for (int i = tags.length; i < holder.tags.size(); i++) {
                    tagsToRemove.add(holder.tags.get(i));
                }
                holder.removeTags(tagsToRemove);
            }
        } else {
            holder.tagsScroller.setVisibility(View.GONE);
        }
    }

    private void highlightIfSelected(View convertView, Form form) {
        if (selectedFormsUuid.contains(form.getUuid())) {
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.listitem_state_pressed));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
        }
    }

    private void highlightIfDownloaded(View convertView, Form form) {
        try {
            if (formController.isFormDownloaded(form)) {
                convertView.setBackgroundColor(Color.parseColor("#A8A8A8"));
            }
        } catch (FormController.FormFetchException e) {
            throw new RuntimeException(e);
        }
    }

    private TextView newTextview(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
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
        new BackgroundQueryTask(backgroundListQueryTaskListener).execute();
    }

    public class BackgroundQueryTask extends QueryTask {

        public BackgroundQueryTask(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
            super(backgroundListQueryTaskListener);
        }

        @Override
        protected List<Form> doInBackground(Void... voids) {
            List<Form> allForms = null;
            try {
                List<Tag> selectedTags = formController.getSelectedTags();
                List<String> tags = new ArrayList<String>();
                for (Tag selectedTag : selectedTags) {
                    tags.add(selectedTag.getUuid());
                }

                allForms = formController.getAllFormByTags(tags);
                Log.i(TAG, "#Forms: " + allForms.size());
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "Exception occurred while fetching local forms " + e);
            }
            return allForms;
        }

        @Override
        protected void onPostExecute(List<Form> forms) {
            if (forms == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching forms from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            NewFormsAdapter.this.clear();
            for (Form form : forms) {
                add(form);
            }
            notifyDataSetChanged();

            super.onPostExecute(forms);
        }
    }
}

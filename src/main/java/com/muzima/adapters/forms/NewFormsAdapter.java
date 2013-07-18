package com.muzima.adapters.forms;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class NewFormsAdapter extends FormsAdapter {
    private static final String TAG = "NewFormsAdapter";
    private List<Form> selectedForms;

    public NewFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        selectedForms = new ArrayList<Form>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        ViewHolder holder = (ViewHolder) convertView.getTag();
        Form form = getItem(position);
        addTags(holder, form);
        highlightIfSelected(convertView, form);
        return convertView;
    }

    private void highlightIfSelected(View convertView, Form form) {
        if(selectedForms.contains(form)){
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.listitem_state_pressed));
        }else{
            convertView.setBackgroundColor(Color.WHITE);
        }
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
                    textView = newTextview(layoutInflater, tags[i]);
                    holder.tags.add(textView);
                    holder.tagsLayout.addView(textView);
                }
                textView = holder.tags.get(i);
                textView.setBackgroundColor(formController.getTagColor(tags[i].getUuid()));
                List<Tag> selectedTags = formController.getSelectedTags();
                if(selectedTags.isEmpty() || selectedTags.contains(tags[i])){
                    textView.setText(tags[i].getName());
                }else{
                    textView.setText(StringUtil.EMPTY);
                }
            }

        //remove already existing extra tags
        if (tags.length < holder.tags.size()) {
            for (int i = tags.length; i < holder.tags.size(); i++) {
                holder.tagsLayout.removeView(holder.tags.get(i));
                holder.tags.remove(i);
            }
        }
        }else{
            holder.tagsScroller.setVisibility(View.GONE);
        }
    }

    private TextView newTextview(LayoutInflater layoutInflater, Tag tag) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    public void onListItemClick(int position) {
        Form form = getItem(position);
        if(selectedForms.contains(form)){
            selectedForms.remove(form);
        }else{
            selectedForms.add(form);
        }
        notifyDataSetChanged();
    }

    public List<Form> getSelectedForms() {
        return selectedForms;
    }

    public void clearSelectedForms(){
        selectedForms.clear();
        notifyDataSetChanged();
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Form>> {

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
            NewFormsAdapter.this.clear();
            for (Form form : forms) {
                add(form);
            }
            notifyDataSetChanged();
            notifyEmptyDataListener(forms.size() == 0);
        }
    }
}

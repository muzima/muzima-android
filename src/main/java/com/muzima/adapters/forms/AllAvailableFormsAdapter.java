package com.muzima.adapters.forms;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.MuzimaSyncService;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.ArrayList;
import java.util.List;

public class AllAvailableFormsAdapter extends FormsAdapter<AvailableForm> implements TagsListAdapter.TagsChangedListener{
    private static final String TAG = "AllAvailableFormsAdapter";
    private List<String> selectedFormsUuid;
    private final MuzimaSyncService muzimaSyncService;


    public AllAvailableFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        selectedFormsUuid = new ArrayList<String>();
        muzimaSyncService = ((MuzimaApplication)(getContext().getApplicationContext())).getMuzimaSyncService();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        highlightIfSelected(convertView, getItem(position));
        addTags((ViewHolder) convertView.getTag(), getItem(position));
        markIfDownloaded(convertView, getItem(position));

        return convertView;
    }

    private void markIfDownloaded(View convertView, AvailableForm form) {
        ImageView imageView = (ImageView) convertView.findViewById(R.id.downloadImg);
        if(form.isDownloaded()){
            imageView.setVisibility(View.VISIBLE);
        }else{
            imageView.setVisibility(View.GONE);
        }
    }

    protected void addTags(ViewHolder holder, AvailableForm form) {
        Tag[] tags = form.getTags();
        if (tags.length > 0) {
            holder.tagsScroller.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            //add update tags
            for (int i = 0; i < tags.length; i++) {
                TextView textView = null;
                if (holder.tags.size() <= i) {
                    textView = newTextView(layoutInflater);
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

    private TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private void highlightIfSelected(View convertView, AvailableForm form) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (selectedFormsUuid.contains(form.getFormUuid())) {
                setSelected(convertView, true);
            } else {
                setSelected(convertView, false);
            }
        } else {
//            TODO for FROYO
            if (selectedFormsUuid.contains(form.getFormUuid())) {
                convertView.setBackgroundColor(getContext().getResources().getColor(R.color.listitem_state_pressed));
            } else {
                convertView.setBackgroundColor(getContext().getResources().getColor(R.color.theme_color));
            }
        }
    }

    private void setSelected(View convertView, boolean selected) {
        convertView.findViewById(R.id.form_name_layout).setSelected(selected);
        convertView.findViewById(R.id.form_name_layout).setActivated(selected);
        convertView.findViewById(R.id.form_name).setSelected(selected);
        convertView.findViewById(R.id.form_name).setActivated(selected);
        convertView.findViewById(R.id.form_description).setSelected(selected);
        convertView.findViewById(R.id.form_description).setActivated(selected);
        convertView.findViewById(R.id.tags_scroller).setSelected(selected);
        convertView.findViewById(R.id.tags_scroller).setActivated(selected);
    }

    public void onListItemClick(int position) {
        AvailableForm form = getItem(position);
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
        notifyDataSetChanged();
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public void downloadFormTemplatesAndReload() {
        new DownloadBackgroundQueryTask(this).execute();
    }

    @Override
    public void onTagsChanged() {
        reloadData();
    }

    public boolean hasRegistrationFormSelected() {
        for (int i = 0; i < getCount(); i++) {
            AvailableForm item = getItem(i);
            if (item.isRegistrationForm() && selectedFormsUuid.contains(getItem(i).getFormUuid())) {
                return true;
            }
        }
        return false;
    }

    public class DownloadBackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        public DownloadBackgroundQueryTask(FormsAdapter adapter) {
            super(adapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            AvailableForms allForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    muzimaSyncService.downloadForms();
                    allForms = formsAdapter.getFormController().getAvailableFormByTags(getSelectedTagUuids());
                    selectRegistrationForm(allForms);
                    Log.i(TAG, "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return allForms;
        }

        private void selectRegistrationForm(AvailableForms allForms) {
            for (AvailableForm form : allForms) {
                if(form.isRegistrationForm()) {
                    selectedFormsUuid.add(form.getFormUuid());
                    Log.d(TAG, "isRegistrationForm size:" + selectedFormsUuid.size());
                }
            }
        }
    }


    public class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            AvailableForms allForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    allForms = formsAdapter.getFormController().getAvailableFormByTags(getSelectedTagUuids());
                    Log.i(TAG, "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms " + e);
                }
            }
            return allForms;
        }

    }

}

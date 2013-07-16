package com.muzima.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.controller.FormController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.muzima.utils.CustomColor.getRandomColor;

public class NewFormsAdapter extends FormsListAdapter<Form> {
    private static final String TAG = "NewFormsAdapter";
    private FormController formController;

    public NewFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId);
        this.formController = formController;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_forms_list, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView) convertView
                    .findViewById(R.id.form_name);
            holder.description = (TextView) convertView
                    .findViewById(R.id.form_description);
            holder.tagsScroller = (HorizontalScrollView) convertView.findViewById(R.id.tags_scroller);
            holder.tagsLayout = (LinearLayout) convertView.findViewById(R.id.tags);
            holder.tags = new ArrayList<TextView>();
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Form form = getItem(position);

        holder.name.setText(form.getName());
        holder.name.setTypeface(Fonts.roboto_bold(getContext()));

        String description = form.getDescription();
        if (StringUtils.isEmpty(description)) {
            description = "No description available";
        }
        holder.description.setText(description);
        holder.name.setTypeface(Fonts.roboto_medium(getContext()));

        addTags(holder, form);

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
                    textView = newTextview(layoutInflater, tags[i]);
                    holder.tags.add(textView);
                    holder.tagsLayout.addView(textView);
                }
                textView = holder.tags.get(i);
                textView.setBackgroundColor(formController.getTagColor(tags[i].getUuid()));
                if(!formController.hasSelectedTags() || formController.isTagSelected(tags[i])){
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

    private static class ViewHolder {
        TextView name;
        TextView description;
        HorizontalScrollView tagsScroller;
        LinearLayout tagsLayout;
        List<TextView> tags;
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

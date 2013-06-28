package com.muzima.view;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.db.Html5FormDataSource;
import com.muzima.domain.Html5Form;
import com.muzima.utils.Fonts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Html5FormsAdapter extends ArrayAdapter<Html5Form> {

    private Html5FormDataSource html5FormDataSource;
    private Map<String, Integer> tagColors;

    public Html5FormsAdapter(Context context, int textViewResourceId, Html5FormDataSource html5FormDataSource) {
        super(context, textViewResourceId);
        this.html5FormDataSource = html5FormDataSource;
        tagColors = new HashMap<String, Integer>();
        new BackgroundQueryTask().execute();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.form_list_item, parent, false);
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

        Html5Form form = getItem(position);

        holder.name.setText(form.getName());
        holder.name.setTypeface(Fonts.roboto_bold(getContext()));

        String description = form.getDescription();
        if (description.equals("")) {
            description = "No description available";
        }
        holder.description.setText(description);
        holder.name.setTypeface(Fonts.roboto_medium(getContext()));

        addTags(holder, form);

        return convertView;
    }

    private void addTags(ViewHolder holder, Html5Form form) {
        List<String> tags = form.getTags();
        if (tags.size() > 0) {
            holder.tagsScroller.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            for (int i = 0; i < tags.size(); i++) {
                TextView textView = null;
                if (holder.tags.size() <= i) {
                    textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
                    holder.tags.add(textView);
                    holder.tagsLayout.addView(textView);
                }
                textView = holder.tags.get(i);
                textView.setText(tags.get(i));
                textView.setBackgroundColor(getTagColor(tags.get(i)));
            }
            if (tags.size() < holder.tags.size()) {
                for (int i = tags.size(); i < holder.tags.size(); i++) {
                    holder.tagsLayout.removeView(holder.tags.get(i));
                    holder.tags.remove(i);
                }
            }
        } else {
            holder.tagsScroller.setVisibility(View.GONE);
        }
    }

    private int getTagColor(String tag) {
        if(tagColors.get(tag) == null){
            tagColors.put(tag, randomColor());
        }
        return tagColors.get(tag);
    }

    public static Integer randomColor() {
        Random random = new Random();
        return Color.rgb(100 + random.nextInt(150), 100 + random.nextInt(150), 100 + random.nextInt(150));
    }

    public void dataSetChanged() {
        new BackgroundQueryTask().execute();
    }

    private static class ViewHolder {
        TextView name;
        TextView description;
        HorizontalScrollView tagsScroller;
        LinearLayout tagsLayout;
        List<TextView> tags;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Html5Form>> {

        @Override
        protected List<Html5Form> doInBackground(Void... voids) {
            List<Html5Form> allForms = html5FormDataSource.getAllForms();
            return allForms;
        }

        @Override
        protected void onPostExecute(List<Html5Form> html5Forms) {
            clear();
            for (Html5Form html5Form : html5Forms) {
                add(html5Form);
            }
            notifyDataSetChanged();
        }
    }
}

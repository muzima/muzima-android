package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.utils.Fonts;

/**
 * Responsible for displaying Cohorts as list.
 */
public abstract class CohortsAdapter extends ListAdapter<Cohort> {
    protected CohortController cohortController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public CohortsAdapter(Context context, int textViewResourceId, CohortController cohortController) {
        super(context, textViewResourceId);
        this.cohortController = cohortController;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_cohorts_list, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setTextToName(getItem(position).getName());
        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public class ViewHolder {
        private CheckedTextView name;
        private ImageView downloadedImage;

        public ViewHolder(View convertView) {
            this.downloadedImage = (ImageView) convertView.findViewById(R.id.downloadImg);
            this.name = (CheckedTextView) convertView
                    .findViewById(R.id.cohort_name);
        }

        public void displayDownloadImage() {
            downloadedImage.setVisibility(View.VISIBLE);
        }

        public void setTextToName(String text) {
            name.setText(text);
            name.setTypeface(Fonts.roboto_medium(getContext()));
        }
    }
}

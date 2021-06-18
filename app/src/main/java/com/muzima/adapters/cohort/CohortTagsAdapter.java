package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.muzima.R;

import java.util.List;

public class CohortTagsAdapter extends ArrayAdapter<String> {

    private List<String> tags;
    private Context context;
    public CohortTagsAdapter(@NonNull Context context, int resource, List<String> tags) {
        super(context, resource);
        this.tags = tags;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_cohort_tags_layout,null);
        TextView titleTextView = view.findViewById(R.id.item_cohort_tag_text_view);
        titleTextView.setText(tags.get(position));
        return view;
    }

    @Override
    public int getCount() {
        return tags.size();
    }
}

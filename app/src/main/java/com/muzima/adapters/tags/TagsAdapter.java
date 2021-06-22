package com.muzima.adapters.tags;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.ViewHolder>{

    private List<Tag> tagList = new ArrayList<>();

    public TagsAdapter(Tag[] tagList) {
        this.tagList.addAll(Arrays.asList(tagList));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cohort_tags_layout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagsAdapter.ViewHolder holder, int position) {
        holder.titleTextView.setText(tagList.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return tagList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_cohort_tag_text_view);
        }
    }
}

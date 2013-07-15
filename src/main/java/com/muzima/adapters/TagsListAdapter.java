package com.muzima.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.domain.Tag;
import com.muzima.listeners.EmptyListListener;
import com.muzima.utils.CustomColor;

import java.util.ArrayList;
import java.util.List;

public class TagsListAdapter extends ArrayAdapter<Tag> {
    List<Tag> tags;

    public TagsListAdapter(Context context, int textViewResourceId, List<Tag> tags) {
        super(context, textViewResourceId);
        tags = new ArrayList<Tag>();
        tags.add(new Tag("All", Color.parseColor("#444444")));
        tags.add(new Tag("Patient", CustomColor.BLESSING.getColor()));
        tags.add(new Tag("Registration", CustomColor.ALLERGIC_RED.getColor()));
        tags.add(new Tag("PMTCT", CustomColor.POOLSIDE.getColor()));
        tags.add(new Tag("Observation", CustomColor.GRUBBY.getColor()));
        tags.add(new Tag("Natal Care", CustomColor.BLUSH.getColor()));

        addAll(tags);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_tags_list, parent, false);
            holder = new ViewHolder();
            holder.indicator = convertView.findViewById(R.id.tag_indicator);
            holder.name = (TextView) convertView
                    .findViewById(R.id.tag_name);
            holder.icon = (ImageView) convertView
                    .findViewById(R.id.tag_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.indicator.setBackgroundColor(getItem(position).getColor());
        holder.name.setText(getItem(position).getName());
        holder.icon.setBackgroundColor(getItem(position).getColor());

        return convertView;
    }

    private static class ViewHolder {
        View indicator;
        TextView name;
        ImageView icon;
    }
}

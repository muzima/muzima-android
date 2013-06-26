package com.muzima.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.domain.Form;
import com.muzima.utils.Fonts;

public class FormsAdapter extends ArrayAdapter<Form> {

    public FormsAdapter(Context context, int textViewResourceId,
                        Form[] forms) {
        super(context, textViewResourceId, forms);
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
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Form form = getItem(position);

        holder.name.setText(form.getName());
        holder.name.setTypeface(Fonts.roboto_bold(getContext()));

        holder.description.setText(form.getDescription());
        holder.name.setTypeface(Fonts.roboto_medium(getContext()));

        return convertView;
    }

    private static class ViewHolder {
        TextView name;
        TextView description;
    }
}

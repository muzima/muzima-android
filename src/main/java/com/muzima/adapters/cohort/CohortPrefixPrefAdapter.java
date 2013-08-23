package com.muzima.adapters.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;

import java.util.HashSet;
import java.util.Set;

import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF_KEY;

public class CohortPrefixPrefAdapter extends ListAdapter<String> {

    private PrefixClickListener prefixClickListener;

    public CohortPrefixPrefAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        reloadData();
    }

    public void setPrefixClickListener(PrefixClickListener prefixClickListener) {
        this.prefixClickListener = prefixClickListener;
    }

    @Override
    public void reloadData() {
        clear();
        SharedPreferences cohortPrefixPref = getContext().getSharedPreferences(COHORT_PREFIX_PREF, Context.MODE_PRIVATE);
        Set<String> stringSet = cohortPrefixPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>());
        addAll(stringSet);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_cohorts_list_pref, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) convertView
                    .findViewById(R.id.prefix);
            holder.text.setOnClickListener(new OnPrefixChangeListener(position));

            holder.deleteButton = (FrameLayout)convertView.findViewById(R.id.delete_button);
            holder.deleteButton.setOnClickListener(new OnPrefixDeleteListener(position));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(getItem(position));
        return convertView;
    }

    protected static class ViewHolder {
        TextView text;
        FrameLayout deleteButton;
    }

    public static interface PrefixClickListener{
        public void onDeletePrefixClick(String prefix);
        public void onChangePrefixClick(String prefix);
    }

    private class OnPrefixChangeListener implements View.OnClickListener {
        private int position;
        OnPrefixChangeListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(prefixClickListener != null){
                prefixClickListener.onChangePrefixClick(getItem(position));
            }
        }
    }

    private class OnPrefixDeleteListener implements View.OnClickListener {
        private int position;
        OnPrefixDeleteListener(int position){
            this.position = position;
        }
        @Override
        public void onClick(View view) {
            if(prefixClickListener != null){
                prefixClickListener.onDeletePrefixClick(getItem(position));
            }
        }
    }
}

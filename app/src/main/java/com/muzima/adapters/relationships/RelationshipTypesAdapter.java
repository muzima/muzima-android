/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.relationships;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;

import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.RelationshipType;
import com.muzima.controller.RelationshipController;
import com.muzima.model.relationship.RelationshipTypeWrap;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RelationshipTypesAdapter extends ListAdapter<RelationshipTypeWrap> {
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final RelationshipController relationshipController;

    public RelationshipTypesAdapter(Activity activity, int textViewResourceId, RelationshipController relationshipController) {
        super(activity, textViewResourceId);
        this.relationshipController = relationshipController;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }


    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent)
    {
        View view = getCustomView(position, convertView, parent);
        return view;
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        RelationshipTypeWrap relationshipTypeWrap = getItem(position);
        Context context = getContext();
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_simple_spinner, parent, false);
            convertView.setClickable(false);
            convertView.setFocusable(false);
            holder = new ViewHolder();
            holder.tvRelationshipType = convertView.findViewById(R.id.item1);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (relationshipTypeWrap != null) {
            holder.tvRelationshipType.setText(relationshipTypeWrap.getName());
        }

        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    class ViewHolder {
        final LayoutInflater inflater;
        final List<LinearLayout> viewHolders;

        ViewHolder() {
            viewHolders = new ArrayList<>();
            inflater = LayoutInflater.from(getContext());
        }

        TextView tvRelationshipType;
    }

    private class BackgroundQueryTask extends MuzimaAsyncTask<String, Void, List<RelationshipTypeWrap>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<RelationshipTypeWrap> doInBackground(String... params) {
            List<RelationshipTypeWrap> relationshipTypeWraps = new ArrayList<>();
            List<RelationshipType> relationshipTypes;
            try {
                relationshipTypes = relationshipController.getAllRelationshipTypes();

                for (RelationshipType relationshipType : relationshipTypes) {
                    RelationshipTypeWrap typeWrap = new RelationshipTypeWrap(relationshipType.getUuid(), relationshipType.getAIsToB(), "A", relationshipType);
                    relationshipTypeWraps.add(typeWrap);

                    if (!StringUtils.equalsIgnoreCase(relationshipType.getAIsToB(), relationshipType.getBIsToA())) {
                        typeWrap = new RelationshipTypeWrap(relationshipType.getUuid(), relationshipType.getBIsToA(), "B", relationshipType);
                        relationshipTypeWraps.add(typeWrap);
                    }
                }
            }catch(RelationshipController.RetrieveRelationshipTypeException e){
                Log.e(this.getClass().getSimpleName(),"Could not get relationship types",e);
            }

            return relationshipTypeWraps;
        }

        @Override
        protected void onPostExecute(List<RelationshipTypeWrap> relationshipTypeWraps){
            if(relationshipTypeWraps==null){
                Toast.makeText(getContext(),getContext().getString(R.string.error_relationship_type_load),Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(sortNameAscending(relationshipTypeWraps));
            notifyDataSetChanged();

        }

        @Override
        protected void onBackgroundError(Exception e) {

        }

        private List<RelationshipTypeWrap> sortNameAscending(List<RelationshipTypeWrap> relationshipTypeWraps) {
            Collections.sort(relationshipTypeWraps);
            relationshipTypeWraps.add(0, new RelationshipTypeWrap(null, "Choose . . .", null, null));
            return relationshipTypeWraps;
        }
    }
}

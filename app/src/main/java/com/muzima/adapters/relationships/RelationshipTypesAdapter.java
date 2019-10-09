/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.relationships;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.RelationshipType;
import com.muzima.controller.RelationshipController;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RelationshipTypesAdapter extends ListAdapter<RelationshipType> {
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

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        RelationshipType relationshipType = getItem(position);
        Context context = getContext();
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_relationship_type, parent, false);
            convertView.setClickable(false);
            convertView.setFocusable(false);
            holder = new ViewHolder();
            holder.btnAIsToB = convertView.findViewById(R.id.aIsToB);
            holder.btnBIsToA = convertView.findViewById(R.id.bIsToA);
            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (relationshipType != null) {

            holder.btnAIsToB.setText(relationshipType.getAIsToB());

            if (StringUtils.equalsIgnoreCase(relationshipType.getAIsToB(), relationshipType.getBIsToA())) {
                holder.btnBIsToA.setVisibility(View.GONE);
            } else {
                holder.btnBIsToA.setText(relationshipType.getBIsToA());
            }
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

        Button btnAIsToB;
        Button btnBIsToA;
    }

    private class BackgroundQueryTask extends AsyncTask<String, Void, List<RelationshipType>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<RelationshipType> doInBackground(String... params) {
            List<RelationshipType> relationshipTypes = null;
            try {
               relationshipTypes = relationshipController.getAllRelationshipTypes();
            }catch(RelationshipController.RetrieveRelationshipTypeException e){
                Log.e(this.getClass().getSimpleName(),"Could not get relationship types",e);
            }

            return relationshipTypes;
        }

        @Override
        protected void onPostExecute(List<RelationshipType> relationshipTypes){
            if(relationshipTypes==null){
                Toast.makeText(getContext(),getContext().getString(R.string.error_relationship_type_load),Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(relationshipTypes);
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }
}
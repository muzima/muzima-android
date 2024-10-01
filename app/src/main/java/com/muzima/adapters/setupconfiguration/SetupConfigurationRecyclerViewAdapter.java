/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.setupconfiguration;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.service.ActiveConfigPreferenceService;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SetupConfigurationRecyclerViewAdapter extends RecyclerView.Adapter<SetupConfigurationRecyclerViewAdapter.ViewHolder> {
    private Context context;
    private List<SetupConfiguration> setupConfigurationList;
    private List<SetupConfiguration> itemsCopy = new ArrayList<>();
    private ArrayList<String> selectedConfigsUuids = new ArrayList<>();
    private OnSetupConfigurationClickedListener onSetupConfigurationClickedListener;
    private boolean enableMultiSelect;

    public SetupConfigurationRecyclerViewAdapter(Context context, List<SetupConfiguration> setupConfigurationList, OnSetupConfigurationClickedListener clickedListener) {
        this.context = context;
        this.setupConfigurationList = setupConfigurationList;
        this.onSetupConfigurationClickedListener = clickedListener;
        setItemsCopy(setupConfigurationList);
    }

    public ArrayList<String> getSelectedConfigs() {
        return selectedConfigsUuids;
    }

    public void setItemsCopy(List<SetupConfiguration> itemsCopy) {
        this.itemsCopy = itemsCopy;
    }

    public void setEnableMultiSelect(boolean enableMultiSelect){
        this.enableMultiSelect = enableMultiSelect;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setup_configs_list, parent, false);
        return new ViewHolder(view, onSetupConfigurationClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SetupConfigurationRecyclerViewAdapter.ViewHolder holder, int position) {
        SetupConfiguration setupConfiguration = setupConfigurationList.get(position);
        holder.nameTextView.setText(setupConfiguration.getName());
        holder.descriptionTextView.setText(setupConfiguration.getDescription());
        ActiveConfigPreferenceService service = new ActiveConfigPreferenceService((MuzimaApplication) context.getApplicationContext());
        String activeConfigUuid = service.getActiveConfigUuid();
        if (selectedConfigsUuids.contains(setupConfiguration.getUuid()) ||
                !StringUtils.isEmpty(activeConfigUuid) && StringUtils.equals(activeConfigUuid, setupConfiguration.getUuid())) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_blue));
            holder.cardView.setChecked(true);
        } else {
            holder.cardView.setChecked(false);

            boolean isLightModeOn = MuzimaPreferences.getBooleanPreference(context, context.getResources().getString(R.string.preference_light_mode), false);;
            if (!isLightModeOn) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_black));
            } else
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_background));
        }
    }

    @Override
    public int getItemCount() {
        return setupConfigurationList.size();
    }

    public void filterItems(String searchTerm) {
        List<SetupConfiguration> filteredSetupConfigurations = new ArrayList<>();
        for (SetupConfiguration setupConfiguration : itemsCopy) {
            if (setupConfiguration.getName() != null && setupConfiguration.getName().toLowerCase().contains(searchTerm.toLowerCase())
                    || setupConfiguration.getDescription() != null && setupConfiguration.getDescription().toLowerCase().contains(searchTerm.toLowerCase())) {
                filteredSetupConfigurations.add(setupConfiguration);
            }
        }

        if (!filteredSetupConfigurations.isEmpty()) {
            setupConfigurationList.clear();
            notifyDataSetChanged();
            setupConfigurationList.addAll(filteredSetupConfigurations);
            notifyDataSetChanged();
        }
    }
    public void toggleSelection(View view, int position){
        MaterialCardView cardView = (MaterialCardView) view;
        cardView.toggle();
        boolean selected = cardView.isChecked();
        SetupConfiguration configuration = setupConfigurationList.get(position);

        if (selected && !selectedConfigsUuids.contains(configuration.getUuid())) {
            if(!enableMultiSelect)
                selectedConfigsUuids.clear();

            selectedConfigsUuids.add(configuration.getUuid());
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_blue));
        } else if (!selected && selectedConfigsUuids.contains(configuration.getUuid())) {
            selectedConfigsUuids.remove(configuration.getUuid());
            boolean isLightModeOn = MuzimaPreferences.getBooleanPreference(context, context.getResources().getString(R.string.preference_light_mode), false);;
            if (!isLightModeOn) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_black));
            } else
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_background));
        }
    }

    public SetupConfiguration getConfig(int position){
        return setupConfigurationList.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final MaterialCardView cardView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final OnSetupConfigurationClickedListener clickedListener;

        public ViewHolder(@NonNull View itemView, OnSetupConfigurationClickedListener clickedListener) {
            super(itemView);
            this.cardView = itemView.findViewById(R.id.setup_configuration_container);
            this.nameTextView = itemView.findViewById(R.id.config_name);
            this.descriptionTextView = itemView.findViewById(R.id.config_description);
            this.clickedListener = clickedListener;

            this.cardView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            this.clickedListener.onSetupConfigClicked(view,getAdapterPosition());
        }
    }

    public interface OnSetupConfigurationClickedListener {
        void onSetupConfigClicked(View view, int position);
    }
}

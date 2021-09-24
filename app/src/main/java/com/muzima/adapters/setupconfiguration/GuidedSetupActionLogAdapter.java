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
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.model.SetupActionLogModel;
import com.muzima.utils.Constants;
import com.muzima.utils.ThemeUtils;

import org.apache.commons.lang.StringUtils;

import java.util.Locale;

public class GuidedSetupActionLogAdapter extends ListAdapter<SetupActionLogModel> {
    private Context context;

    public GuidedSetupActionLogAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.context = context;
    }

    @Override
    public void reloadData() {
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_guided_setup_action_log, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.setSetupAction(getItem(position).getSetupAction());
        holder.setSetupActionResult(getItem(position).getSetupActionResult(), getItem(position).getSetupActionResultStatus());
        holder.setSetupActionResultStatus(getItem(position).getSetupActionResultStatus());
        return convertView;
    }

    class ViewHolder {
        private final TextView setupActionResult;
        private final ImageView statusImageView;

        ViewHolder(View convertView) {
            setupActionResult = convertView.findViewById(R.id.setup_action_result);
            statusImageView = convertView.findViewById(R.id.item_guided_setup_status_image_view);
        }

        void setSetupAction(String text) {
            setupActionResult.setText(text);
        }

        void setSetupActionResult(String text, String actionResultText) {
            if (StringUtils.isEmpty(actionResultText) && StringUtils.equals(text, Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG))
                setupActionResult.setText(String.format(Locale.getDefault(), "✔ %s", text));
            else
                setupActionResult.setText(text);
        }

        void setSetupActionResultStatus(String text) {
            //update progress here
            if (!StringUtils.isEmpty(text) && StringUtils.equals(text, Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG)) {
                setupActionResult.setTextColor(Color.RED);
                //setupActionResult.setText(String.format("%s: ", (getContext().getString(R.string.general_fail)).toUpperCase()));
                statusImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_close));
            } else if (!StringUtils.isEmpty(text) && StringUtils.equals(text, Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG)) {
                if (ThemeUtils.getInstance().isLightModeSettingEnabled(context.getApplicationContext()))
                    setupActionResult.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_blue));
                else
                    setupActionResult.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_white));
                statusImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_tick));
                // setupActionResult.setText(String.format("%s: ", (getContext().getString(R.string.general_ok)).toUpperCase()));
            }
        }
    }

}

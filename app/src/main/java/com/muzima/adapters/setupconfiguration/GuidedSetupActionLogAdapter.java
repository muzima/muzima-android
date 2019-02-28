/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.setupconfiguration;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.model.SetupActionLogModel;
import com.muzima.utils.Constants;
import com.muzima.utils.Fonts;
import org.apache.commons.lang.StringUtils;

public class GuidedSetupActionLogAdapter extends ListAdapter<SetupActionLogModel> {
    public GuidedSetupActionLogAdapter(Context context, int textViewResourceId){
        super(context, textViewResourceId);
    }

    @Override
    public void reloadData(){}


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
        holder.setSetupActionResult(getItem(position).getSetupActionResult());
        holder.setSetupActionResultStatus(getItem(position).getSetupActionResultStatus());
        return convertView;
    }
    class ViewHolder {
        private final TextView setupAction;
        private final TextView setupActionResult;
        private final TextView setupActionResultStatus;

        ViewHolder(View convertView) {
            setupAction = convertView
                    .findViewById(R.id.setup_action);
            setupActionResult = convertView
                    .findViewById(R.id.setup_action_result);
            setupActionResultStatus = convertView
                    .findViewById(R.id.setup_action_result_status);
        }

        void setSetupAction(String text) {
            setupAction.setText(text);
            setupAction.setTypeface(Fonts.roboto_medium(getContext()));
        }

        void setSetupActionResult(String text) {
            setupActionResult.setText(text);
            setupActionResult.setTypeface(Fonts.roboto_medium(getContext()));
        }

        void setSetupActionResultStatus(String text){
            if(!StringUtils.isEmpty(text) && StringUtils.equals(text, Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG)){
                setupActionResultStatus.setTextColor(Color.RED);
                setupActionResult.setTextColor(Color.RED);
                setupActionResultStatus.setText(String.format("%s: ", (getContext().getString(R.string.general_fail)).toUpperCase()));
            } else if(!StringUtils.isEmpty(text) && StringUtils.equals(text, Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG)){
                setupActionResultStatus.setTextColor(ContextCompat.getColor(getContext(),R.color.primary_blue));
                setupActionResult.setTextColor(ContextCompat.getColor(getContext(),R.color.primary_blue));
                setupActionResultStatus.setText(String.format("%s: ", (getContext().getString(R.string.general_ok)).toUpperCase()));
            }
        }
    }

}

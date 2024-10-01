/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.reports;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.model.ProviderReportStatistic;

import java.util.List;

public class SummaryStatisticAdapter extends RecyclerView.Adapter {
    private List<ProviderReportStatistic> individualProviderStatistics;
    private Context context;

    public SummaryStatisticAdapter(List<ProviderReportStatistic> individualProviderStatistics, Context context){
        this.individualProviderStatistics = individualProviderStatistics;
        this.context = context;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_summary_statistic_1, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder)holder;
        ProviderReportStatistic statistic = individualProviderStatistics.get(position);
        viewHolder.summaryStatisticTitle.setText(statistic.getStatisticTitle()+" ("+statistic.getAchievement()+"/"+statistic.getExpectedAchievement()+")");
        viewHolder.summaryStatisticHint.setText(statistic.getStatisticHint());

        int achievementRate = statistic.getExpectedAchievement() == 0? 0 : statistic.getAchievement()*100/statistic.getExpectedAchievement();
        viewHolder.summaryStatisticProgressText.setText(achievementRate+"%");

        viewHolder.summaryStatisticProgress.setProgress(achievementRate);
        viewHolder.summaryStatisticProgress.setSecondaryProgress(100);

        Drawable drawable = context.getResources().getDrawable(R.drawable.circular_progress);
        viewHolder.summaryStatisticProgress.setProgressDrawable(drawable);
        viewHolder.summaryStatisticProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor(statistic.getSummaryColorCode())));

    }

    @Override
    public int getItemCount() {
        return individualProviderStatistics.size();
    }

    public ProviderReportStatistic getReportStatistic(int position){
        if(position < 0 || position >= getItemCount()){
            return null;
        }
        return individualProviderStatistics.get(position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public final ProgressBar summaryStatisticProgress;
        public final TextView summaryStatisticProgressText;
        public final TextView summaryStatisticTitle;
        public final TextView summaryStatisticHint;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            summaryStatisticProgress = itemView.findViewById(R.id.summary_statistic_progress);
            summaryStatisticProgressText = itemView.findViewById(R.id.summary_statistic_progress_text);
            summaryStatisticTitle = itemView.findViewById(R.id.summary_statistic_title);
            summaryStatisticHint = itemView.findViewById(R.id.summary_statistic_hint);
        }
    }
}

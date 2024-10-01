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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.model.ProviderReportStatistic;
import com.muzima.utils.StringUtils;

import java.util.List;

public class PerformanceComparisonAdapter extends RecyclerView.Adapter<PerformanceComparisonAdapter.ViewHolder>{
    private List<ProviderReportStatistic> individualProviderStatistics;
    private Context context;
    private String loggedInUserSystemId;

   public PerformanceComparisonAdapter(List<ProviderReportStatistic> individualProviderStatistics, Context context){
        this.individualProviderStatistics = individualProviderStatistics;
        this.context = context;
        loggedInUserSystemId = ((MuzimaApplication)context.getApplicationContext()).getAuthenticatedUser().getSystemId();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_performance_comparison, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        ProviderReportStatistic statistic = individualProviderStatistics.get(position);
        viewHolder.summaryStatisticTitle.setText(statistic.getStatisticTitle());

        if(StringUtils.equals(loggedInUserSystemId,statistic.getProviderId())){
            viewHolder.summaryStatisticReference.setText("YOU");//ToDo: Translate once the text is clarified
        } else {
            viewHolder.summaryStatisticReference.setText("THEM");//ToDo: Translate once the text is clarified
        }

        int achievementRate = statistic.getExpectedAchievement() == 0? 0 : statistic.getAchievement()*100/statistic.getExpectedAchievement();

        viewHolder.summaryStatisticProgress.setProgress(achievementRate);
        viewHolder.summaryStatisticProgress.setSecondaryProgress(100);
        viewHolder.summaryStatisticProgress.post(new Runnable() {
            @Override
            public void run() {
                int progressWidth = viewHolder.summaryStatisticProgress.getMeasuredWidth();
                int youPerformanceWidth = viewHolder.youPerformance.getMeasuredWidth();
                viewHolder.avgPerformance.setX(progressWidth*statistic.getAchievementGroupAverage()/100 - youPerformanceWidth/2);
                viewHolder.youPerformance.setX(progressWidth*achievementRate/100 - youPerformanceWidth/2);
            }});
        viewHolder.summaryStatisticProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor(statistic.getSummaryColorCode())));
    }

    @Override
    public int getItemCount() {
        return individualProviderStatistics.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public final ProgressBar summaryStatisticProgress;
        public final TextView summaryStatisticTitle;
        public final TextView summaryStatisticReference;
        public final View youPerformance;
        public final View avgPerformance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            summaryStatisticProgress = itemView.findViewById(R.id.summary_statistic_progress);
            summaryStatisticTitle = itemView.findViewById(R.id.summary_statistic_title);
            summaryStatisticReference = itemView.findViewById(R.id.summary_statistic_reference);
            youPerformance = itemView.findViewById(R.id.you_performance);
            avgPerformance = itemView.findViewById(R.id.avg_performance);
        }
    }
}

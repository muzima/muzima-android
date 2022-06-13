package com.muzima.adapters.reports;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.model.ProviderReportStatistic;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PerformanceComparisonAdapter extends RecyclerView.Adapter<PerformanceComparisonAdapter.ViewHolder>{
    private List<ProviderReportStatistic> achievementStatistics;
    private Context context;

   public PerformanceComparisonAdapter(List<ProviderReportStatistic> achievementStatistics, Context context){
        this.achievementStatistics = achievementStatistics;
        this.context = context;
    }
    @NonNull
    @Override
    public PerformanceComparisonAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PerformanceComparisonAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_performance_comparison, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PerformanceComparisonAdapter.ViewHolder viewHolder, int position) {
        ProviderReportStatistic statistic = achievementStatistics.get(position);
        viewHolder.summaryStatisticTitle.setText(statistic.getStatisticTitle());

        int achievementRate = statistic.getExpectedAchievement() == 0? 0 : statistic.getAchievement()*100/statistic.getExpectedAchievement();

        viewHolder.summaryStatisticProgress.setProgress(achievementRate);
        viewHolder.summaryStatisticProgress.setSecondaryProgress(100);
        viewHolder.summaryStatisticProgress.post(new Runnable() {
            @Override
            public void run() {
                int progressWidth = viewHolder.summaryStatisticProgress.getMeasuredWidth();
                int youPerformanceWidth = viewHolder.youPerformance.getMeasuredWidth();
                viewHolder.avgPerformance.setX(progressWidth*statistic.getAchievementGroupAverage()/100 - youPerformanceWidth/4);
                viewHolder.youPerformance.setX(progressWidth*achievementRate/100 - youPerformanceWidth/4);
            }});
        viewHolder.summaryStatisticProgress.setProgressTintList(ColorStateList.valueOf(Color.parseColor(statistic.getSummaryColorCode())));
    }

    @Override
    public int getItemCount() {
        return achievementStatistics.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public final ProgressBar summaryStatisticProgress;
        public final TextView summaryStatisticTitle;
        public final View youPerformance;
        public final View avgPerformance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            summaryStatisticProgress = itemView.findViewById(R.id.summary_statistic_progress);
            summaryStatisticTitle = itemView.findViewById(R.id.summary_statistic_title);
            youPerformance = itemView.findViewById(R.id.you_performance);
            avgPerformance = itemView.findViewById(R.id.avg_performance);
        }
    }
}

package com.muzima.adapters.reports;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.model.ProviderAchievementStatistic;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PerformanceComparisonAdapter extends RecyclerView.Adapter{
    private List<ProviderAchievementStatistic> achievementStatistics;
    private Context context;

   public PerformanceComparisonAdapter(List<ProviderAchievementStatistic> achievementStatistics, Context context){
        this.achievementStatistics = achievementStatistics;
        this.context = context;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PerformanceComparisonAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_performance_comparison, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PerformanceComparisonAdapter.ViewHolder viewHolder = (PerformanceComparisonAdapter.ViewHolder)holder;
        ProviderAchievementStatistic statistic = achievementStatistics.get(position);
        viewHolder.summaryStatisticTitle.setText(statistic.getStatisticTitle());

        int achievementRate = statistic.getExpectedAchievement() == 0? 0 : statistic.getAchievement()*100/statistic.getExpectedAchievement();

        viewHolder.summaryStatisticProgress.setProgress(achievementRate);
        viewHolder.summaryStatisticProgress.setSecondaryProgress(100);
//
//        Drawable drawable = context.getResources().getDrawable(R.drawable.circular_progress);
//        viewHolder.summaryStatisticProgress.setProgressDrawable(drawable);
    }

    @Override
    public int getItemCount() {
        return achievementStatistics.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public final ProgressBar summaryStatisticProgress;
        public final TextView summaryStatisticTitle;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            summaryStatisticProgress = itemView.findViewById(R.id.summary_statistic_progress);
            summaryStatisticTitle = itemView.findViewById(R.id.summary_statistic_title);
        }
    }
}

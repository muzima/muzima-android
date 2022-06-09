package com.muzima.adapters.reports;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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

public class SummaryStatisticAdapter extends RecyclerView.Adapter {
    List<ProviderAchievementStatistic> achievementStatistics;
    Context context;

    public SummaryStatisticAdapter(List<ProviderAchievementStatistic> achievementStatistics, Context context){
        this.achievementStatistics = achievementStatistics;
        this.context = context;
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SummaryStatisticAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_summary_statistic_1, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SummaryStatisticAdapter.ViewHolder vh = (SummaryStatisticAdapter.ViewHolder)holder;
        ProviderAchievementStatistic statistic = achievementStatistics.get(position);
        vh.summaryStatisticTitle.setText(statistic.getStatisticTitle());
        vh.summaryStatisticHint.setText(statistic.getStatisticHint());

        int achievementRate = statistic.getAchievement()*100/statistic.getExpectedAchievement();
        vh.summaryStatisticProgressText.setText(achievementRate+"%");

        vh.summaryStatisticProgress.setProgress(achievementRate);
        vh.summaryStatisticProgress.setSecondaryProgress(100);

        Drawable drawable = context.getResources().getDrawable(R.drawable.circular_progress);
        vh.summaryStatisticProgress.setProgressDrawable(drawable);
    }

    @Override
    public int getItemCount() {
        return achievementStatistics.size();
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

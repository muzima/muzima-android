package com.muzima.adapters.reports;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

public class SummaryStatisticAdapter extends RecyclerView.Adapter {
    private List<ProviderReportStatistic> achievementStatistics;
    private Context context;

    public SummaryStatisticAdapter(List<ProviderReportStatistic> achievementStatistics, Context context){
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
        SummaryStatisticAdapter.ViewHolder viewHolder = (SummaryStatisticAdapter.ViewHolder)holder;
        ProviderReportStatistic statistic = achievementStatistics.get(position);
        viewHolder.summaryStatisticTitle.setText(statistic.getStatisticTitle());
        viewHolder.summaryStatisticHint.setText(statistic.getStatisticHint());

        int achievementRate = statistic.getExpectedAchievement() == 0? 0 : statistic.getAchievement()*100/statistic.getExpectedAchievement();
        viewHolder.summaryStatisticProgressText.setText(achievementRate+"%");

        viewHolder.summaryStatisticProgress.setProgress(achievementRate);
        viewHolder.summaryStatisticProgress.setSecondaryProgress(100);

        Drawable drawable = context.getResources().getDrawable(R.drawable.circular_progress);
        viewHolder.summaryStatisticProgress.setProgressDrawable(drawable);
        //viewHolder.summaryStatisticProgress.setProgressTintBlendMode(Mode.C);
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

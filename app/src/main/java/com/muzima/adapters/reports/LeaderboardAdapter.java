package com.muzima.adapters.reports;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.model.ProviderReportStatistic;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>{
    private List<ProviderReportStatistic> achievementStatistics;
    private Context context;

   public LeaderboardAdapter(List<ProviderReportStatistic> achievementStatistics, Context context){
        this.achievementStatistics = achievementStatistics;
        this.context = context;
    }
    @NonNull
    @Override
    public LeaderboardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LeaderboardAdapter.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardAdapter.ViewHolder holder, int position) {
       ProviderReportStatistic statistic = achievementStatistics.get(position);
        holder.rankTextView.setText(String.format(Locale.getDefault(), "%d", position));

        holder.usernameTextView.setText(statistic.getProviderName());
        holder.pointsTextView.setText(String.format(Locale.getDefault(),"%d ",statistic.getAchievement()));

    }

    @Override
    public int getItemCount() {
        return achievementStatistics.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView accountAvatarImageView;
        private final TextView usernameTextView;
        private final TextView rankTextView;
        private final TextView pointsTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            accountAvatarImageView = itemView.findViewById(R.id.avatar_view);
            usernameTextView = itemView.findViewById(R.id.name_text_view);
            rankTextView = itemView.findViewById(R.id.item_leaderboard_main_position_text_view);
            pointsTextView = itemView.findViewById(R.id.points_text_view);
        }
    }
}

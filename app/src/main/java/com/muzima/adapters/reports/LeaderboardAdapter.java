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
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.model.ProviderReportStatistic;
import com.muzima.utils.StringUtils;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>{
    private List<ProviderReportStatistic> reportStatistics;
    private LeaderboardItemClickListener leaderboardItemClickListener;
    private Context context;
    private String loggedInUserSystemId;

   public LeaderboardAdapter(List<ProviderReportStatistic> reportStatistics,
                             LeaderboardItemClickListener leaderboardItemClickListener, Context context){
        this.reportStatistics = reportStatistics;
        this.leaderboardItemClickListener = leaderboardItemClickListener;
        this.context = context;
        loggedInUserSystemId = ((MuzimaApplication)context.getApplicationContext()).getAuthenticatedUser().getSystemId();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
       ProviderReportStatistic statistic = reportStatistics.get(position);
        holder.rankTextView.setText(String.format(Locale.getDefault(), "%d", position+1));

        if(!StringUtils.isEmpty(statistic.getProviderName())) {
            String providerName = statistic.getProviderName().trim();
            holder.usernameTextView.setText(providerName);
            holder.avatarTextView.setText(""+providerName.charAt(0));
        }

        holder.avatarImageView.setImageTintList(ColorStateList.valueOf(statistic.getLeaderboardColor()));

        holder.pointsTextView.setText(String.format(Locale.getDefault(),"%d ",statistic.getScore()));
        holder.container.setOnClickListener(view -> leaderboardItemClickListener.onLeaderboardItemClick(view, holder.getAdapterPosition()));

        if(StringUtils.equals(loggedInUserSystemId,statistic.getProviderId())){
            holder.container.setBackgroundColor(Color.parseColor("#F6F0FA"));
        }
    }

    public ProviderReportStatistic getReportStatistic(int position){
        if(position < 0 || position >= getItemCount()){
            return null;
        }
       return reportStatistics.get(position);
    }

    @Override
    public int getItemCount() {
        return reportStatistics.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarTextView;
        private final ImageView avatarImageView;
        private final TextView usernameTextView;
        private final TextView rankTextView;
        private final TextView pointsTextView;
        private final View container;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            avatarImageView = itemView.findViewById(R.id.avatar_image_view);
            avatarTextView = itemView.findViewById(R.id.avatar_text_view);
            usernameTextView = itemView.findViewById(R.id.name_text_view);
            rankTextView = itemView.findViewById(R.id.item_leaderboard_main_position_text_view);
            pointsTextView = itemView.findViewById(R.id.points_text_view);
            container = itemView.findViewById(R.id.leaderboard_item_container);
        }
    }

    public interface LeaderboardItemClickListener{
        void onLeaderboardItemClick(View view, int position);
    }
}

/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.reports.LeaderboardAdapter;
import com.muzima.adapters.reports.PerformanceComparisonAdapter;
import com.muzima.adapters.reports.SummaryStatisticAdapter;
import com.muzima.model.ProviderReportStatistic;
import com.muzima.utils.StringUtils;

public class PerformanceSummaryFragment extends Fragment {
    private SummaryStatisticAdapter summaryStatisticAdapter;
    private PerformanceComparisonAdapter performanceComparisonAdapter;
    private LeaderboardAdapter leaderboardAdapter;
    private LeaderBoardTitleClickListener leaderBoardTitleClickListener;
    public PerformanceSummaryFragment() {}
    public static PerformanceSummaryFragment newInstance(SummaryStatisticAdapter summaryStatisticAdapter,
                     PerformanceComparisonAdapter performanceComparisonAdapter, LeaderboardAdapter leaderboardAdapter,
                     LeaderBoardTitleClickListener leaderBoardTitleClickListener) {
        PerformanceSummaryFragment fragment = new PerformanceSummaryFragment();
        fragment.summaryStatisticAdapter = summaryStatisticAdapter;
        fragment.performanceComparisonAdapter = performanceComparisonAdapter;
        fragment.leaderboardAdapter = leaderboardAdapter;
        fragment.leaderBoardTitleClickListener = leaderBoardTitleClickListener;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_performance_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if(summaryStatisticAdapter.getItemCount()>0){
            ProviderReportStatistic statistic = summaryStatisticAdapter.getReportStatistic(0);
            TextView greeting1 = view.findViewById(R.id.greeting1);
            TextView greeting2 = view.findViewById(R.id.greeting2);
            TextView statisticsSummaryTitle = view.findViewById(R.id.statistics_summary_title);
            String loggedInUserSystemId = ((MuzimaApplication)getContext().getApplicationContext()).getAuthenticatedUser().getSystemId();

            if(StringUtils.equals(statistic.getProviderId(), loggedInUserSystemId)) {
                greeting1.setText(getString(R.string.hello_general) +" "+ statistic.getProviderName());
                greeting1.setVisibility(View.VISIBLE);
                greeting2.setText(getString(R.string.hint_your_performance, statistic.getStartDate(),statistic.getEndDate()));
                statisticsSummaryTitle.setText(R.string.general_your_statistics);
            } else {
                greeting1.setVisibility(View.GONE);
                greeting2.setText(getString(R.string.hint_their_performance, statistic.getProviderName(), statistic.getStartDate(), statistic.getEndDate()));
                statisticsSummaryTitle.setText(R.string.general_their_statistics);
            }
            ImageView avatar = view.findViewById(R.id.main_avatar_image_view);
            avatar.setImageTintList(ColorStateList.valueOf(statistic.getLeaderboardColor()));

            TextView textView = view.findViewById(R.id.main_avatar_text_view);
            if(!StringUtils.isEmpty(statistic.getProviderName())) {
                String providerName = statistic.getProviderName().trim();
                textView.setText(""+providerName.charAt(0));
            }
        }
        RecyclerView summaryView = view.findViewById(R.id.summary_statistic_1);
        summaryView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        summaryView.setAdapter(summaryStatisticAdapter);
        summaryStatisticAdapter.notifyDataSetChanged();

        RecyclerView performanceComparisonView =  view.findViewById(R.id.performance_comparison);
        performanceComparisonView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        performanceComparisonView.setAdapter(performanceComparisonAdapter);
        performanceComparisonAdapter.notifyDataSetChanged();

        RecyclerView leaderboardView = view.findViewById(R.id.leaderboard);
        leaderboardView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        leaderboardView.setAdapter(leaderboardAdapter);
        leaderboardAdapter.notifyDataSetChanged();

        View leaderboardTitleBar = view.findViewById(R.id.leaderboard_section_title_bar);
        leaderboardTitleBar.setOnClickListener(view1 -> leaderBoardTitleClickListener.onLeaderboardSectionTitleClicked());
    }

    public interface LeaderBoardTitleClickListener{
        void onLeaderboardSectionTitleClicked();
    }
}
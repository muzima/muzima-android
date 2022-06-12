package com.muzima.view.reports;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.R;
import com.muzima.adapters.reports.LeaderboardAdapter;
import com.muzima.adapters.reports.PerformanceComparisonAdapter;
import com.muzima.adapters.reports.SummaryStatisticAdapter;

public class PerformanceReportFragment extends Fragment {
    private SummaryStatisticAdapter summaryStatisticAdapter;
    private PerformanceComparisonAdapter performanceComparisonAdapter;
    private LeaderboardAdapter leaderboardAdapter;
    public PerformanceReportFragment() {}
    public static PerformanceReportFragment newInstance(SummaryStatisticAdapter summaryStatisticAdapter,
              PerformanceComparisonAdapter performanceComparisonAdapter, LeaderboardAdapter leaderboardAdapter) {
        PerformanceReportFragment fragment = new PerformanceReportFragment();
        fragment.summaryStatisticAdapter = summaryStatisticAdapter;
        fragment.performanceComparisonAdapter = performanceComparisonAdapter;
        fragment.leaderboardAdapter = leaderboardAdapter;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_performance_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView summaryView = (RecyclerView) view.findViewById(R.id.summary_statistic_1);
        summaryView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        summaryView.setAdapter(summaryStatisticAdapter);
        summaryStatisticAdapter.notifyDataSetChanged();

        RecyclerView performanceComparisonView = (RecyclerView) view.findViewById(R.id.performance_comparison);
        performanceComparisonView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        performanceComparisonView.setAdapter(performanceComparisonAdapter);
        summaryStatisticAdapter.notifyDataSetChanged();

        RecyclerView leaderboardView = (RecyclerView) view.findViewById(R.id.leaderboard);
        leaderboardView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        leaderboardView.setAdapter(leaderboardAdapter);
        leaderboardAdapter.notifyDataSetChanged();
    }
}
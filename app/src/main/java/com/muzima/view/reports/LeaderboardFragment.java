package com.muzima.view.reports;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.adapters.reports.LeaderboardAdapter;

public class LeaderboardFragment extends Fragment {
    private LeaderboardAdapter leaderboardAdapter;
    public LeaderboardFragment() {}
    public static LeaderboardFragment newInstance(LeaderboardAdapter leaderboardAdapter) {
        LeaderboardFragment fragment = new LeaderboardFragment();
        fragment.leaderboardAdapter = leaderboardAdapter;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_performance_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView leaderboardView = view.findViewById(R.id.leaderboard);
        leaderboardView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        leaderboardView.setAdapter(leaderboardAdapter);
        leaderboardAdapter.notifyDataSetChanged();
    }
}
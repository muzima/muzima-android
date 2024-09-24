package com.muzima.view.reports;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.adapters.reports.LeaderboardAdapter;
import com.muzima.view.custom.MuzimaRecyclerView;

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
        MuzimaRecyclerView leaderboardView = view.findViewById(R.id.leaderboard);
        leaderboardView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        leaderboardView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_no_provider_reports_found),
                getString(R.string.hint_no_provider_reports_available));
        leaderboardView.setAdapter(leaderboardAdapter);
        leaderboardAdapter.notifyDataSetChanged();

        EditText searchView = view.findViewById(R.id.provider_search);
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                leaderboardAdapter.filterByText(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
}
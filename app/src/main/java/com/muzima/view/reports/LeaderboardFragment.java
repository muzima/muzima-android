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

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.muzima.R;
import com.muzima.adapters.reports.LeaderboardAdapter;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardFragment extends Fragment {
    private LeaderboardAdapter leaderboardAdapter;
    private Map<String, View> statisticHeaderViews = new HashMap<>();
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

        String activeHeader = leaderboardAdapter.getActiveStatisticHeader();
        List<String> statisticAbbreviations = leaderboardAdapter.getStatisticHeaderList();

        for(String abbreviation : statisticAbbreviations) {
            LinearLayout pointsHeaders = view.findViewById(R.id.points_headers_layout);
            View headerPv = getLayoutInflater().inflate(R.layout.item_leaderboard_points_header, null);
            TextView tv = headerPv.findViewById(R.id.header_text_view);
            tv.setText(abbreviation);

            TextView divider = new TextView(getContext());
            divider.setText("  ");
            pointsHeaders.addView(divider);
            pointsHeaders.addView(headerPv);
            statisticHeaderViews.put(abbreviation, headerPv);

            headerPv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean isAscending = leaderboardAdapter.sortListBySelectedStatistic(abbreviation);
                    updateSelectedHeaderView(abbreviation, isAscending);
                }
            });
        }
        updateSelectedHeaderView(activeHeader, false);
        TextView headerHelpInfo = view.findViewById(R.id.statistic_header_details_txt);
        headerHelpInfo.setSelected(true);
        headerHelpInfo.setText(leaderboardAdapter.getStatisticHeaderHelpInfo()
                + ",  " + leaderboardAdapter.getStatisticHeaderHelpInfo() );

        View detailsHelpIcon = view.findViewById(R.id.statistic_header_info);
        View detailsSection = view.findViewById(R.id.statistic_header_details);
        detailsHelpIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detailsHelpIcon.setVisibility(View.INVISIBLE);
                detailsSection.setVisibility(View.VISIBLE);
            }
        });
        View detailsHider = view.findViewById(R.id.details_hider);
        detailsHider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detailsSection.setVisibility(View.GONE);
                detailsHelpIcon.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateSelectedHeaderView(String abbreviation, boolean isAscending){
        for(String key : statisticHeaderViews.keySet()){
            View headerPv = statisticHeaderViews.get(key);
            TextView tv = headerPv.findViewById(R.id.header_text_view);
            if(!StringUtils.equals(key, abbreviation)) {
                tv.setTextColor(Color.BLACK);
                headerPv.setBackgroundResource(R.drawable.round_corners_transparent);
                headerPv.findViewById(R.id.arrow_up).setVisibility(View.GONE);
                headerPv.findViewById(R.id.arrow_down).setVisibility(View.GONE);
            } else {
                tv.setTextColor(Color.WHITE);
                headerPv.setBackgroundResource(R.drawable.round_corners_primary_blue);
                if(isAscending) {
                    headerPv.findViewById(R.id.arrow_up).setVisibility(View.GONE);
                    headerPv.findViewById(R.id.arrow_down).setVisibility(View.VISIBLE);
                } else {
                    headerPv.findViewById(R.id.arrow_up).setVisibility(View.VISIBLE);
                    headerPv.findViewById(R.id.arrow_down).setVisibility(View.GONE);
                }
            }
        }
    }
}
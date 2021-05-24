package com.muzima.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.viewpager.FormsViewPagerAdapter;
import com.muzima.tasks.FormsCountService;

public class DashboardFormsFragment extends Fragment implements FormsCountService.FormsCountServiceCallback {

    private View incompleteFormsCardView;
    private View completeFormsCardView;
    private TextView incompleteFormsCountTextView;
    private TextView completeFormsCountTextView;
    private EditText searchFormsEditText;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private FormsViewPagerAdapter viewPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_forms, container, false);
        initializeResources(view);
        loadFormsCount();
        return view;
    }

    private void loadFormsCount() {
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute( new FormsCountService(getActivity().getApplicationContext(),this));
    }

    private void initializeResources(View view) {
        incompleteFormsCardView = view.findViewById(R.id.dashboard_forms_incomplete_forms_view);
        completeFormsCardView = view.findViewById(R.id.dashboard_forms_complete_forms_view);
        incompleteFormsCountTextView = view.findViewById(R.id.dashboard_forms_incomplete_forms_count_view);
        completeFormsCountTextView = view.findViewById(R.id.dashboard_forms_complete_forms_count_view);
        searchFormsEditText = view.findViewById(R.id.dashboard_forms_search_edit_text);
        viewPager = view.findViewById(R.id.dashboard_forms_view_pager);
        tabLayout = view.findViewById(R.id.dashboard_forms_tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        viewPagerAdapter = new FormsViewPagerAdapter(getChildFragmentManager(),getActivity().getApplicationContext());
        viewPager.setAdapter(viewPagerAdapter);

        completeFormsCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo start complete forms
            }
        });

        incompleteFormsCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo start incomplete forms
            }
        });
    }

    @Override
    public void onFormsCountLoaded(final long completeFormsCount, final long incompleteFormsCount) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                completeFormsCountTextView.setText(String.valueOf(completeFormsCount));
                incompleteFormsCountTextView.setText(String.valueOf(incompleteFormsCount));
            }
        });
    }
}

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
import com.muzima.R;
import com.muzima.adapters.viewpager.FormsViewPagerAdapter;

public class DashboardFormsFragment extends Fragment {

    private EditText searchFormsEditText;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private FormsViewPagerAdapter viewPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_forms, container, false);
        initializeResources(view);
        return view;
    }

    private void initializeResources(View view) {
        searchFormsEditText = view.findViewById(R.id.dashboard_forms_search_edit_text);
        viewPager = view.findViewById(R.id.dashboard_forms_view_pager);
        tabLayout = view.findViewById(R.id.dashboard_forms_tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        viewPagerAdapter = new FormsViewPagerAdapter(getChildFragmentManager(), getActivity().getApplicationContext());
        viewPager.setAdapter(viewPagerAdapter);
    }
}

package com.muzima.view.fragments.patient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.muzima.R;
import com.muzima.adapters.patients.HistoricalDataViewPagerAdapter;

public class HistoricalDataFragment extends Fragment {
    private final String patientUuid;

    public HistoricalDataFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_historical_data, container, false);
        ViewPager2 viewPager =  view.findViewById(R.id.hd_view_pager);
        TabLayout tabLayout = view.findViewById(R.id.hdTabLayout);
        HistoricalDataViewPagerAdapter adapter = new HistoricalDataViewPagerAdapter(requireActivity(), tabLayout.getTabCount(), patientUuid);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        return view;
    }
}

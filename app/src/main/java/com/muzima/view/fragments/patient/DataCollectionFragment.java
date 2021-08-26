package com.muzima.view.fragments.patient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.muzima.R;
import com.muzima.adapters.patients.DataCollectionViewPagerAdapter;
import com.muzima.adapters.patients.HistoricalDataViewPagerAdapter;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.ClientSummaryActivity;

public class DataCollectionFragment extends Fragment {

    private final String patientUuid;

    public DataCollectionFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_collection, container, false);
        ViewPager viewPager =  view.findViewById(R.id.dc_view_pager);
        TabLayout tabLayout = view.findViewById(R.id.dcTabLayout);
        DataCollectionViewPagerAdapter adapter = new DataCollectionViewPagerAdapter(getChildFragmentManager(), patientUuid);
        viewPager.setAdapter(adapter);

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

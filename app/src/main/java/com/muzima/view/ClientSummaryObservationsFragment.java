package com.muzima.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.muzima.R;
import com.muzima.model.events.ReloadObservationsDataEvent;

import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

public class ClientSummaryObservationsFragment extends Fragment {

    private ViewPager viewPager;
    private ObservationsViewPagerAdapter adapter;
    private String patientUuid;

    public ClientSummaryObservationsFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_client_summary_observations_view,container,false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeResources(view);
    }

    private void initializeResources(View view) {
        viewPager = view.findViewById(R.id.fragment_client_summary_individual_obs_obs_viewpager);
        adapter = new ObservationsViewPagerAdapter(getChildFragmentManager(),getActivity().getApplicationContext(),patientUuid);
        viewPager.setAdapter(adapter);
    }

}

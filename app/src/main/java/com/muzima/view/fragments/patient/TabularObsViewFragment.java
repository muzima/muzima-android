package com.muzima.view.fragments.patient;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.view.custom.TableFixHeaders;
import com.muzima.adapters.observations.BaseTableAdapter;
import com.muzima.adapters.observations.ObservationGroupAdapter;

public class TabularObsViewFragment extends Fragment implements RecyclerAdapter.BackgroundListQueryTaskListener {
    private final String patientUuid;
    private com.muzima.adapters.observations.ObservationGroupAdapter observationGroupAdapter;
    private final Context context;

    public TabularObsViewFragment(String patientUuid, Context context) {
        this.patientUuid = patientUuid;
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.table, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TableFixHeaders tableFixHeaders = view.findViewById(R.id.table);
        BaseTableAdapter baseTableAdapter = new ObservationGroupAdapter(context, patientUuid);
        tableFixHeaders.setAdapter(baseTableAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {}

    @Override
    public void onQueryTaskCancelled() {}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {}

}
package com.muzima.view.cohort;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.muzima.R;
import com.muzima.controller.CohortController;
import com.muzima.view.MuzimaListFragment;

public abstract class CohortListFragment extends MuzimaListFragment {
    private static final String TAG = "CohortListFragment";

    protected CohortController cohortController;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View formsLayout = inflater.inflate(R.layout.layout_list_fragment, container, false);
        list = (ListView) formsLayout.findViewById(R.id.list);

        setupNoDataView(formsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
        }
        list.setEmptyView(formsLayout.findViewById(R.id.no_data_layout));

        return formsLayout;
    }
}

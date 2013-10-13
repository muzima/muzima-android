package com.muzima.view.cohort;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPrefixPrefAdapter;
import com.muzima.view.preferences.CohortPrefActivity;

public class CohortPrefixFragment extends Fragment{
    private CohortPrefixPrefAdapter prefAdapter;
    private EditText addPrefixEditText;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_cohort_pref,container, false);

        initialiseList(view);

        addPrefixEditText = (EditText) view.findViewById(R.id.prefix_edit_text);
        return view;
    }

    private void initialiseList(View view) {
        ListView cohortPrefList = (ListView) view.findViewById(R.id.cohort_pref_list);
        prefAdapter = new CohortPrefixPrefAdapter(getActivity(), R.layout.item_preference);
        prefAdapter.setPreferenceClickListener((CohortPrefActivity) getActivity());
        cohortPrefList.setEmptyView(view.findViewById(R.id.no_data_msg));
        cohortPrefList.setAdapter(prefAdapter);
    }

    public String getPrefixText() {
        return addPrefixEditText.getText().toString();
    }

    public void updateView() {
        prefAdapter.reloadData();
        addPrefixEditText.setText("");
    }

    public void reloadData() {
        prefAdapter.reloadData();
    }
}

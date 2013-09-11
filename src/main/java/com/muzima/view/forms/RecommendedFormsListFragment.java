package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.DownloadedFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.DownloadedForm;

public class RecommendedFormsListFragment extends FormsListFragment implements AllAvailableFormsListFragment.OnTemplateDownloadComplete {
    private static String TAG = "RecommendedFormsListFragment";
    private String patientId;

    public static RecommendedFormsListFragment newInstance(FormController formController, String patientId) {
        RecommendedFormsListFragment f = new RecommendedFormsListFragment();
        f.formController = formController;
        f.patientId = patientId;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new DownloadedFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_downloaded_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_downloaded_form_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        startActivity(new FormViewIntent(getActivity(), (DownloadedForm) listAdapter.getItem(position), patientId));
    }

    @Override
    public void onTemplateDownloadComplete(Integer[] result) {
        ((FormsActivity) getActivity()).hideProgressbar();
        synchronizationComplete(result);
    }
}

package com.muzima.view.forms;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.DownloadedFormsAdapter;
import com.muzima.controller.FormController;

public class DownloadedFormsListFragment extends FormsListFragment implements AllAvailableFormsListFragment.OnTemplateDownloadComplete{

    public static DownloadedFormsListFragment newInstance(FormController formController) {
        DownloadedFormsListFragment f = new DownloadedFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new DownloadedFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_downloaded_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_downloaded_form_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        //TODO: Launch patient selection view
    }

    @Override
    public void onTemplateDownloadComplete(Integer[] result) {
        ((FormsActivity)getActivity()).hideProgressbar();
        synchronizationComplete(result);
    }
}

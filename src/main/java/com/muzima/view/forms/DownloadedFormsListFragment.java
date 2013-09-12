package com.muzima.view.forms;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.DownloadedFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.DownloadedForm;

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
        DownloadedForm form = (DownloadedForm) listAdapter.getItem(position);
        Intent intent = new Intent(getActivity(), FormWebViewActivity.class);
        intent.putExtra(FormWebViewActivity.FORM_UUID, form.getFormUuid());
        intent.putExtra(FormWebViewActivity.FORM_DATA_UUID, "patientId");
        startActivity(intent);
    }

    @Override
    public void onTemplateDownloadComplete(Integer[] result) {
        ((FormsActivity)getActivity()).hideProgressbar();
        synchronizationComplete(result);
    }
}

package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.NewFormsAdapter;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.tasks.forms.DownloadFormTemplateTask;

import java.util.List;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;
import static com.muzima.utils.Constants.FORMS_SERVER;
import static com.muzima.utils.Constants.PASS;
import static com.muzima.utils.Constants.USERNAME;

public class NewFormsListFragment extends FormsListFragment implements DownloadListener<Integer[]> {
    private static final String TAG = "NewFormsListFragment";

    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private DownloadFormTemplateTask formTemplateDownloadTask;
    private OnTemplateDownloadComplete templateDownloadCompleteListener;

    public static NewFormsListFragment newInstance(FormController formController) {
        NewFormsListFragment f = new NewFormsListFragment();
        f.formController = formController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new NewFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_new_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_new_form_tip);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new NewFormsActionModeCallback());
            actionModeActive = true;
        }
        ((NewFormsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedForms = ((NewFormsAdapter) listAdapter).getSelectedForms().size();
        if(numOfSelectedForms == 0 && actionModeActive){
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        if(templateDownloadCompleteListener != null){
            templateDownloadCompleteListener.onTemplateDownloadComplete(result);
        }
    }

    public final class NewFormsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.form_list_actionmode_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_download:
                    if (formTemplateDownloadTask != null &&
                            (formTemplateDownloadTask.getStatus() == PENDING || formTemplateDownloadTask.getStatus() == RUNNING)) {
                        Toast.makeText(getActivity(), "Already fetching form templates, ignored the request", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    formTemplateDownloadTask = new DownloadFormTemplateTask((MuzimaApplication) getActivity().getApplication());
                    formTemplateDownloadTask.addDownloadListener(NewFormsListFragment.this);
                    String[] credentials = new String[]{USERNAME, PASS, FORMS_SERVER};
                    formTemplateDownloadTask.execute(credentials, getSelectedFormsUuid());
                    if (NewFormsListFragment.this.actionMode != null) {
                        NewFormsListFragment.this.actionMode.finish();
                    }
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((NewFormsAdapter)listAdapter).clearSelectedForms();
        }
    }

    private String[] getSelectedFormsUuid() {
        List<Form> selectedForms = ((NewFormsAdapter) listAdapter).getSelectedForms();
        String[] selectedFormUuids = new String[selectedForms.size()];
        for(int i =0; i < selectedForms.size(); i++){
            selectedFormUuids[i] = selectedForms.get(i).getUuid();
        }
        return selectedFormUuids;
    }

    public void setTemplateDownloadCompleteListener(OnTemplateDownloadComplete templateDownloadCompleteListener) {
        this.templateDownloadCompleteListener = templateDownloadCompleteListener;
    }

    public interface OnTemplateDownloadComplete {
        public void onTemplateDownloadComplete(Integer[] result);
    }
}

package com.muzima.view.forms;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.view.MuzimaListFragment;

import static com.muzima.adapters.ListAdapter.BackgroundListQueryTaskListener;

public abstract class FormsListFragment extends MuzimaListFragment implements BackgroundListQueryTaskListener{
    private static final String TAG = "FormsListFragment";

    protected FormController formController;
    protected FrameLayout progressBarContainer;
    protected LinearLayout noDataView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View formsLayout = setupMainView(inflater, container);
        list = (ListView) formsLayout.findViewById(R.id.list);
        progressBarContainer = (FrameLayout) formsLayout.findViewById(R.id.progressbarContainer);
        noDataView = (LinearLayout) formsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(formsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            ((FormsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(formsLayout.findViewById(R.id.no_data_layout));

        return formsLayout;
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    public void tagsChanged() {
        listAdapter.reloadData();
    }

    @Override
    public void synchronizationComplete(Integer[] status) {
        Integer downloadStatus = status[0];
        String msg = "Download Complete with status " + downloadStatus;
        Log.i(TAG, msg);
        if (downloadStatus == DownloadMuzimaTask.SUCCESS) {
            msg = "Forms downloaded: " + status[1];
            if (listAdapter != null) {
                listAdapter.reloadData();
            }
        } else if (downloadStatus == DownloadMuzimaTask.DOWNLOAD_ERROR) {
            msg = "An error occurred while downloading forms";
        } else if (downloadStatus == DownloadMuzimaTask.AUTHENTICATION_ERROR) {
            msg = "Authentication error occurred while downloading forms";
        } else if (downloadStatus == DownloadMuzimaTask.DELETE_ERROR) {
            msg = "An error occurred while deleting existing forms";
        } else if (downloadStatus == DownloadMuzimaTask.SAVE_ERROR) {
            msg = "An error occurred while saving the downloaded forms";
        } else if (downloadStatus == DownloadMuzimaTask.CANCELLED) {
            msg = "Form download task has been cancelled";
        } else if (downloadStatus == DownloadMuzimaTask.CONNECTION_ERROR) {
            msg = "Connection error occurred while downloading forms";
        } else if (downloadStatus == DownloadMuzimaTask.PARSING_ERROR) {
            msg = "Parse exception has been thrown while fetching data";
        }
        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onQueryTaskStarted() {
        list.setVisibility(View.INVISIBLE);
        noDataView.setVisibility(View.INVISIBLE);
        progressBarContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        list.setVisibility(View.VISIBLE);
        progressBarContainer.setVisibility(View.INVISIBLE);
    }
}

package com.muzima.view.fragments;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.muzima.R;
import com.muzima.adapters.cohort.CohortsViewPagerAdapter;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.cohort.SyncCohortsIntent;

public class DashboardCohortsFragment extends Fragment {

    private ViewPager viewPager;
    private CohortsViewPagerAdapter cohortPagerAdapter;
    private MenuItem menubarLoadButton;
    private boolean syncInProgress;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeUtils.onCreate(this.getActivity());
        languageUtil.onCreate(this.getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_cohorts, container, false);
        initPager(view);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        themeUtils.onResume(this.getActivity());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (!NetworkUtils.isConnectedToNetwork(getActivity().getApplicationContext())) {
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.error_local_connection_unavailable), Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (syncInProgress) {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.info_cohort_fetch_in_progress), Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncCohortsInBackgroundService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    @Override
//    protected void onReceive(Context context, Intent intent) {
//        super.onReceive(context, intent);
//
//        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
//        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
//
//        switch (syncType) {
//            case Constants.DataSyncServiceConstants.SYNC_COHORTS_METADATA:
//                hideProgressbar();
//                syncInProgress = false;
//                if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
//                    cohortPagerAdapter.onCohortDownloadFinish();
//                }
//                break;
//            case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA:
//                if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
//                    cohortPagerAdapter.onPatientsDownloadFinish();
//                }
//                break;
//            case Constants.DataSyncServiceConstants.SYNC_ENCOUNTERS:
//                hideProgressbar();
//                break;
//        }
//    }

    private void hideProgressbar() {
        menubarLoadButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarLoadButton.setActionView(R.layout.refresh_menuitem);
    }

    private void initPager(View view) {
        viewPager = view.findViewById(R.id.pager);
        cohortPagerAdapter = new CohortsViewPagerAdapter(getChildFragmentManager(), getActivity().getApplicationContext());
        viewPager.setAdapter(cohortPagerAdapter);
    }

    private void syncCohortsInBackgroundService() {
        syncInProgress = true;
        showProgressBar();
        new SyncCohortsIntent(this.getActivity()).start();
    }

    public void setCurrentView(int position) {
        viewPager.setCurrentItem(position);
    }
}

/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

public abstract class MuzimaListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "MuzimaListFragment";

    protected ListView list;

    protected String noDataMsg;
    protected String noDataTip;
    protected ListAdapter listAdapter;

    protected MuzimaListFragment() {
        setRetainInstance(true);
    }

    protected void setupNoDataView(View formsLayout) {
        TextView noDataMsgTextView = formsLayout.findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(noDataMsg);
        TextView noDataTipTextView = formsLayout.findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(noDataTip);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(getActivity()));
        noDataTipTextView.setTypeface(Fonts.roboto_medium(getActivity()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reloadData();
    }

    public void reloadData() {
        if(listAdapter != null){
            listAdapter.reloadData();
            listAdapter.notifyDataSetChanged();
        }
    }

    public void unselectAllItems() {
        unselectAllItems(list);
    }

    protected void unselectAllItems(ListView listView) {
        if(listView==null){
            return;
        }
        for (int i = listView.getCount() - 1; i >= 0; i--){
            if(listView.getChildAt(i) instanceof CheckedRelativeLayout){
                listView.getChildAt(i).setActivated(false);
                ((CheckedRelativeLayout) listView.getChildAt(i)).setChecked(false);
            }
            listView.setItemChecked(i, false);
        }
    }

    protected void updateDataLoadStatus(View layout, String noDataMsg){
        TextView noDataMsgTextView = layout.findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(noDataMsg);
    }

    protected void logEvent(String tag, String details){
        if(StringUtils.isEmpty(details)){
            details = "{}";
        }
        MuzimaApplication muzimaApplication = (MuzimaApplication) getActivity().getApplication();
        MuzimaLoggerService.log(muzimaApplication,tag,  details);
    }

    protected void logEvent(String tag){
        logEvent(tag,null);
    }
}

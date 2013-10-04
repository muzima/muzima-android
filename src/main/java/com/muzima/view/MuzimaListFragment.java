package com.muzima.view;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.utils.Fonts;

public abstract class MuzimaListFragment extends SherlockFragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "MuzimaListFragment";

    protected ListView list;

    protected String noDataMsg;
    protected String noDataTip;
    protected ListAdapter listAdapter;

    protected MuzimaListFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    protected void setupNoDataView(View formsLayout) {
        TextView noDataMsgTextView = (TextView) formsLayout.findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(noDataMsg);
        TextView noDataTipTextView = (TextView) formsLayout.findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(noDataTip);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(getActivity()));
        noDataTipTextView.setTypeface(Fonts.roboto_light(getActivity()));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reloadData();
    }

    public void reloadData() {
        if(listAdapter != null){
            listAdapter.reloadData();
        }
    }
}

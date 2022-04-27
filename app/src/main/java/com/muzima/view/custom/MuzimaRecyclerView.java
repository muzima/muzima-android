/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;

public class MuzimaRecyclerView extends RecyclerView {
    private LinearLayout noDataLayout;
    final private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkNoData();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkNoData();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkNoData();
        }
    };

    public MuzimaRecyclerView(Context context) {
        super(context);
    }

    public MuzimaRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MuzimaRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    void checkNoData() {
        if (noDataLayout != null && getAdapter() != null) {
            final boolean emptyViewVisible = getAdapter().getItemCount() == 0;
            noDataLayout.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter<?> oldAdapter = getAdapter();
        if (oldAdapter != null)
            oldAdapter.unregisterAdapterDataObserver(observer);

        super.setAdapter(adapter);
        if (adapter != null)
            adapter.registerAdapterDataObserver(observer);

        checkNoData();
    }

    public void setNoDataLayout(LinearLayout noDataLayout, String noDataMsg, String noDataTip) {
//        this.noDataLayout = noDataLayout;
//        TextView noDataMsgTextView = this.noDataLayout.findViewById(R.id.no_data_msg);
//        noDataMsgTextView.setText(noDataMsg);
//        TextView noDataTipTextView = this.noDataLayout.findViewById(R.id.no_data_tip);
//        noDataTipTextView.setText(noDataTip);
//        checkNoData();
    }
}

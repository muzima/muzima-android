/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/**
 * TODO: Write brief description about the class here.
 */
public class ScrollViewWithDetection extends ScrollView {

    private OnBottomReachedListener mListener;

    public ScrollViewWithDetection(final Context context) {
        super(context);
    }

    public ScrollViewWithDetection(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollViewWithDetection(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        View view = getChildAt(getChildCount() - 1);
        int diff = (view.getBottom() - (getHeight() + getScrollY()));
        if (diff <= 0 && mListener != null) {
            mListener.onBottomReached();
        }
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public OnBottomReachedListener getOnBottomReachedListener() {
        return mListener;
    }

    public void setOnBottomReachedListener(
            OnBottomReachedListener onBottomReachedListener) {
        mListener = onBottomReachedListener;
    }

    /**
     * Event listener.
     */
    public interface OnBottomReachedListener {
        void onBottomReached();
    }

}

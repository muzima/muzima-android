/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.view.View;

import java.util.Stack;

public class Recycler  {

    private Stack<View>[] views;

    /**
     * Constructor
     *
     * @param size
     *            The number of types of view to recycle.
     */
    @SuppressWarnings("unchecked")
    public Recycler(int size) {
        views = new Stack[size];
        for (int i = 0; i < size; i++) {
            views[i] = new Stack<View>();
        }
    }

    /**
     * Add a view to the Recycler. This view may be reused in the function
     * {@link #getRecycledView(int)}
     *
     * @param view
     *            A view to add to the Recycler. It can no longer be used.
     * @param type
     *            the type of the view.
     */
    public void addRecycledView(View view, int type) {
        views[type].push(view);
    }

    /**
     * Returns, if exists, a view of the type <code>typeView</code>.
     *
     * @param typeView
     *            the type of view that you want.
     * @return a view of the type <code>typeView</code>. <code>null</code> if
     *         not found.
     */
    public View getRecycledView(int typeView) {
        try {
            return views[typeView].pop();
        } catch (java.util.EmptyStackException e) {
            return null;

        }
    }
}
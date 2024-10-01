/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.relationship;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

public class AutocompleteRelatedPersonTextView extends AppCompatAutoCompleteTextView {
    Context context;
    public AutocompleteRelatedPersonTextView(Context context) {
        super(context);
        this.context = context;
    }

    public AutocompleteRelatedPersonTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void showDropDown() {
        //This function overrides the height of the autocomplete so as to leave a space of 80px at the bottom of the screen
        // whenever the keyboard is not displayed
        Rect displayFrame = new Rect();
        getWindowVisibleDisplayFrame(displayFrame);

        int[] locationOnScreen = new int[2];
        getLocationOnScreen(locationOnScreen);

        int bottom = locationOnScreen[1] + getHeight();
        int availableHeightBelow = displayFrame.bottom - bottom;
        Resources r = getResources();
        int bottomHeight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, r.getDisplayMetrics()));
        int downHeight = availableHeightBelow - bottomHeight;
        int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        //The calculated drop down height will be less than a third of the screen if the keyboard is up
        if(downHeight >= screenHeight/3) {
            setDropDownHeight(downHeight);
        }
        super.showDropDown();
    }
}

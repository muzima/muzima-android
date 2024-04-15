/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.appcompat.app.AlertDialog;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
import com.muzima.listners.IDialogListener;
import com.muzima.model.FormItem;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewUtil {
    public static void applyFormsListSorting(final Context context, List<FormItem> formList, boolean isNaturalSorting) {
        if (!isNaturalSorting) {
            Collections.sort(formList, new Comparator<FormItem>() {
                @Override
                public int compare(FormItem o1, FormItem o2) {
                    try {
                        Integer o1Downloaded = ((MuzimaApplication) context.getApplicationContext()).getFormController().isFormDownloaded(o1.getForm()) ? 1 : 0;
                        Integer o2Downloaded = ((MuzimaApplication) context.getApplicationContext()).getFormController().isFormDownloaded(o2.getForm()) ? 1 : 0;
                        return o2Downloaded.compareTo(o1Downloaded);
                    } catch (FormController.FormFetchException ex) {
                        ex.printStackTrace();
                        return 1;
                    }
                }
            });
        } else {
            Collections.sort(formList, new Comparator<FormItem>() {
                @Override
                public int compare(FormItem o1, FormItem o2) {
                    return o1.getForm().getName().compareTo(o2.getForm().getName());
                }
            });
        }
    }

    public static void expand(View view) {
        Animation animation = expandAction(view);
        view.startAnimation(animation);
    }

    private static Animation expandAction(final View view) {

        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int actualheight = view.getMeasuredHeight();

        view.getLayoutParams().height = 0;
        view.setVisibility(View.VISIBLE);

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.getLayoutParams().height = interpolatedTime == 1 ? ViewGroup.LayoutParams.WRAP_CONTENT : (int) (actualheight * interpolatedTime);
                view.requestLayout();
            }
        };

        animation.setDuration((long) (actualheight / view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);

        return animation;


    }

    public static void collapse(final View view) {

        final int actualHeight = view.getMeasuredHeight();

        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (interpolatedTime == 1) {
                    view.setVisibility(View.GONE);
                } else {
                    view.getLayoutParams().height = actualHeight - (int) (actualHeight * interpolatedTime);
                    view.requestLayout();

                }
            }
        };

        animation.setDuration((long) (actualHeight/ view.getContext().getResources().getDisplayMetrics().density));
        view.startAnimation(animation);
    }

    public static AlertDialog displayAlertDialog(final Context mContext, final String alertMessage, IDialogListener listener) {
        return genericDisplayAlertDialog(mContext, alertMessage, listener);
    }

    public static AlertDialog displayAlertDialog(final Context mContext, final String alertMessage) {
        return genericDisplayAlertDialog(mContext, alertMessage, null);
    }
    /**
     * Common AppCompat Alert Dialog to be used in the Application everywhere
     *
     * @param mContext, Context of where to display
     */
    private static AlertDialog genericDisplayAlertDialog(final Context mContext, final String alertMessage, IDialogListener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                .setMessage(alertMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (listener != null) listener.doOnConfirmed();
                        dialog.dismiss();
                    }

                });

        return builder.create();
    }
}

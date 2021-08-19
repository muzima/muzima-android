package com.muzima.utils;

import android.content.Context;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.controller.FormController;
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
}

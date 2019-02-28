/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.progressdialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MuzimaProgressDialogTest {

    private ProgressDialog progressDialog;
    private MuzimaProgressDialog dialog;

    @Before
    public void setUp() {
        progressDialog = mock(ProgressDialog.class);
        dialog = new MuzimaProgressDialog(progressDialog);
        Context context = mock(Context.class);
        Resources resources = mock(Resources.class);
        when(progressDialog.getContext()).thenReturn(context);
        when(context.getResources()).thenReturn(resources);
        when(resources.getString(anyInt())).thenReturn("This might take a while");
    }

    @Test
    public void shouldShowProgressDialogWithGivenText() {
        dialog.show("title");

        Mockito.verify(progressDialog).setCancelable(false);
        Mockito.verify(progressDialog).setTitle("title");
        Mockito.verify(progressDialog).setMessage("This might take a while");
        Mockito.verify(progressDialog).show();
    }

    @Test
    public void shouldDismissADialogOnlyWhenVisible() {
        when(progressDialog.isShowing()).thenReturn(true);
        dialog.dismiss();

        Mockito.verify(progressDialog).dismiss();
    }

    @Test
    public void shouldNotCallDismissIfProgressBarISNotVisible() {
        when(progressDialog.isShowing()).thenReturn(false);

        dialog.dismiss();
        Mockito.verify(progressDialog, Mockito.never()).dismiss();
    }
}
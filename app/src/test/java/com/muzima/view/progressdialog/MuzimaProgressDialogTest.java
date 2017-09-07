/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.progressdialog;

import android.app.ProgressDialog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MuzimaProgressDialogTest {

    private ProgressDialog progressDialog;
    private MuzimaProgressDialog dialog;

    @Before
    public void setUp() throws Exception {
        progressDialog = Mockito.mock(ProgressDialog.class);
        dialog = new MuzimaProgressDialog(progressDialog);
    }

    @Test
    public void shouldShowProgressDialogWithGivenText() throws Exception {
        dialog.show("title");

        Mockito.verify(progressDialog).setCancelable(false);
        Mockito.verify(progressDialog).setTitle("title");
        Mockito.verify(progressDialog).setMessage("This might take a while");
        Mockito.verify(progressDialog).show();
    }

    @Test
    public void shouldDismissADialogOnlyWhenVisible() throws Exception {
        Mockito.when(progressDialog.isShowing()).thenReturn(true);
        dialog.dismiss();

        Mockito.verify(progressDialog).dismiss();
    }

    @Test
    public void shouldNotCallDismissIfProgressBarISNotVisible() throws Exception {
        Mockito.when(progressDialog.isShowing()).thenReturn(false);

        dialog.dismiss();
        Mockito.verify(progressDialog, Mockito.never()).dismiss();
    }
}
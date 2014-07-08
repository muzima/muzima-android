package com.muzima.view.forms;

import android.app.ProgressDialog;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MuzimaProgressDialogTest {

    private ProgressDialog progressDialog;
    private MuzimaProgressDialog dialog;

    @Before
    public void setUp() throws Exception {
        progressDialog = mock(ProgressDialog.class);
        dialog = new MuzimaProgressDialog(progressDialog);
    }

    @Test
    public void shouldShowProgressDialogWithGivenText() throws Exception {
        dialog.show("title");

        verify(progressDialog).setCancelable(false);
        verify(progressDialog).setTitle("title");
        verify(progressDialog).setMessage("This might take a while");
        verify(progressDialog).show();
    }

    @Test
    public void shouldDismissADialogOnlyWhenVisible() throws Exception {
        when(progressDialog.isShowing()).thenReturn(true);
        dialog.dismiss();

        verify(progressDialog).dismiss();
    }

    @Test
    public void shouldNotCallDismissIfProgressBarISNotVisible() throws Exception {
        when(progressDialog.isShowing()).thenReturn(false);

        dialog.dismiss();
        verify(progressDialog,never()).dismiss();
    }
}

package com.muzima.view.forms;

import android.app.ProgressDialog;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

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
        verify(progressDialog).setMessage("Please wait");
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

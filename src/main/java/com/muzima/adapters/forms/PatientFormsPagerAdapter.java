package com.muzima.adapters.forms;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.view.forms.CompleteFormsListFragment;
import com.muzima.view.forms.DownloadedFormsListFragment;
import com.muzima.view.forms.FormsListFragment;
import com.muzima.view.forms.IncompleteFormsListFragment;
import com.muzima.view.forms.NewFormsListFragment;
import com.muzima.view.forms.SyncedFormsListFragment;

public class PatientFormsPagerAdapter extends MuzimaPagerAdapter{
    private static final int TAB_INCOMPLETE = 0;
    private static final int TAB_RECOMMENDED = 1;
    private static final int TAB_COMPLETE = 2;
    private static final int TAB_SYNCED = 3;

    public PatientFormsPagerAdapter(Context context, FragmentManager fm) {
        super(context, fm);
    }

    @Override
    protected void initPagerViews(Context context) {
        pagers = new PagerView[4];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        IncompleteFormsListFragment incompleteFormsListFragment = IncompleteFormsListFragment.newInstance(formController);
        CompleteFormsListFragment recommendedFormsListFragment = CompleteFormsListFragment.newInstance(formController);
        CompleteFormsListFragment completeFormsListFragment = CompleteFormsListFragment.newInstance(formController);
        SyncedFormsListFragment syncedFormsListFragment = SyncedFormsListFragment.newInstance(formController);

        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", incompleteFormsListFragment);
        pagers[TAB_RECOMMENDED] = new PagerView("Recommended", recommendedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView("Complete", completeFormsListFragment);
        pagers[TAB_SYNCED] = new PagerView("Synced", syncedFormsListFragment);
    }
}

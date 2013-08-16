package com.muzima.adapters.forms;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

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

public class FormsPagerAdapter extends MuzimaPagerAdapter implements DownloadListener<Integer[]>, TagsListAdapter.TagsChangedListener {
    private static final int TAB_All = 0;
    private static final int TAB_DOWNLOADED = 1;
    private static final int TAB_COMPLETE = 2;
    private static final int TAB_INCOMPLETE = 3;
    private static final int TAB_SYNCED = 4;

    private PagerView[] pagers;

    public FormsPagerAdapter(Context context, FragmentManager fm) {
        super(context,fm);
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        pagers[TAB_All].fragment.formDownloadComplete(result);
    }

    @Override
    public void onTagsChanged() {
        ((FormsListFragment)pagers[TAB_All].fragment).tagsChanged();
    }

    @Override
    protected void initPagerViews(Context context) {
        pagers = new PagerView[5];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        NewFormsListFragment newFormsListFragment = NewFormsListFragment.newInstance(formController);
        DownloadedFormsListFragment downloadedFormsListFragment = DownloadedFormsListFragment.newInstance(formController);
        CompleteFormsListFragment completeFormsListFragment = CompleteFormsListFragment.newInstance(formController);
        IncompleteFormsListFragment incompleteFormsListFragment = IncompleteFormsListFragment.newInstance(formController);
        SyncedFormsListFragment syncedFormsListFragment = SyncedFormsListFragment.newInstance(formController);

        newFormsListFragment.setTemplateDownloadCompleteListener(downloadedFormsListFragment);

        pagers[TAB_All] = new PagerView("All", newFormsListFragment);
        pagers[TAB_DOWNLOADED] = new PagerView("Downloaded", downloadedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView("Complete", completeFormsListFragment);
        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", incompleteFormsListFragment);
        pagers[TAB_SYNCED] = new PagerView("Synced", syncedFormsListFragment);
    }
}

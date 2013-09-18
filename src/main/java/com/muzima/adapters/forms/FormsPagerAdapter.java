package com.muzima.adapters.forms;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.view.forms.*;

public class FormsPagerAdapter extends MuzimaPagerAdapter implements TagsListAdapter.TagsChangedListener {
    public static final int TAB_All = 0;
    public static final int TAB_DOWNLOADED = 1;
    public static final int TAB_INCOMPLETE = 2;
    public static final int TAB_COMPLETE = 3;

    public FormsPagerAdapter(Context context, FragmentManager fm) {
        super(context, fm);
    }

    public void onDownloadStart(){
        pagers[TAB_All].fragment.synchronizationStarted();
    }

    public void onDownloadFinish(){
        pagers[TAB_All].fragment.synchronizationComplete();
    }

    @Override
    public void onTagsChanged() {
        ((FormsListFragment)pagers[TAB_All].fragment).tagsChanged();
    }

    @Override
    public void initPagerViews() {
        pagers = new PagerView[4];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        AllAvailableFormsListFragment allAvailableFormsListFragment = AllAvailableFormsListFragment.newInstance(formController);
        DownloadedFormsListFragment downloadedFormsListFragment = DownloadedFormsListFragment.newInstance(formController);
        CompleteFormsListFragment completeFormsListFragment = CompleteFormsListFragment.newInstance(formController);
        IncompleteFormsListFragment incompleteFormsListFragment = IncompleteFormsListFragment.newInstance(formController);

        allAvailableFormsListFragment.setTemplateDownloadCompleteListener(downloadedFormsListFragment);

        pagers[TAB_All] = new PagerView("All", allAvailableFormsListFragment);
        pagers[TAB_DOWNLOADED] = new PagerView("Downloaded", downloadedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView("Complete", completeFormsListFragment);
        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", incompleteFormsListFragment);
    }
}

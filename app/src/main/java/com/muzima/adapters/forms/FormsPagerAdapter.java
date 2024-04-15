/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import androidx.fragment.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.LanguageUtil;
import com.muzima.view.forms.CompleteFormsListFragment;
import com.muzima.view.forms.IncompleteFormsListFragment;

/**
 * Responsible to display all the form fragments under form section.
 */
public class FormsPagerAdapter extends MuzimaPagerAdapter implements TagsListAdapter.TagsChangedListener {
    public static final int TAB_INCOMPLETE = 0;
    public static final int TAB_COMPLETE = 1;

    public FormsPagerAdapter(Context context, FragmentManager fm) {
        super(context, fm);
    }

    public void onFormMetadataDownloadStart(){
    }

    public void onFormMetadataDownloadFinish(){
    }

    public void onFormTemplateDownloadFinish() {
    }

    public void onFormUploadFinish() {
        ((CompleteFormsListFragment)pagers[TAB_COMPLETE].fragment).onFormUploadFinish();
    }

    @Override
    public void onTagsChanged() {
    }

    @Override
    public void initPagerViews() {
        pagers = new PagerView[2];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();
        ObservationController observationController = ((MuzimaApplication) context.getApplicationContext()).getObservationController();
        CompleteFormsListFragment completeFormsListFragment = CompleteFormsListFragment.newInstance(formController, observationController);
        IncompleteFormsListFragment incompleteFormsListFragment = IncompleteFormsListFragment.newInstance(formController, observationController);

        LanguageUtil languageUtil = new LanguageUtil();
        Context localizedContext = languageUtil.getLocalizedContext(context);
        pagers[TAB_COMPLETE] = new PagerView(localizedContext.getResources().getString(R.string.title_form_data_complete), completeFormsListFragment);
        pagers[TAB_INCOMPLETE] = new PagerView(localizedContext.getResources().getString(R.string.title_form_data_incomplete), incompleteFormsListFragment);
    }

    public void endActionMode() {
    }

}

package com.muzima.adapters.forms;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.FormController;
import com.muzima.view.forms.CompleteFormsListFragment;
import com.muzima.view.forms.IncompleteFormsListFragment;
import com.muzima.view.forms.RecommendedFormsListFragment;

public class PatientFormsPagerAdapter extends MuzimaPagerAdapter{
    private static final int TAB_INCOMPLETE = 0;
    private static final int TAB_RECOMMENDED = 1;
    private static final int TAB_COMPLETE = 2;
    private final String patientId;

    public PatientFormsPagerAdapter(Context context, FragmentManager fm, String patientId) {
        super(context, fm);
        this.patientId = patientId;
    }

    @Override
    protected void initPagerViews(Context context) {
        pagers = new PagerView[3];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        IncompleteFormsListFragment incompleteFormsListFragment = IncompleteFormsListFragment.newInstance(formController);
        RecommendedFormsListFragment recommendedFormsListFragment = RecommendedFormsListFragment.newInstance(formController, patientId);
        CompleteFormsListFragment completeFormsListFragment = CompleteFormsListFragment.newInstance(formController);

        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", incompleteFormsListFragment);
        pagers[TAB_RECOMMENDED] = new PagerView("Recommended", recommendedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView("Complete", completeFormsListFragment);
    }
}

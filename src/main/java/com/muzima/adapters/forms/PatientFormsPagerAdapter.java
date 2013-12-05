/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.muzima.adapters.forms;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.view.forms.CompletePatientsFormsListFragment;
import com.muzima.view.forms.IncompletePatientsFormsListFragment;
import com.muzima.view.forms.RecommendedFormsListFragment;

/**
 * Responsible to hold all the form pages that are part of a specific patient.
 */
public class PatientFormsPagerAdapter extends MuzimaPagerAdapter {
    private static final int TAB_RECOMMENDED = 0;
    private static final int TAB_INCOMPLETE = 1;
    private static final int TAB_COMPLETE = 2;
    private final Patient patient;

    public PatientFormsPagerAdapter(Context context, FragmentManager fm, Patient patient) {
        super(context, fm);
        this.patient = patient;
    }

    @Override
    public void initPagerViews() {
        pagers = new PagerView[3];
        FormController formController = ((MuzimaApplication) context.getApplicationContext()).getFormController();

        IncompletePatientsFormsListFragment incompleteFormsListFragment = IncompletePatientsFormsListFragment.newInstance(formController, patient);
        RecommendedFormsListFragment recommendedFormsListFragment = RecommendedFormsListFragment.newInstance(formController, patient);
        CompletePatientsFormsListFragment completeFormsListFragment = CompletePatientsFormsListFragment.newInstance(formController, patient);

        pagers[TAB_INCOMPLETE] = new PagerView("Incomplete", incompleteFormsListFragment);
        pagers[TAB_RECOMMENDED] = new PagerView("Recommended", recommendedFormsListFragment);
        pagers[TAB_COMPLETE] = new PagerView("Complete", completeFormsListFragment);
    }
}

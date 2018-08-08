/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;


import android.widget.ListView;

import com.muzima.testSupport.CustomTestRunner;
import com.muzima.view.forms.FormsActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(CustomTestRunner.class)
@Config(manifest = Config.NONE)
public class FormsActivityTest {
    private FormsActivity activity;
    private ListView formsListView;

    @Before
    public void setUp() {
//        activity = new FormsActivity();
//        activity.onCreate(null);
//        formsListView = (ListView) activity.findViewById(R.id.forms_list);
    }

    @Test
    public void onCreate_listviewShouldHaveAnAdapter() {
//        assertNotNull(formsListView.getAdapter());
    }
}

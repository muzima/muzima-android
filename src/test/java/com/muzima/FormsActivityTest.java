package com.muzima;


import android.widget.ListView;

import com.muzima.testSupport.CustomTestRunner;
import com.muzima.view.FormsActivity;

import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

@RunWith(CustomTestRunner.class)
public class FormsActivityTest {
    private FormsActivity activity;
    private ListView formsListView;

    @Before
    public void setUp() throws Exception {
        activity = new FormsActivity();
        activity.onCreate(null);
        formsListView = (ListView) activity.findViewById(R.id.forms_list);
    }

//    @Test
//    public void onCreate_listviewShouldHaveAnAdapter() throws Exception {
//        assertNotNull(formsListView.getAdapter());
//    }
}

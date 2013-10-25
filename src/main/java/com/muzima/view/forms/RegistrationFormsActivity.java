package com.muzima.view.forms;

import android.os.Bundle;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockActivity;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.RegistrationFormsAdapter;

public class RegistrationFormsActivity extends SherlockActivity {
    private ListView list;
    private RegistrationFormsAdapter registrationFormsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_list);
        list = (ListView) findViewById(R.id.list);
        MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        registrationFormsAdapter = new RegistrationFormsAdapter(applicationContext, R.layout.item_forms_list, applicationContext.getFormController());
        list.setAdapter(registrationFormsAdapter);
        registrationFormsAdapter.reloadData();
    }
}

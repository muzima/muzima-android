package com.muzima.view.forms;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.RegistrationFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.model.AvailableForm;

public class RegistrationFormsActivity extends SherlockActivity {
    private ListView list;
    private RegistrationFormsAdapter registrationFormsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_form_list);
        MuzimaApplication applicationContext = (MuzimaApplication) getApplicationContext();
        registrationFormsAdapter = new RegistrationFormsAdapter(applicationContext, R.layout.item_forms_list, applicationContext.getFormController());
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AvailableForm form = registrationFormsAdapter.getItem(position);
                startActivity(new FormViewIntent(RegistrationFormsActivity.this, form, new Patient()));
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        list.setAdapter(registrationFormsAdapter);
        registrationFormsAdapter.reloadData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

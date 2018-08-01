/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaGeneratedReport;
import com.muzima.api.model.Patient;
import com.muzima.controller.MuzimaGeneratedReportController;
import com.muzima.view.BaseActivity;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.List;

public class PatientReportListViewActivity extends BaseActivity {
    
    private static final String TAG = "PatientReportListViewActivity";
    
    String[] reportNames = new String[] { "Patient Report 1", "Patient Report 2", "Patient Report 3" };
    
    ListView listView;
    
    private Patient patient;
    
    private WebView myWebView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_test_button);
        
       /* listView = (ListView) findViewById(R.id.list);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, reportNames);
        listView.setAdapter(adapter);*/
        
        Intent intent = getIntent();
        patient = (Patient) intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
        
    
       /* listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                
                // ListView Clicked item value
                String itemValue = (String) listView.getItemAtPosition(position);
                
                // Show Alert 
                Toast.makeText(getApplicationContext(), "Position :" + position + "  ListItem : " + itemValue,
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), PatientReportWebActivity.class);
                
                intent.putExtra("url", "http://www.cricinfo.com");
                
                startActivity(intent);
                
            }
        });*/
        Toast.makeText(getApplicationContext(), "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee ", Toast.LENGTH_LONG).show();
    
    
    }
    
    public void downloadReport(View v){
        Toast.makeText(getApplicationContext(), "11111111111111111111", Toast.LENGTH_LONG).show();
    
       
        MuzimaGeneratedReportController muzimaGeneratedReportController = ((MuzimaApplication) getApplicationContext()).getMuzimaGeneratedReportController();
    
        List<MuzimaGeneratedReport> muzimaGeneratedReports = null;
        try {
            muzimaGeneratedReports = muzimaGeneratedReportController.getAllMuzimaGeneratedReportsByPatientUuid(patient.getUuid());
        }
        catch (MuzimaGeneratedReportController.MuzimaGeneratedReportException e) {
            e.printStackTrace();
        }
        if(muzimaGeneratedReports.size()==0) {
            try {
                muzimaGeneratedReports = muzimaGeneratedReportController.downloadLastPriorityMuzimaGeneratedReportByPatientUuid(patient.getUuid());
                muzimaGeneratedReportController.saveAllMuzimaGeneratedReports(muzimaGeneratedReports);
            }
            catch (MuzimaGeneratedReportController.MuzimaGeneratedReportSaveException e) {
                e.printStackTrace();
            }
            catch (MuzimaGeneratedReportController.MuzimaGeneratedReportDownloadException e) {
                e.printStackTrace();
            }
        }
        Toast.makeText(getApplicationContext(), "Size is : "+muzimaGeneratedReports.size(), Toast.LENGTH_LONG).show();
    
    
        Intent intent = new Intent(getApplicationContext(), PatientReportWebActivity.class);
    
        intent.putExtra("url", "http://www.cricinfo.com");
        
        Toast.makeText(getApplicationContext(), "77777777777777777", Toast.LENGTH_LONG).show();
        startActivity(intent);
    }
}

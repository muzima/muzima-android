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
import com.muzima.R;
import com.muzima.api.model.Patient;

public class PatientReportListViewActivity extends Activity {
    
    private static final String TAG = "PatientReportListViewActivity";
    
    String[] values = new String[] { "Android List View", "Adapter implementation", "Simple List View In Android",
            "Create List View Android", "Android Example", "List View Source Code", "List View Array Adapter",
            "Android Example List View" };
    
    ListView listView;
    
    private Patient patient;
    
    private WebView myWebView;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        
        listView = (ListView) findViewById(R.id.list);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, values);
        listView.setAdapter(adapter);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                
                // ListView Clicked item index
                int itemPosition = position;
                
                // ListView Clicked item value
                String itemValue = (String) listView.getItemAtPosition(position);
                
                // Show Alert 
                Toast.makeText(getApplicationContext(), "Position :" + itemPosition + "  ListItem : " + itemValue,
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), PatientReportWebActivity.class);
                
                intent.putExtra("url", "http://www.cricinfo.com");
                
                startActivity(intent);
                
            }
        });
    }
}

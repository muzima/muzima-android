package com.muzima.view.provider;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import com.actionbarsherlock.view.Menu;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.view.concept.ConceptListActivity;
import com.muzima.view.forms.MuzimaProgressDialog;
import com.muzima.view.preferences.ProviderPreferenceActivity;


public class ProviderListActivity extends ProviderPreferenceActivity{
    private static final String TAG = "ProviderListActivity";
    private MuzimaProgressDialog muzimaProgressDialog;
    protected Credentials credentials;
    private boolean isProcessDialogOn = false;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null ;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentials = new Credentials(this);

        Button nextButton = (Button) findViewById(R.id.next);
        muzimaProgressDialog = new MuzimaProgressDialog(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToNextActivity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isProcessDialogOn){
            turnOnProgressDialog("Downloading Provider(s)...");
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_provider_list;
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), ConceptListActivity.class);
        startActivity(intent);
        finish();
    }

    private void turnOnProgressDialog(String message){
        muzimaProgressDialog.show(message);
        isProcessDialogOn = true;
    }
}

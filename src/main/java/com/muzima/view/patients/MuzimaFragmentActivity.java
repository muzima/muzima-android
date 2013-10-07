package com.muzima.view.patients;

import com.actionbarsherlock.view.MenuItem;
import com.muzima.view.BroadcastListenerActivity;

//TODO pull the onOptionsItemClick up here
public class MuzimaFragmentActivity extends BroadcastListenerActivity {
    public MuzimaFragmentActivity() {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }
}

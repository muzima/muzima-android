package com.muzima.view.patients;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;

//TODO pull the onOptionsItemClick up here
public class MuzimaFragmentActivity extends SherlockFragmentActivity {
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

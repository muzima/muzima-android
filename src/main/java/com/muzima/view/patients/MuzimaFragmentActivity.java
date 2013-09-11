package com.muzima.view.patients;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;

public class MuzimaFragmentActivity extends SherlockFragmentActivity {
    public MuzimaFragmentActivity() {
        overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
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

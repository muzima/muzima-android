package com.muzima.view.patients;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.muzima.R;

public class MuzimaFragmentActivity extends SherlockFragmentActivity {
    public MuzimaFragmentActivity() {
        overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
    }
}

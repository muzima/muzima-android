package com.muzima.messaging;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.muzima.messaging.fragments.PlayServicesProblemFragment;

public class PlayServicesProblemActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        PlayServicesProblemFragment fragment = new PlayServicesProblemFragment();
        fragment.show(getSupportFragmentManager(), "dialog");
    }
}

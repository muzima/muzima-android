package com.muzima.messaging;

import android.content.Intent;
import android.os.Bundle;

import com.muzima.R;
import com.muzima.messaging.fragments.ContactSelectionListFragment;

import java.util.ArrayList;
import java.util.List;

public class PushContactSelectionActivity extends ContactSelectionActivity {

    @SuppressWarnings("unused")
    private final static String TAG = PushContactSelectionActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
        getIntent().putExtra(ContactSelectionListFragment.MULTI_SELECT, true);
        super.onCreate(icicle, ready);

        getToolbar().setNavigationIcon(R.drawable.ic_check_white_24dp);
        getToolbar().setNavigationOnClickListener(v -> {
            Intent resultIntent = getIntent();
            List<String> selectedContacts = contactsFragment.getSelectedContacts();

            if (selectedContacts != null) {
                resultIntent.putStringArrayListExtra("contacts", new ArrayList<>(selectedContacts));
            }

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
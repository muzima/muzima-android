package com.muzima.messaging;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

import com.muzima.R;
import com.muzima.messaging.contacts.ContactsCursorLoader.DisplayMode;
import com.muzima.messaging.customcomponents.ContactFilterToolbar;
import com.muzima.messaging.fragments.ContactSelectionListFragment;
import com.muzima.messaging.utils.DirectoryHelper;
import com.muzima.utils.ViewUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class ContactSelectionActivity extends PassphraseRequiredActionBarActivity
        implements SwipeRefreshLayout.OnRefreshListener,
        ContactSelectionListFragment.OnContactSelectedListener {
    private static final String TAG = ContactSelectionActivity.class.getSimpleName();

    protected ContactSelectionListFragment contactsFragment;

    private ContactFilterToolbar toolbar;

    @Override
    protected void onPreCreate() {

    }

    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
        if (!getIntent().hasExtra(ContactSelectionListFragment.DISPLAY_MODE)) {
            int displayMode = TextSecurePreferences.isSmsEnabled(this) ? DisplayMode.FLAG_ALL
                    : DisplayMode.FLAG_PUSH | DisplayMode.FLAG_GROUPS;
            getIntent().putExtra(ContactSelectionListFragment.DISPLAY_MODE, displayMode);
        }

        setContentView(R.layout.contact_selection_activity);

        initializeToolbar();
        initializeResources();
        initializeSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    protected ContactFilterToolbar getToolbar() {
        return toolbar;
    }

    private void initializeToolbar() {
        this.toolbar = ViewUtil.findById(this, R.id.toolbar);
        setSupportActionBar(toolbar);

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setIcon(null);
        getSupportActionBar().setLogo(null);
    }

    private void initializeResources() {
        contactsFragment = (ContactSelectionListFragment) getSupportFragmentManager().findFragmentById(R.id.contact_selection_list_fragment);
        contactsFragment.setOnContactSelectedListener(this);
        contactsFragment.setOnRefreshListener(this);
    }

    private void initializeSearch() {
        toolbar.setOnFilterChangedListener(filter -> contactsFragment.setQueryFilter(filter));
    }

    @Override
    public void onRefresh() {
        new RefreshDirectoryTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getApplicationContext());
    }

    @Override
    public void onContactSelected(String number) {
    }

    @Override
    public void onContactDeselected(String number) {
    }

    private static class RefreshDirectoryTask extends AsyncTask<Context, Void, Void> {

        private final WeakReference<ContactSelectionActivity> activity;

        private RefreshDirectoryTask(ContactSelectionActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        protected Void doInBackground(Context... params) {

            try {
                DirectoryHelper.refreshDirectory(params[0], true);
            } catch (IOException e) {
                Log.w(TAG, e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            ContactSelectionActivity activity = this.activity.get();

            if (activity != null && !activity.isFinishing()) {
                activity.toolbar.clear();
                activity.contactsFragment.resetQueryFilter();
            }
        }
    }
}

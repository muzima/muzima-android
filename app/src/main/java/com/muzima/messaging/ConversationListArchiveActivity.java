package com.muzima.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.muzima.R;
import com.muzima.model.SignalRecipient;

public class ConversationListArchiveActivity extends PassphraseRequiredActionBarActivity
        implements ConversationListFragment.ConversationSelectedListener
{
    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.hint_archived_conversations);

        Bundle bundle = new Bundle();
        bundle.putBoolean(ConversationListFragment.ARCHIVE, true);

        initFragment(android.R.id.content, new ConversationListFragment(), getResources().getConfiguration().locale, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.home: super.onBackPressed(); return true;
        }

        return false;
    }

    @Override
    public void onCreateConversation(long threadId, SignalRecipient recipient, int distributionType, long lastSeenTime) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
        intent.putExtra(ConversationActivity.IS_ARCHIVED_EXTRA, true);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, distributionType);
        intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, lastSeenTime);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
    }

    @Override
    public void onSwitchToArchive() {
        throw new AssertionError();
    }

}

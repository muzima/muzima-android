package com.muzima.messaging;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.ThreadDatabase;
import com.muzima.model.SignalRecipient;

public class NewConversationActivity extends ContactSelectionActivity {

    @SuppressWarnings("unused")
    private static final String TAG = NewConversationActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle bundle, boolean ready) {
        super.onCreate(bundle, ready);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onContactSelected(String number) {
        SignalRecipient recipient = SignalRecipient.from(this, SignalAddress.fromExternal(this, number), true);

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.TEXT_EXTRA, getIntent().getStringExtra(ConversationActivity.TEXT_EXTRA));
        intent.setDataAndType(getIntent().getData(), getIntent().getType());

        long existingThread = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipient);

        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:   super.onBackPressed(); return true;
            case R.id.menu_refresh:   handleManualRefresh(); return true;
            case R.id.menu_new_group: handleCreateGroup();   return true;
            case R.id.menu_invite:    handleInvite();        return true;
        }

        return false;
    }

    private void handleManualRefresh() {
        contactsFragment.setRefreshing(true);
        onRefresh();
    }

    private void handleCreateGroup() {
        startActivity(new Intent(this, GroupCreateActivity.class));
    }

    private void handleInvite() {
        startActivity(new Intent(this, InviteActivity.class));
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.new_conversation_activity, menu);
        super.onPrepareOptionsMenu(menu);
        return true;
    }
}
